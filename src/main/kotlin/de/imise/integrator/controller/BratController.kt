package de.imise.integrator.controller

import com.beust.klaxon.JsonObject
import com.beust.klaxon.json
import de.imise.integrator.controller.BratController.Companion.AVERBIS_HEALTH_PRE
import de.imise.integrator.controller.BratController.Companion.DOCUMENT_TEXT_TYPE
import de.imise.integrator.extensions.DateFunctionality
import de.imise.integrator.extensions.ResponseType
import de.imise.integrator.extensions.ResponseTypeEntry
import javafx.collections.ObservableList
import tornadofx.*
import java.io.File


data class BratAnnotation(val id: Int, val bratType: String, override val type: String,
                          val begin: Int, val end: Int, override val text: String) : ResponseTypeEntry

class BratResponse(annFile: File?, jsonFile: File?): ResponseType {
    val averbisData = jsonFile?.let { AverbisResponse(it).apply { this.readJson(jsonFile.readText()) } }
    var textboundData = mutableMapOf<Int, BratAnnotation>()
    override val basename: String = annFile?.nameWithoutExtension ?: "none"
    override val additionalColumn = annFile?.parent ?: "none"
    override val items: ObservableList<ResponseTypeEntry>  //ToDo: I want all? (even json entries)
        get() = textboundData
            .values
            .toList()
            .asObservable()

    init {
        if (annFile != null) {
            readBrat(annFile)
        }
    }

    fun readBrat(file: File) {
        file.readLines().forEach {
            if (it.isEmpty()) return@forEach
            when (it.first().toString()) {
                "T" -> readTextbound(it)
                else -> return@forEach
            }
        }
    }

    private fun readTextbound(line: String) {
        line.split("\t").run {
            BratAnnotation(this.first().substring(1).toInt(), this.first()[0].toString(),
            this.component2().split(" ").first(), this.component2().split(" ").component2().toInt(),
            this.component2().split( " ").component3().toInt(), this.last().substringBefore("\n"))
                .also { textboundData[it.id] = it }
        }
    }

    private fun crossOutModifyAnnotations(
        sb: StringBuilder,
        data: BratAnnotation,
        crossOut: List<String>,
        modify: List<String>
    ) {
        val newText = if (crossOut.any { it == "$AVERBIS_HEALTH_PRE${data.type}" }) {
            "X".repeat(data.text.length)
        } else if (modify.contains("$AVERBIS_HEALTH_PRE${data.type}")) {  //ToDo: modify data as well!
            if (data.type.lowercase() == "date") {  //ToDo: only for date right now and hard-coded
                val newDate = DateFunctionality(data.text).getDate()
                newDate.padStart(data.text.length, ' ') //ToDo: what if newDate.length is bigger than text.length? Is this even possible?
            } else { "<${".".repeat(data.text.length)}>" }
        } else { data.text }
        sb.setRange(data.begin, data.end, newText)
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
                    crossOutModifyAnnotations(sb, data, crossOut, modify)
                    if (removeCrossedOut && crossOut.contains("$AVERBIS_HEALTH_PRE${data.type}")) return@forEach
                    mergedData.addJson(data)
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