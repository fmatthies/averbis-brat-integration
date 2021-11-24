package de.imise.integrator.controller

import com.beust.klaxon.JsonObject
import com.beust.klaxon.json
import de.imise.integrator.controller.BratController.Companion.AVERBIS_HEALTH_PRE
import de.imise.integrator.controller.BratController.Companion.DOCUMENT_TEXT_TYPE
import de.imise.integrator.extensions.*
import javafx.collections.ObservableList
import tornadofx.*


data class BratAnnotation(val id: Int, val bratType: String, override val type: String,
                          val offsets: List<Pair<Int, Int>>, override val text: String) : ResponseTypeEntry

class BratResponse(annFile: InMemoryFile?, jsonFile: InMemoryFile?): ResponseType {
    val averbisData = jsonFile?.let { AverbisResponse(it.baseName, "in-memory")
        .apply { this.readJson(jsonFile.content.toString(Charsets.UTF_8)) } }
    var textboundData = mutableMapOf<Int, BratAnnotation>()
    private var textData = ""
    override val basename: String = annFile?.baseName ?: "none"
    override val additionalColumn = "in-memory"
    override val items: ObservableList<ResponseTypeEntry>  //ToDo: I want all? (even json entries; maybe two different column extensions)
        get() = textboundData
            .values
            .toList()
            .asObservable()

    init {
        if (annFile != null) {
            readBrat(annFile.content)
        }
    }

    fun getTxt(anonymize: Boolean = true): String {
        return when (anonymize) {
            true -> textData
            false -> averbisData?.documentText ?: ""
        }
    }

    fun readBrat(content: ByteArray) {
        content.toString(Charsets.UTF_8).splitToSequence("\n").forEach {
            if (it.isEmpty()) return@forEach
            when (it.first().toString()) {
                "T" -> readTextbound(it)
                else -> return@forEach
            }
        }
    }

    private fun readTextbound(line: String) {
        line.split("\t").run {
            val (id, type_offset, text) = this
            val (annotationType, offsets) = if (type_offset.split(" ").size == 3) {
                type_offset.split(" ").run {
                    listOf(this.first(), listOf(Pair(this[1].toInt(), this[2].toInt())))
                }
            } else {
                val aType = type_offset.split(" ").first()
                listOf(
                    aType,
                    mutableListOf<Pair<Int, Int>>().apply {
                        type_offset.removePrefix("$aType ").split(";").forEach {
                            this.add(Pair(
                                it.split(" ").first().toInt(),
                                it.split(" ").component2().toInt()
                            ))
                        }
                    }.toList()
                )
            }
            BratAnnotation(
                id.substring(1).toInt(),
                id[0].toString(),
                annotationType as String,
                offsets as List<Pair<Int, Int>>,
                text.substringBeforeRegex("\\r?\\n|\\r")
            ).also { textboundData[it.id] = it }
        }
    }

    private fun crossOutModifyAnnotations(
        sb: StringBuilder,
        data: BratAnnotation,
        crossOut: List<String>,
        modify: List<String>
    ): BratAnnotation {  //ToDo: text with more than one offsets (i.e. text with Frag.) will always be crossed out right now
        val newText = if (crossOut.any { it == "$AVERBIS_HEALTH_PRE${data.type}" } || data.offsets.size > 1) {
            "X".repeat(data.text.length)
        } else if (modify.contains("$AVERBIS_HEALTH_PRE${data.type}")) {
            if (data.type.lowercase() == "date") {  //ToDo: only for date right now and hard-coded
                val newDate = DateFunctionality(data.text).getDate()
                newDate.padAround(data.text.length, ' ') //ToDo: what if newDate.length is bigger than text.length? Is this even possible?
            } else { "<${".".repeat(data.text.length)}>" }
        } else { data.text }
        data.offsets.forEach { (begin, end) ->
            if (data.offsets.size > 1) {
                sb.setRange(begin, end, "X".repeat(end - begin))
            } else {
                sb.setRange(begin, end, newText)
            }
        }
        return data.copy(text = newText)
    }

    fun mergeAverbisBrat(crossOut: List<String>, modify: List<String>, removeCrossedOut: Boolean): JsonObject {
        val idSetBrat = textboundData.keys
        val idSetAverbis = averbisData!!.getData().keys
        val mergedData = mutableListOf<JsonObject>()
        fun MutableList<JsonObject>.addJson(sourceData: BratAnnotation, id: Int? = null) {
            sourceData.offsets.forEach { (begin, end) ->
                this.add(
                    json {
                        obj(
                            "begin" to begin,
                            "end" to end,
                            "type" to "$AVERBIS_HEALTH_PRE${sourceData.type}",
                            "coveredText" to sourceData.text,//.substring(IntRange(begin, end)),
                            "id" to (id ?: sourceData.id),
                        )
                    }
                )
            }
        }
        fun Iterable<Int>.addById(sb: StringBuilder) {  //ToDo: I want to reserve the json structure of the original Averbis files if no changes were applied!
            this.forEach {
                textboundData[it]?.let { data ->
                    val newData = crossOutModifyAnnotations(sb, data, crossOut, modify)
                    if (removeCrossedOut && crossOut.contains("$AVERBIS_HEALTH_PRE${data.type}")) return@forEach
                    mergedData.addJson(newData)
                }
            }
        }
        textData = StringBuilder(averbisData.documentText).also { sb ->
            // Everything that's in the brat annotation file and in the Averbis json
            idSetBrat.intersect(idSetAverbis).addById(sb)
            // Everything that's only in the brat annotation file
            idSetBrat.subtract(idSetAverbis).addById(sb)
        }.toString()

        mergedData.add(json { obj(
            "begin" to 0,
            "end" to textData.length,
            "type" to DOCUMENT_TEXT_TYPE,
            "coveredText" to textData,
            "id" to averbisData.documentTextId,
            "language" to averbisData.documentLanguage,
            "version" to averbisData.documentAverbisVersion
        ) })

        return json { obj("annotationDtos" to array(mergedData.toList())) }
    }
}

class BratController : Controller() {
    companion object {
        val crossOutAnnotations = arrayOf(
            "de.averbis.types.health.Age", "de.averbis.types.health.Name", "de.averbis.types.health.Location",
            "de.averbis.types.health.Id", "de.averbis.types.health.Contact", "de.averbis.types.health.Profession",
            "de.averbis.types.health.PHIOther")
        val replaceAnnotations = arrayOf("de.averbis.types.health.Date")
        const val DOCUMENT_TEXT_TYPE: String = "de.averbis.types.health.DocumentAnnotation"
        const val AVERBIS_HEALTH_PRE: String = "de.averbis.types.health."
    }
}