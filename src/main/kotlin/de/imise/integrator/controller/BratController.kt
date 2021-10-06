package de.imise.integrator.controller

import com.beust.klaxon.JsonObject
import com.beust.klaxon.json
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

    private fun crossOutModifyAnnotations(sb: StringBuilder, data: BratAnnotation) {
        if (crossOutAnnotations.any { it == "$AVERBIS_HEALTH_PRE${data.type}" }) {
            sb.setRange(data.begin, data.end, "X".repeat(data.text.length))
        }
    }

    // ToDo: right now, the single elements (even if they should be crossed out) are returned as json; maybe remove
    //  those as well?
    fun mergeAverbisBrat(): JsonObject {
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
        val doc = StringBuilder(averbisData.documentText).also { sb ->
            idSetBrat.intersect(idSetAverbis).forEach {
                textboundData[it]?.let { data ->
                    mergedData.addJson(data)
                    crossOutModifyAnnotations(sb, data)
                }
            }
            idSetBrat.subtract(idSetAverbis).forEach {
                textboundData[it]?.let { data ->
                    mergedData.addJson(data)
                    crossOutModifyAnnotations(sb, data)
                }
            }
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

    // ToDo: put this into a checkbox list thingy?!
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

class BratController : Controller() {

}