package de.imise.integrator.controller

import de.imise.integrator.extensions.ResponseType
import de.imise.integrator.view.MainView
import javafx.collections.ObservableList
import javafx.scene.control.ProgressBar
import tornadofx.*
import java.io.File
import java.nio.file.Paths

class DebugController : Controller() {
    private val mainView : MainView by inject()
    private val dataFolder = "${Paths.get("").toAbsolutePath()}/data/Schulz-Arztbriefe/${mainView.pipelineNameField.text}/"

    private fun jsonResourceByName(name: String, ext: String) : File {
        return File("$dataFolder/$name.$ext")
    }

    fun postDocuments(
        documents: List<File>,
        averbisResponseList: ObservableList<AverbisResponse>,
        averbisProgress: ProgressBar
    ) {
        val maxDocs = documents.size
        var currentDoc = 0
        documents.forEach { fi ->
            currentDoc += 1
            val response = AverbisResponse(fi.name, fi.parent)
            response.apply {
                readJson(jsonResourceByName(fi.nameWithoutExtension, "json").readText())
                setAnnotations(mainView.analysisModel.annotationValues.value)
            }
            averbisResponseList.add(response)
            averbisProgress.progress = (currentDoc/maxDocs).toDouble()
            Thread.sleep(1000)
        }
    }

    fun getDataFromRemote(random: Boolean = false): List<InMemoryFile> {
        if (random) {
            val data = File("$dataFolder/brat").list()
            if (data.isNullOrEmpty()) return listOf()
            data.shuffle()
            return data.slice(IntRange(0, 4))
                .map {
                    val fi = File("$dataFolder/brat/$it")
                    InMemoryFile( baseName = fi.nameWithoutExtension, content = fi.readBytes(), extension = fi.extension )
                }
        }
        return listOf("Albers", "Beuerle", "Clausthal").run {
            this.map {
                val fi = File("$dataFolder/brat/$it.ann")
                InMemoryFile( baseName = fi.nameWithoutExtension, content = fi.readBytes(), extension = fi.extension )
            }.plus(
                this.map {
                    val fi = File("$dataFolder/$it.json")
                    InMemoryFile( baseName = fi.nameWithoutExtension, content = fi.readBytes(), extension = fi.extension )
            } )
        }
    }
}