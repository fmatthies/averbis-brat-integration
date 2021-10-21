package de.imise.integrator.controller

import de.imise.integrator.view.MainView
import javafx.collections.ObservableList
import javafx.scene.control.ProgressBar
import tornadofx.*
import java.io.File
import java.nio.file.Paths

class DebugController : Controller() {
    private val mainView : MainView by inject()
    private val logging: LoggingController by inject()
    private val dataFolder = "${Paths.get("").toAbsolutePath()}/data/Schulz-Arztbriefe/${mainView.pipelineNameField.text}/"

    private val sleep: Long = 4000

    private fun jsonResourceByName(name: String, ext: String) : File {
        return File("$dataFolder/$name.$ext")
    }

    fun postDocuments(
        documents: List<File>,
        averbisResponseList: ObservableList<AverbisResponse>,
        fxTask: FXTask<*>
    ) {
        val maxDocs = documents.size
        documents.forEachIndexed { index, file ->
            val response = AverbisResponse(file.name, file.parent)
            response.apply {
                readJson(jsonResourceByName(file.nameWithoutExtension, "json").readText())
                setAnnotations(mainView.averbisAnalysisModel.annotationValues.value)
            }
            averbisResponseList.add(response)
            fxTask.updateProgress((index + 1).toDouble(), maxDocs.toDouble())
            Thread.sleep(1000)
        }
    }

    fun getDataFromRemote(random: Boolean = false): List<InMemoryFile> {
        Thread.sleep(sleep)
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

    fun transferData() {
        Thread.sleep(sleep)
        logging.logBrat("Thread slept for $sleep milliseconds")
    }
}