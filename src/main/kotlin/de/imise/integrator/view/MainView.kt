package de.imise.integrator.view

import de.imise.integrator.controller.AverbisController
import de.imise.integrator.controller.FileHandlingController
import de.imise.integrator.controller.TransformationTypes
import de.imise.integrator.model.*
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.geometry.Side
import javafx.scene.control.*
import javafx.scene.layout.HBox
import tornadofx.*
import java.io.File

class MainView : View("Averbis & Brat Integrator") {
    private val averbisController: AverbisController by inject()
    private val fileHandlingController: FileHandlingController by inject()

    val setupModel = SetupModel(Setup(
        url = app.config.getProperty(AVERBIS_URL_CONFIG_STRING),
        projectName = app.config.getProperty(DEFAULT_PROJECT_CONFIG_STRING),
        pipelineName = app.config.getProperty(DEFAULT_PIPELINE_CONFIG_STRING)
    ))

    val inputDataModel = InputModel(Input())

    val analysisModel = AnalysisModel(Analysis(
        annotationValues = app.config.getProperty(ANNOTATION_TYPES).split(",").toMutableList().asObservable()
    ))

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
    var outputDrawerItem: DrawerItem by singleAssign()

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
        const val ANNOTATION_TYPES = "annotation_types"
    }

    override val root = hbox {
        var fis: List<File> = listOf()

        prefHeight = 600.0
        prefWidth = 550.0
        tabpane {
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
            tabMinWidthProperty().bind(tabBinding(this, this@hbox))
            tabMaxWidthProperty().bind(tabBinding(this, this@hbox))

            tab("Averbis") {
                drawer(side = Side.RIGHT) {
                    item("General", expanded = true) {
                        form {
                            fieldset("Setup") {
                                field("Health Discovery URL") {
                                    urlField = textfield(setupModel.url).apply { required() }
                                }
                                field("API Token") {
                                    apiTokenField = textfield(setupModel.apiToken).apply { required() }
                                }
                                field("Project Name") {
                                    projectNameField = textfield(setupModel.projectName).apply { required() }
                                }
                                field("Pipeline Name") {
                                    pipelineNameField = textfield(setupModel.pipelineName).apply { required() }
                                }
                                field("Language") {
                                    languageGroup = togglegroup {
                                        bind(setupModel.language)
                                        app.config.getProperty(DEFAULT_LANGUAGES_CONFIG_LIST).split(",").forEach {
                                            val rad = radiobutton(it)
                                            if (this.selectedToggle as ToggleButton? == null) {
                                                this.selectToggle(rad)
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
                                    inputDirField = textfield(inputDataModel.input).apply { required() }
                                    inputDirButton = button("Choose ${inputSelectionMode.value}") {
                                        action {
                                            fis = fileHandlingController.readFiles(inputSelectionMode.value)
                                        }
                                    }
                                }
                            }
                            fieldset("Analyze Data") {
                                squeezebox {
                                    fold("Annotation Values") {
                                        analysisModel.annotationValues.value.forEach {
                                            checkbox(it).apply {
                                                isSelected = true
                                                action {
                                                    if (isSelected) {
                                                        analysisModel.annotationValues.value.add(this.text)
                                                    } else {
                                                        analysisModel.annotationValues.value.remove(this.text)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
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
                                                        setupModel.commit()
                                                        inputDataModel.commit()
                                                        analysisModel.commit()

                                                        val setup: Setup = setupModel.item
                                                        val input: Input = inputDataModel.item
//                                                        val analysis: Analysis = analysisModel.item

                                                        if (setup.hasNoNullProperties() and input.hasNoNullProperties()) {
                                                            runAsyncWithProgress {
                                                                averbisController.postDocuments(fis)
                                                            } ui { response ->
                                                                response.forEach {
                                                                    val text = "---${it.inputFileName}---\n" +
                                                                            "${it.transformToType(TransformationTypes.BRAT)}\n"
                                                                    outputField.text += text
                                                                }
                                                                outputDrawerItem.expanded = true
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    outputDrawerItem = item("Output") {
                        form {
                            fieldset("Output") {
                                outputField = textarea {
                                    prefHeight = 1000.0
                                    isEditable = false
                                }
                            }
                        }
                    }
                }
            }
            tab("Brat") {
                label("Brat")
            }
        }
    }
}