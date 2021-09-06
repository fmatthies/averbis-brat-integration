package de.imise.integrator.view

import de.imise.integrator.controller.AverbisController
import de.imise.integrator.controller.FileHandlingController
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
    private val fileHandlingController: FileHandlingController by inject()

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

    companion object {
        const val AVERBIS_URL_CONFIG_STRING = "default_url"
        const val DEFAULT_PROJECT_CONFIG_STRING = "default_project"
        const val DEFAULT_PIPELINE_CONFIG_STRING = "default_pipeline"
        const val DEFAULT_LANGUAGES_CONFIG_LIST = "default_languages"
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
                            urlField = textfield(app.config.getProperty(AVERBIS_URL_CONFIG_STRING))
                        }
                        field("API Token") {
                            apiTokenField = textfield()
                        }
                        field("Project Name") {
                            projectNameField = textfield(app.config.getProperty(DEFAULT_PROJECT_CONFIG_STRING))
                        }
                        field("Pipeline Name") {
                            pipelineNameField = textfield(app.config.getProperty(DEFAULT_PIPELINE_CONFIG_STRING))
                        }
                        field("Language") {
                            languageGroup = togglegroup {
                                app.config.getProperty(DEFAULT_LANGUAGES_CONFIG_LIST).split(",").forEach {
                                    val rad = radiobutton(it)
                                    if (this.selectedToggle as ToggleButton? == null) {
                                        rad.isSelected = true
                                    }
                                }
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
                                    fis = fileHandlingController.readFiles(inputSelectionMode.value)
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
                                                runAsyncWithProgress {
                                                    averbisController.postDocuments(fis)
                                                } ui { response ->
                                                    outputField.text = response
                                                }
                                            }
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