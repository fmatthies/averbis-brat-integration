package de.imise.integrator.controller

import de.imise.integrator.view.MainView
import tornadofx.*
import java.io.File
import java.nio.file.Paths

class DebugController : Controller() {
    private val mainView : MainView by inject()

    private fun jsonResourceByName(name: String) : File {
        val fileString = "${Paths.get("").toAbsolutePath()}" +
                "/data/Schulz-Arztbriefe/${mainView.pipelineNameField.text}/${name}.json"
        return File(fileString)
    }

    fun postDocuments(documents: List<File>) : List<AverbisResponse> {
        return documents.map { fi ->
            val response = AverbisResponse(fi)
            response.apply {
                readJson(jsonResourceByName(fi.nameWithoutExtension).readText())
                setAnnotations(mainView.analysisModel.annotationValues.value)
            }
        }
    }
}