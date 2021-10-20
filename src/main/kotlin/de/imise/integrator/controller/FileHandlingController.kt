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
                        .filter(File::isFile)
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

    fun writeOutputToDisk(response: List<AverbisResponse>, outputPath: String) {
        OutputTransformationController.transformToBrat(response).forEach {
            File(outputPath,"${it.first.fileName}.${it.first.extension}")
                .bufferedWriter()
                .use { out -> out.write(it.first.content) }
            File(outputPath, "${it.second.fileName}.${it.second.extension}")
                .bufferedWriter()
                .use { out -> out.write(it.second.content) }
        }
        OutputTransformationController.getFilteredJson(response).forEach {
            File(outputPath,"${it.fileName}.${it.extension}" )
                .bufferedWriter()
                .use { out -> out.write(it.content) }
        }
    }

//    fun writeOutputToApp(response: List<ResponseType>, fieldSet: Fieldset, onWritten: () -> Unit) {
//       fieldSet.children
//            .filter { it::class.simpleName == "TableView" }
//            .forEach { it.removeFromParent() }
//        fieldSet.tableview(response.asObservable()) {
//            prefHeight = 1000.0
//            isEditable = false
//            columnResizePolicy = SmartResize.POLICY
//
//            readonlyColumn("File Name", ResponseType::basename)
//            readonlyColumn("File Path", ResponseType::additionalColumn)
//
//            rowExpander(expandOnDoubleClick = true) {
//                paddingLeft = expanderColumn.width
//
//                tableview(it.items) {
//                    columnResizePolicy = SmartResize.POLICY
//
//                    readonlyColumn("Type", ResponseTypeEntry::type).contentWidth(padding = 50.0)
//                    readonlyColumn("Text", ResponseTypeEntry::text)
//                }
//            }
//        }
//        onWritten()
//    }

    fun writeMergedData(
        folder: File?,
        data: List<BratResponse>,
        crossOut: List<String>,
        modify: List<String>,
        removeSelected: Boolean,
        createTxt: Boolean
    ) {
        if (folder == null) return
        data.forEach { br ->
            File(folder, "${br.basename}.json")
                .bufferedWriter()
                .use { out -> out.write(br.mergeAverbisBrat(crossOut, modify, removeSelected).toJsonString(prettyPrint = true)) }
            if (createTxt) {
                File(folder, "${br.basename}.txt")
                    .bufferedWriter()
                    .use { out -> out.write(br.getTxt()) }
            }
        }
    }
}