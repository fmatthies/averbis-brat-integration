package de.imise.integrator.controller

import de.imise.integrator.extensions.ResponseType
import de.imise.integrator.extensions.ResponseTypeEntry
import javafx.collections.ObservableList
import tornadofx.*
import java.io.File

//ToDo: compare json entries with the brat entries and merge them to one;
// e.g. replacing json entries with different brat entries if they share the same id
// in the end I want to be able to put them all back to a json format (according to at least minimal averbis format)

data class BratAnnotation(val id: Int, val bratType: String, override val type: String,
                          val begin: Int, val end: Int, override val text: String) : ResponseTypeEntry

class BratResponse(annFile: File?, jsonFile: File?): ResponseType {
    val averbisData = jsonFile?.let { AverbisResponse(it).apply { this.readJson(jsonFile.readText()) } }
    var textboundData = mutableMapOf<Int, BratAnnotation>()
    override val info1: String = annFile?.nameWithoutExtension ?: "none"
    override val info2: String = annFile?.parent ?: "none"
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
}

class BratController : Controller() {

}