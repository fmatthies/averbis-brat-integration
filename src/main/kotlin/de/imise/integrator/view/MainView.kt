package de.imise.integrator.view

import de.imise.integrator.controller.*
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
    private val remoteController: RemoteController by inject()

    val setupModel = SetupModel(Setup(
        url = app.config.getProperty(AVERBIS_URL_CONFIG_STRING),
        apiToken = app.config.getProperty(AVERBIS_API_TOKEN),
        projectName = app.config.getProperty(DEFAULT_PROJECT_CONFIG_STRING),
        pipelineName = app.config.getProperty(DEFAULT_PIPELINE_CONFIG_STRING)
    ))

    val inputDataModel = InputModel(Input())

    val analysisModel = AnalysisModel(Analysis(
        annotationValues = app.config.getProperty(ANNOTATION_TYPES).split(",").toMutableList().asObservable()
    ))

    // Averbis Tab
    var urlField: TextField by singleAssign()
    var apiTokenField: TextField by singleAssign()
    var projectNameField: TextField by singleAssign()
    var pipelineNameField: TextField by singleAssign()
//    var outputField: TableView<AverbisResponse> by singleAssign()
    var outputFieldSet: Fieldset by singleAssign()
    var logFieldAverbis: TextArea by singleAssign()
    var languageGroup: ToggleGroup by singleAssign()
    var inputDirField: TextField by singleAssign()
    var inputDirButton: Button by singleAssign()
    val inputSelectionMode = SimpleStringProperty()
    var outputDirField: TextField by singleAssign()
    var chooseOutputButton: Button by singleAssign()
    var outputDrawerItem: DrawerItem by singleAssign()
    var outputModeBox: ComboBox<String> by singleAssign()
    val outputMode = SimpleStringProperty()

    // Brat Tab
    var hostField: TextField by singleAssign()
    var usernameField: TextField by singleAssign()
    var passwordField: PasswordField by singleAssign()
    var remotePortField: TextField by singleAssign()
    var bratDataFolderField: TextField by singleAssign()
    var bratSubfolderField: TextField by singleAssign()
    var logFieldBrat: TextArea by singleAssign()

    val tabBinding = { tabPane: TabPane, parent: HBox ->
        val x = parent.widthProperty().doubleBinding(tabPane.tabs) {
            it as Double / tabPane.tabs.size - 25
        }
        x
    }

    companion object {
        const val AVERBIS_URL_CONFIG_STRING = "default_url"
        const val AVERBIS_API_TOKEN = "default_api_token"
        const val DEFAULT_PROJECT_CONFIG_STRING = "default_project"
        const val DEFAULT_PIPELINE_CONFIG_STRING = "default_pipeline"
        const val DEFAULT_LANGUAGES_CONFIG_LIST = "default_languages"
        const val ANNOTATION_TYPES = "annotation_types"
        const val DEFAULT_REMOTE_HOST = "default_remote"
        const val DEFAULT_REMOTE_PORT = "default_port"
        const val DEFAULT_REMOTE_DEST = "default_destination"
    }

    override val root = hbox {
        var fis: List<File> = listOf()
        var responseList: List<AverbisResponse> = listOf()

        prefHeight = 750.0
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
                                field("Output Mode") {
                                    outputModeBox = combobox(outputMode, listOf("Remote", "Local")).apply {
                                        selectionModel.selectFirst()
                                        setOnAction {
                                            when (outputMode.value) {
                                                "Local" -> {
                                                    outputDirField.isDisable = false
                                                    chooseOutputButton.isDisable = false
                                                }
                                                "Remote" -> {
                                                    outputDirField.isDisable = true
                                                    chooseOutputButton.isDisable = true
                                                }
                                            }
                                        }
                                    }
                                }
                                field("Select Output") {
                                    outputDirField = textfield(analysisModel.output).apply {
                                        required()
                                        isDisable = true
                                    }
                                    chooseOutputButton = button("Choose Folder") {
                                        action {
                                            val dir = chooseDirectory("Choose Folder")
                                            outputDirField.text = dir?.absolutePath
                                        }
                                    }.apply { isDisable = true }
                                }
                                //ToDo: add viewer (and selector) for which path parts should be used for later output path
                                //ToDo: separate "post data" from "analyze data" so that filtering can be done later
                                // this allows us to extract `types` from json and they don't have to be declared in the config
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
                                                        val analysis: Analysis = analysisModel.item

                                                        //ToDo: progress indicator that shows a real progress and not just spinning wheel
                                                        if (setup.hasNoNullProperties() and
                                                            input.hasNoNullProperties() and
                                                            (analysis.hasNoNullProperties() or (outputMode.value == "Remote"))) {
                                                            runAsyncWithProgress {
                                                                averbisController.postDocuments(fis)
                                                            } ui { response ->
                                                                if (analysis.outputIsProperPath() && outputMode.value == "Local") {
                                                                    fileHandlingController.writeOutputToDisk(response, analysis.outputData!!)
                                                                } else {
                                                                    responseList = response
                                                                    fileHandlingController.writeOutputToApp(response)
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
                            fieldset("Log") {
                                logFieldAverbis = textarea { isEditable = false }
                            }
                        }
                    }
                    outputDrawerItem = item("Output") {
                        form {
                            outputFieldSet = fieldset("Output") {
                            }
                        }
                    }
                }
            }
            tab("Brat") {
                form {
                    //ToDo: use models here as well
                    fieldset("Setup") {
                        field("Remote Host") {
                            hostField = textfield(app.config.getProperty(DEFAULT_REMOTE_HOST)).apply {  }
                        }
                        field("Remote Port") {
                            remotePortField = textfield(app.config.getProperty(DEFAULT_REMOTE_PORT)).apply {  }
                        }
                        field("Username") {
                            usernameField = textfield().apply {  }
                        }
                        field("Password") {
                            passwordField = passwordfield().apply {  }
                        }
                        field("Brat Data Folder") {
                            bratDataFolderField = textfield(app.config.getProperty(DEFAULT_REMOTE_DEST)).apply {  }
                        }
                    }
                    fieldset("Transfer") {
                        field("Subfolder (optional)") {
                            bratSubfolderField = textfield()
                            tooltip("Transfers files to specified subfolder under Brat data folder;" +
                                    "if empty the Averbis pipeline name is used as subfolder.")
                        }
                        field {
                            borderpane {
                                padding = insets(10)
                                center {
                                    vbox {
                                        alignment = Pos.CENTER
                                        spacing = 10.0
                                        button("Transfer data") {
                                            setPrefSize(200.0, 40.0)
                                            action {
                                                remoteController.FileTransfer().apply {
                                                    transferData(responseList)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    fieldset("Receive") {

                    }
                    fieldset("Log") {
                        logFieldBrat = textarea { isEditable = false }
                    }
                }
            }
        }
    }
}