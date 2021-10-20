package de.imise.integrator.view

import de.imise.integrator.controller.*
import de.imise.integrator.extensions.ResponseType
import de.imise.integrator.extensions.withActionButton
import de.imise.integrator.extensions.withTableFrom
import de.imise.integrator.model.*
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.geometry.Side
import javafx.scene.control.*
import javafx.scene.layout.HBox
import tornadofx.*
import java.io.File

// ToDo: disable buttons only while processing; right now the whole field gets disabled

class MainView : View("Averbis & Brat Integrator") {
    private val averbisController: AverbisController by inject()
    private val fileHandlingController: FileHandlingController by inject()
    private val remoteController: RemoteController by inject()
    private val debugController: DebugController by inject()
    private val logging: LoggingController by inject()

    val averbisResponseList = mutableListOf<AverbisResponse>().asObservable()
    val bratResponseList = mutableListOf<BratResponse>().asObservable()

    val averbisSetupModel = AverbisSetupModel(
        AverbisSetup(
            url = app.config.getProperty(AVERBIS_URL_CONFIG_STRING),
            apiToken = app.config.getProperty(AVERBIS_API_TOKEN),
            projectName = app.config.getProperty(DEFAULT_PROJECT_CONFIG_STRING),
            pipelineName = app.config.getProperty(DEFAULT_PIPELINE_CONFIG_STRING)
        ))

    val averbisInputDataModel = AverbisInputModel(AverbisInput())

    val averbisAnalysisModel = AverbisAnalysisModel(
        AverbisAnalysis(
            annotationValues = app.config.getProperty(ANNOTATION_TYPES).split(",").toMutableList().asObservable()
        ))

    val bratSetupModel = BratSetupModel(
        BratSetup(
            host = app.config.getProperty(DEFAULT_REMOTE_HOST),
            port = app.config.getProperty(DEFAULT_REMOTE_PORT),
            dataFolder = app.config.getProperty(DEFAULT_REMOTE_DEST)
        ))

    val bratReceiveModel = BratReceiveModel(BratReceive())

    val bratTransferModel = BratTransferModel(BratTransfer())

    // General
    var offlineCheck: CheckMenuItem by singleAssign()  //ToDo: remove this when going live

    // ToDo: LogField --> scroll (to end) automatically
    // Averbis Tab
    var urlField: TextField by singleAssign()
    var apiTokenField: TextField by singleAssign()
    var projectNameField: TextField by singleAssign()
    var pipelineNameField: TextField by singleAssign()
    var logFieldAverbis: TextArea by singleAssign()
    var languageGroup: ToggleGroup by singleAssign()
    var inputDirField: TextField by singleAssign()
    var inputDirButton: Button by singleAssign()
    val inputSelectionMode = SimpleStringProperty()
    var outputDirField: TextField by singleAssign()
    var chooseOutputButton: Button by singleAssign()
    var outputDrawerItemAverbis: DrawerItem by singleAssign()
    var outputModeBox: ComboBox<String> by singleAssign()
    val outputMode = SimpleStringProperty()
    val averbisProgress = ProgressBar(0.0)

    // Brat Tab
    var hostField: TextField by singleAssign()
    var usernameField: TextField by singleAssign()
    var passwordField: PasswordField by singleAssign()
    var remotePortField: TextField by singleAssign()
    var bratDataFolderField: TextField by singleAssign()
    var bratTransferSubfolderField: TextField by singleAssign()
    var bratReceiveSubfolderField: TextField by singleAssign()
    var logFieldBrat: TextArea by singleAssign()
    var outputDrawerItemBrat: DrawerItem by singleAssign()
    var mergeDataButton: Button by singleAssign()

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

