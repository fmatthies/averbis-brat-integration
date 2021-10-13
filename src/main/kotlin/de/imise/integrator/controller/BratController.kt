package de.imise.integrator.controller

import com.beust.klaxon.JsonObject
import com.beust.klaxon.json
import de.imise.integrator.controller.BratController.Companion.AVERBIS_HEALTH_PRE
import de.imise.integrator.controller.BratController.Companion.DOCUMENT_TEXT_TYPE
import de.imise.integrator.extensions.DateFunctionality
import de.imise.integrator.extensions.ResponseType
import de.imise.integrator.extensions.ResponseTypeEntry
import de.imise.integrator.extensions.padAround
import javafx.collections.ObservableList
import tornadofx.*
import java.nio.charset.Charset


data class BratAnnotation(val id: Int, val bratType: String, override val type: String,
                          val begin: Int, val end: Int, override val text: String) : ResponseTypeEntry

class BratResponse(annFile: InMemoryFile?, jsonFile: InMemoryFile?): ResponseType {
    val averbisData = jsonFile?.let { AverbisResponse(it.baseName, "in-memory")
        .apply { this.readJson(jsonFile.content.toString(Charset.defaultCharset())) } }
    var textboundData = mutableMapOf<Int, BratAnnotation>()
    override val basename: String = annFile?.baseName ?: "none"
    override val additionalColumn = "in-memory"
    override val items: ObservableList<ResponseTypeEntry>  //ToDo: I want all? (even json entries)
        get() = textboundData
            .values
            .toList()
            .asObservable()

    init {
        if (annFile != null) {
            readBrat(annFile.content)
        }
    }

    fun readBrat(content: ByteArray) {
        content.toString(Charset.defaultCharset()).splitToSequence("\n").forEach {
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
            val (annotationType, begin, end) = type_offset.split(" ")
            BratAnnotation(
                id.substring(1).toInt(),
                id[0].toString(),
                annotationType,
                begin.toInt(),
                end.toInt(),
                text.substringBefore("\n")
            ).also { textboundData[it.id] = it }
        }
    }

    private fun crossOutModifyAnnotations(
        sb: StringBuilder,
        data: BratAnnotation,
        crossOut: List<String>,
        modify: List<String>
    ): BratAnnotation {
        val newText = if (crossOut.any { it == "$AVERBIS_HEALTH_PRE${data.type}" }) {
            "X".repeat(data.text.length)
        } else if (modify.contains("$AVERBIS_HEALTH_PRE${data.type}")) {
            if (data.type.lowercase() == "date") {  //ToDo: only for date right now and hard-coded
                val newDate = DateFunctionality(data.text).getDate()
                newDate.padAround(data.text.length, ' ') //ToDo: what if newDate.length is bigger than text.length? Is this even possible?
            } else { "<${".".repeat(data.text.length)}>" }
        } else { data.text }
        sb.setRange(data.begin, data.end, newText)
        return data.copy(text = newText)
    }

    fun mergeAverbisBrat(crossOut: List<String>, modify: List<String>, removeCrossedOut: Boolean): JsonObject {
        val idSetBrat = textboundData.keys
        val idSetAverbis = averbisData!!.getData().keys
        val mergedData = mutableListOf<JsonObject>()
        fun MutableList<JsonObject>.addJson(sourceData: BratAnnotation, id: Int? = null) {
            this.add(
                json { obj(
                    "begin" to sourceData.begin,
                    "end" to sourceData.end,
                    "type" to "$AVERBIS_HEALTH_PRE${sourceData.type}",
                    "coveredText" to sourceData.text,
                    "id" to (id ?: sourceData.id),
                ) }
            )
        }
        fun Iterable<Int>.addById(sb: StringBuilder) {
            this.forEach {
                textboundData[it]?.let { data ->
                    val newData = crossOutModifyAnnotations(sb, data, crossOut, modify)
                    if (removeCrossedOut && crossOut.contains("$AVERBIS_HEALTH_PRE${data.type}")) return@forEach
                    mergedData.addJson(newData)
                }
            }
        }
        val doc = StringBuilder(averbisData.documentText).also { sb ->
            // Everything that's in the brat annotation file and in the Averbis json
            idSetBrat.intersect(idSetAverbis).addById(sb)
            // Everything that's only in the brat annotation file
            idSetBrat.subtract(idSetAverbis).addById(sb)
        }.toString()

        mergedData.add(json { obj(
            "begin" to 0,
            "end" to doc.length,
            "type" to DOCUMENT_TEXT_TYPE,
            "coveredText" to doc,
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