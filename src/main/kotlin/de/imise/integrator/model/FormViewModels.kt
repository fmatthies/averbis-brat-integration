package de.imise.integrator.model

import javafx.beans.property.Property
import tornadofx.*
import java.io.File


class AverbisSetup(
    url: String = "",
    apiToken: String? = "",
    projectName: String? = "",
    pipelineName: String? = "",
    language: String? = ""
) {
    var url by property(url)
    fun urlProperty() = getProperty(AverbisSetup::url)

    var apiToken by property(apiToken)
    fun apiTokenProperty() = getProperty(AverbisSetup::apiToken)

    var projectName by property(projectName)
    fun projectNameProperty() = getProperty(AverbisSetup::projectName)

    var pipelineName by property(pipelineName)
    fun pipelineNameProperty() = getProperty(AverbisSetup::pipelineName)

    var language by property(language)
    fun languageProperty() = getProperty(AverbisSetup::language)

    fun hasNoNullProperties() : Boolean{
        if (!url.isNullOrBlank()) {
            if (!apiToken.isNullOrBlank()) {
                if (!projectName.isNullOrBlank()) {
                    if (!pipelineName.isNullOrBlank()) {
                        if (!language.isNullOrBlank()) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }
}

class BratSetup(
    host: String = "",
    port: String? = "",
    username: String? = "",
    password: String? = "",
    dataFolder: String? = ""
) {
    var host by property(host)
    fun hostProperty() = getProperty(BratSetup::host)

    var port by property(port)
    fun portProperty() = getProperty(BratSetup::port)

    var username by property(username)
    fun usernameProperty() = getProperty(BratSetup::username)

    var password by property(password)
    fun passwordProperty() = getProperty(BratSetup::password)

    var dataFolder by property(dataFolder)
    fun dataFolderProperty() = getProperty(BratSetup::dataFolder)

    fun hasNoNullProperties() : Boolean{
        if (!host.isNullOrBlank()) {
            if (!port.isNullOrBlank()) {
                if (!username.isNullOrBlank()) {
                    if (!password.isNullOrBlank()) {
                        if (!dataFolder.isNullOrBlank()) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }
}

class AverbisInput(
    inputData: String? = ""
) {
    var inputData by property(inputData)
    fun inputDataProperty() = getProperty(AverbisInput::inputData)

    fun hasNoNullProperties() : Boolean {
        if (!inputData.isNullOrBlank()) {
            return true
        }
        return false
    }
}

class BratReceive(
    subfolder: String? = ""
) {
    var subfolder by property(subfolder)
    fun subfolderProperty() = getProperty(BratReceive::subfolder)

    fun hasNoNullProperties() : Boolean {
        if (!subfolder.isNullOrBlank()) {
            return true
        }
        return false
    }
}

class AverbisAnalysis(
    outputData: String = "",
    annotationValues: MutableList<String> = mutableListOf<String>().asObservable()
) {
    var outputData by property(outputData)
    fun outputDataProperty() = getProperty(AverbisAnalysis::outputData)

    var annotationValues by property(annotationValues)
    fun annotationValuesProperty() = getProperty(AverbisAnalysis::annotationValues)

    fun hasNoNullProperties() : Boolean {
        if (!outputData.isNullOrBlank()) {
            return true
        }
        return false
    }

    fun outputIsProperPath(): Boolean {
        if (!outputData.isNullOrBlank()) {
            return File(outputData).isDirectory
        }
        return false
    }
}

class BratTransfer(
    subfolder: String? = ""
) {
    var subfolder by property(subfolder)
    fun subfolderProperty() = getProperty(BratTransfer::subfolder)

    fun hasNoNullProperties() : Boolean {
        if (!subfolder.isNullOrBlank()) {
            return true
        }
        return false
    }
}

class AverbisSetupModel(setup: AverbisSetup) : ItemViewModel<AverbisSetup>(setup) {
    val url: Property<String> = bind(AverbisSetup::urlProperty)
    val apiToken: Property<String> = bind(AverbisSetup::apiTokenProperty)
    val projectName: Property<String> = bind(AverbisSetup::projectNameProperty)
    val pipelineName: Property<String> = bind(AverbisSetup::pipelineNameProperty)
    val language: Property<String> = bind(AverbisSetup::languageProperty)
}

class BratSetupModel(setup: BratSetup) : ItemViewModel<BratSetup>(setup) {
    val host: Property<String> = bind(BratSetup::hostProperty)
    val port: Property<String> = bind(BratSetup::portProperty)
    val username: Property<String> = bind(BratSetup::usernameProperty)
    val password: Property<String> = bind(BratSetup::passwordProperty)
    val dataFolder: Property<String> = bind(BratSetup::dataFolderProperty)
}

class AverbisInputModel(input: AverbisInput): ItemViewModel<AverbisInput>(input) {
    val input: Property<String> = bind(AverbisInput::inputDataProperty)
}

class BratReceiveModel(receive: BratReceive): ItemViewModel<BratReceive>(receive) {
    val subfolder: Property<String> = bind(BratReceive::subfolderProperty)
}

class AverbisAnalysisModel(analysis: AverbisAnalysis): ItemViewModel<AverbisAnalysis>(analysis) {
    val output: Property<String> = bind(AverbisAnalysis::outputDataProperty)
    val annotationValues: Property<MutableList<String>> = bind(AverbisAnalysis::annotationValuesProperty)
}

class BratTransferModel(transfer: BratTransfer): ItemViewModel<BratTransfer>(transfer) {
    val subfolder: Property<String> = bind(BratTransfer::subfolderProperty)
}
