package de.imise.integrator.view

import de.imise.integrator.controller.AverbisController
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.stage.FileChooser
import tornadofx.*
import java.io.File

class MainView : View("Averbis & Brat Integrator") {
    private val averbisController: AverbisController by inject()
    private val averbisUrl = "http://10.230.7.129:8445/health-discovery"
    private val projectName = "1000PA"
    private val pipelineName = "deid"

    var urlField: TextField by singleAssign()
    var apiTokenField: TextField by singleAssign()
    var projectNameField: TextField by singleAssign()
    var pipelineNameField: TextField by singleAssign()
    var outputField: TextArea by singleAssign()
    var languageGroup: ToggleGroup by singleAssign()
    var inputDirField: TextField by singleAssign()
    var inputDirButton: Button by singleAssign()
    val inputSelectionMode = SimpleStringProperty()
    var outputDirField: TextField by singleAssign()
    var progress: ProgressBar by singleAssign()

    val tabBinding = { tabPane: TabPane, parent: HBox ->
        val x = parent.widthProperty().doubleBinding(tabPane.tabs) {
            it as Double / tabPane.tabs.size - 25
        }
        x
    }

    override val root = hbox {
        var fis: List<File> = listOf()

        prefHeight = 700.0
        prefWidth = 550.0
        tabpane {
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
            tabMinWidthProperty().bind(tabBinding(this, this@hbox))
            tabMaxWidthProperty().bind(tabBinding(this, this@hbox))

            tab("Averbis") {
                form { //ToDo: check -> no empty fields in form!!
                    fieldset("Setup") {
                        field("Health Discovery URL") {
                            urlField = textfield(averbisUrl)
                        }
                        field("API Token") {
                            apiTokenField = textfield()
                        }
                        field("Project Name") {
                            projectNameField = textfield(projectName)
                        }
                        field("Pipeline Name") {
                            pipelineNameField = textfield(pipelineName)
                        }
                        field("Language") {
                            languageGroup = togglegroup {
                                radiobutton("de").isSelected = true
                                radiobutton("en")
                            }
                        }
                    }
                    fieldset("Data Selection") {
                        field("Selection Mode") {
                            val selects = FXCollections.observableArrayList("File(s)", "Folder")
                            combobox(inputSelectionMode, selects).apply {
                                selectionModel.selectFirst()
                                setOnAction {
                                    inputDirButton.text = "Choose ${inputSelectionMode.value}"
                                    fis = listOf()
                                    inputDirField.text = ""
                                }
                            }
                        }
                        field("Select Input") {
                            inputDirField = textfield()
                            inputDirButton = button("Choose ${inputSelectionMode.value}") {
                                action {
                                    //ToDo extract this to controller
                                    when (inputSelectionMode.value) {
                                        "File(s)" -> {
                                            fis = chooseFile(
                                                title = "Choose File(s)",
                                                filters = arrayOf(FileChooser.ExtensionFilter("text files", "*.txt")),
                                                mode = FileChooserMode.Multi
                                            )
                                            inputDirField.text = fis.joinToString { it.name }
                                            }
                                        "Folder" -> {
                                            val dir = chooseDirectory("Choose Folder")
                                            //ToDo: transform list of txt files in folder to `fis`
                                            inputDirField.text = dir?.absolutePath
                                        }
                                        else -> {
                                            fis = listOf()
                                        }
                                    }

                                }
                            }
                        }
                    }
                    fieldset("Analyze Data") {
                        field("Select Output") {
                            outputDirField = textfield()
                            button("Choose Folder") {
                                action {
                                    val dir = chooseDirectory("Choose Folder")
                                    outputDirField.text = dir?.absolutePath
                                }
                            }
                        }
                        field {
                            borderpane {
                                padding = insets(10)
                                center {
                                    vbox {
                                        alignment = Pos.CENTER
                                        spacing = 10.0
                                        button("Post data") {
                                            setPrefSize(200.0, 40.0)
                                            action {
                                                averbisController.postDocuments(fis)
                                            }
                                        }
                                        progress = progressbar(0.0) {
                                            prefWidth = 200.0
                                        }
                                    }
                                }
                            }
                        }
                    }
                    fieldset("Output") {
                        outputField = textarea()
                    }
                }
            }
            tab("Brat") {
                label("Brat")
            }
        }
    }
}