package de.imise.integrator.controller

import de.imise.integrator.view.MainView
import javafx.stage.FileChooser
import tornadofx.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors.toList

class FileHandlingController : Controller() {
    private val mainView: MainView by inject()

    fun readFiles(inputMode: String): List<File> {
        var files: List<File> = listOf()

        when (inputMode) {
            "File(s)" -> {
                files = chooseFile(
                    title = "Choose File(s)",
                    filters = arrayOf(FileChooser.ExtensionFilter("text files", "*.txt")),
                    mode = FileChooserMode.Multi
                )
                mainView.inputDirField.text = files.joinToString { it.name }
            }
            "Folder" -> {
                val dir = chooseDirectory("Choose Folder")
                if (dir != null) {
                    files = Files.list(dir.toPath())
                        .map(Path::toFile)
                        .collect(toList())
                }
                mainView.inputDirField.text = dir?.absolutePath
            }
            else -> {
                files = listOf()
            }
        }
        return files
    }

    fun writeOutputToDisk(response: List<AverbisResponse>, outputData: String) {
        val ext = mapOf(
            TransformationTypes.BRAT.name to "ann",
//            TransformationTypes.STRING.name to "txt",
//            TransformationTypes.JSON.name to "json"
        )
        response.forEach {
            File(outputData,
                "${it.inputFileName}.${ext.getValue(mainView.outputTransformationTypeBox.selectedItem!!)}")
                .bufferedWriter()
                .use { out ->
                    out.write(
                        it.transformToType(TransformationTypes.valueOf(mainView.outputTransformationTypeBox.selectedItem!!))
                            .replace("\\r\\n?", "\n")
                )
            }
            File(outputData,"${it.inputFileName}.txt")
                .bufferedWriter()
                .use { out ->
                    out.write(it.documentText.replace("\\r\\n?", "\n"))
                }
        }
    }

    fun writeOutputToApp(response: List<AverbisResponse>) {
        mainView.outputField.text = ""
        response.forEach {
            val text = "--- ${it.inputFileName} ---\n" +
                    "${it.transformToType(TransformationTypes.valueOf(mainView.outputTransformationTypeBox.selectedItem!!))}\n"
            mainView.outputField.text += "${text}\n"
        }
        mainView.outputDrawerItem.expanded = true
    }
}