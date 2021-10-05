package de.imise.integrator.controller

import com.beust.klaxon.JsonArray
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
    override val pathname: String = annFile?.parent ?: "none"
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

    fun mergeAverbisBrat(): JsonObject {
        val idSetBrat = textboundData.keys
        val idSetAverbis = averbisData!!.getData().keys
        val mergedData = mutableListOf<JsonObject>()
        fun MutableList<JsonObject>.addJson(sourceData: BratAnnotation, id: Int? = null) {
            this.add(
                json { obj(
                    "begin" to sourceData.begin,
                    "end" to sourceData.end,
                    "type" to "de.averbis.types.health.${sourceData.type}",
                    "coveredText" to sourceData.text,
                    "id" to (id ?: sourceData.id),
                ) }
            )
        }

        idSetBrat.intersect(idSetAverbis).forEach {
            textboundData[it]?.let { data -> mergedData.addJson(data) }
        }
        idSetBrat.subtract(idSetAverbis).forEach {
            textboundData[it]?.let { data -> mergedData.addJson(data) }
        }
        return json { obj("annotationDtos" to array(mergedData.toList())) }
    }
}

class BratController : Controller() {

}