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
//    private val outputTransform: OutputTransformationController by inject()

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
        OutputTransformationController.transformToBrat(response).forEach {
            File(outputData,"${it.first.fileName}.${it.first.extension}")
                .bufferedWriter()
                .use { out -> out.write(it.first.content) }
            File(outputData, "${it.second.fileName}.${it.second.extension}")
                .bufferedWriter()
                .use { out -> out.write(it.second.content) }
        }
        OutputTransformationController.getFilteredJson(response).forEach {
            File(outputData,"${it.fileName}.${it.extension}" )
                .bufferedWriter()
                .use { out -> out.write(it.content) }
        }
    }

    fun writeOutputToApp(response: List<AverbisResponse>) {
        mainView.outputFieldSet.children
            .filter { it::class.simpleName == "TableView" }
            .forEach { it.removeFromParent() }
        mainView.outputFieldSet.tableview(response.asObservable()) {
            prefHeight = 1000.0
            isEditable = false
            columnResizePolicy = SmartResize.POLICY

            readonlyColumn("File Name", AverbisResponse::inputFileName)
            readonlyColumn("File Path", AverbisResponse::inputFilePath)

            rowExpander(expandOnDoubleClick = true) {
                paddingLeft = expanderColumn.width

                tableview(it.items) {
                    columnResizePolicy = SmartResize.POLICY

                    readonlyColumn("Type", AverbisJsonEntry::type).contentWidth(padding = 50.0)
                    readonlyColumn("Text", AverbisJsonEntry::coveredText)
                }
            }
        }
//        mainView.outputField.text = ""
//        response.forEach {
//            val text = "--- ${it.inputFileName} ---\n" +
//                    "${it.transformToType(TransformationTypes.valueOf(mainView.outputTransformationTypeBox.selectedItem!!))}\n"
//            mainView.outputField.text += "${text}\n"
//        }
        mainView.outputDrawerItem.expanded = true
    }
}