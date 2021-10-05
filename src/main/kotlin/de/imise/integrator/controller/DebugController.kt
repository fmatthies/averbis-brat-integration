package de.imise.integrator.controller

import de.imise.integrator.extensions.ResponseType
import de.imise.integrator.view.MainView
import javafx.collections.ObservableList
import tornadofx.*
import java.io.File
import java.nio.file.Paths

class DebugController : Controller() {
    private val mainView : MainView by inject()
    private val dataFolder = "${Paths.get("").toAbsolutePath()}/data/Schulz-Arztbriefe/${mainView.pipelineNameField.text}/"

    private fun jsonResourceByName(name: String, ext: String) : File {
        return File("$dataFolder/$name.$ext")
    }

    fun postDocuments(documents: List<File>, averbisResponseList: ObservableList<ResponseType>) {
        documents.forEach { fi ->
            val response = AverbisResponse(fi)
            response.apply {
                readJson(jsonResourceByName(fi.nameWithoutExtension, "json").readText())
                setAnnotations(mainView.analysisModel.annotationValues.value)
            }
            averbisResponseList.add(response)
            Thread.sleep(1000)
        }
    }

    fun getDataFromRemote(random: Boolean = false): List<File> {
        if (random) {
            val data = File("$dataFolder/brat").list()
            if (data.isNullOrEmpty()) return listOf()
            data.shuffle()
            return data.slice(IntRange(0, 4)).map {
                File("$dataFolder/brat/$it")
            }
        }
        return listOf("Albers", "Beuerle", "Clausthal").run {
            this.map { File("$dataFolder/brat/$it.ann") }
                .plus( this.map { File("$dataFolder/$it.json") } )
        }
    }
}