    override val root = borderpane {
        var fis: List<File> = listOf()

        prefHeight = 800.0
        prefWidth = 550.0

        top = menubar {
            menu("Options") {
                menu("Debug") {
                    offlineCheck = checkmenuitem("Offline") { isSelected = true }
                }
            }
        }

        center = hbox {
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
                                        urlField = textfield(averbisSetupModel.url).apply { required() }
                                    }
                                    field("API Token") {
                                        apiTokenField = textfield(averbisSetupModel.apiToken).apply { required() }
                                    }
                                    field("Project Name") {
                                        projectNameField = textfield(averbisSetupModel.projectName).apply { required() }
                                    }
                                    field("Pipeline Name") {
                                        pipelineNameField = textfield(averbisSetupModel.pipelineName).apply { required() }
                                    }
                                    field("Language") {
                                        languageGroup = togglegroup {
                                            bind(averbisSetupModel.language)
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
                                        inputDirField = textfield(averbisInputDataModel.input).apply { required() }
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
                                            averbisAnalysisModel.annotationValues.value.forEach {
                                                checkbox(it).apply {
                                                    isSelected = true
                                                    action {
                                                        if (isSelected) {
                                                            averbisAnalysisModel.annotationValues.value.add(this.text)
                                                        } else {
                                                            averbisAnalysisModel.annotationValues.value.remove(this.text)
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
                                        outputDirField = textfield(averbisAnalysisModel.output).apply {
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
                                    field().withActionButton("Post Data") {
                                        averbisSetupModel.commit()
                                        averbisInputDataModel.commit()
                                        averbisAnalysisModel.commit()

                                        val averbisSetup: AverbisSetup = averbisSetupModel.item
                                        val averbisInput: AverbisInput = averbisInputDataModel.item
                                        val averbisAnalysis: AverbisAnalysis = averbisAnalysisModel.item

                                        //ToDo: progress indicator that shows a real progress and not just spinning wheel
                                        if (averbisSetup.hasNoNullProperties() and
                                            averbisInput.hasNoNullProperties() and
                                            (averbisAnalysis.hasNoNullProperties() or (outputMode.value == "Remote"))
                                        ) {
                                            outputDrawerItemAverbis.expanded = true
                                            isDisable = true
                                            averbisResponseList.clear()
                                            runAsync {
                                                when (offlineCheck.isSelected) {
                                                    true -> debugController.postDocuments(fis, averbisResponseList, averbisProgress)
                                                    false -> averbisController.postDocuments(fis, averbisResponseList, averbisAnalysis)
                                                }
                                            } ui { isDisable = false }
                                        }
                                    }
                                }
                                fieldset("Log") {
                                    logFieldAverbis = textarea { isEditable = false }
                                }
                            }
                        }
                        outputDrawerItemAverbis = item("Output") {
                            form {
                                fieldset("Output").withTableFrom(averbisResponseList as ObservableList<ResponseType>) {
                                    sequenceOf(
                                        field("Progress") { averbisProgress }
                                    )
                                }
                            }
                        }
                    }
                }
                tab("Brat") {
                    drawer(side = Side.RIGHT) {
                        item("General", expanded = true) {
                            form {
                                fieldset("Setup") {
                                    field("Remote Host") {
                                        hostField = textfield(bratSetupModel.host).apply { required() }
                                    }
                                    field("Remote Port") {
                                        remotePortField = textfield(bratSetupModel.port).apply { required() }
                                    }
                                    field("Username") {
                                        usernameField = textfield(bratSetupModel.username).apply { required() }
                                    }
                                    field("Password") {
                                        passwordField = passwordfield(bratSetupModel.password).apply { required() }
                                    }
                                    field("Brat Data Folder") {
                                        bratDataFolderField = textfield(bratSetupModel.dataFolder).apply { required() }
                                    }
                                }
                                fieldset("Transfer") {
                                    field("Subfolder (optional)") {
                                        bratTransferSubfolderField = textfield(bratTransferModel.subfolder)
                                        tooltip(
                                            "Transfers files to specified subfolder under Brat data folder;" +
                                                    "if empty the Averbis pipeline name is used as subfolder."
                                        )
                                    }
                                    field().withActionButton("Transfer data") {
                                        bratSetupModel.commit()
                                        bratTransferModel.commit()

                                        val bratSetup: BratSetup = bratSetupModel.item
                                        val bratTransfer: BratTransfer = bratTransferModel.item

                                        if (bratSetup.hasNoNullProperties()) {
                                            isDisable = true
                                            remoteController.FileTransfer().apply {
                                                setupTransfer(bratSetup, bratTransfer)
                                                transferData(averbisResponseList)
                                            }
                                            isDisable = false
                                        }
                                    }
                                }
                                fieldset("Receive") {
                                    field("Subfolder") {
                                        bratReceiveSubfolderField = textfield(bratReceiveModel.subfolder).apply { required() }
                                    }
                                    field().withActionButton("Get Data") {
                                        bratSetupModel.commit()
                                        bratReceiveModel.commit()

                                        val bratSetup: BratSetup = bratSetupModel.item
                                        val bratReceive: BratReceive = bratReceiveModel.item

                                        if (bratSetup.hasNoNullProperties() && bratReceive.hasNoNullProperties()) {
                                            isDisable = true
                                            bratResponseList.clear()
                                            runAsync {
                                                when (offlineCheck.isSelected) {
                                                    true -> debugController.getDataFromRemote()
                                                    false -> remoteController.FileTransfer().run {
                                                        setupReceive(bratSetup, bratReceive)
                                                        getDataFromRemote()
                                                    }
                                                }
                                            } ui { data ->
                                                data
                                                    .groupBy { it.baseName }
                                                    .values
                                                    .map { fileList ->
                                                        BratResponse(
                                                            fileList.find { it.extension == "ann" },
                                                            fileList.find { it.extension == "json" }
                                                        )
                                                    }.forEach { bratResponseList.add(it) }
                                                outputDrawerItemBrat.expanded = true
                                                isDisable = false
                                                openInternalWindow<TemporaryFileDeletionFragment>()
                                            }
                                        }
                                    }
                                }
                                fieldset("Log") {
                                    logFieldBrat = textarea { isEditable = false }
                                }
                            }
                        }
                        outputDrawerItemBrat = item("Output") {
                            borderpane {
                                center = form {
                                    fieldset("Output").withTableFrom(bratResponseList as ObservableList<ResponseType>) {
                                        sequenceOf(
                                            field { }
                                        )
                                    }
                                }
                                bottom = vbox {
                                    alignment = Pos.CENTER
                                    spacing = 10.0
                                    paddingAll = 10.0
                                    mergeDataButton = button("Merge Data") {
                                        setPrefSize(200.0, 40.0)
                                        action {
                                            openInternalWindow<MergeFragment>()
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