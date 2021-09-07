package de.imise.integrator.model

import javafx.beans.property.Property
import tornadofx.*

class Setup(
    url: String? = null,
    apiToken: String? = null,
    projectName: String? = null,
    pipelineName: String? = null,
    language: String? = null
) {
    var url by property(url)
    fun urlProperty() = getProperty(Setup::url)

    var apiToken by property(apiToken)
    fun apiTokenProperty() = getProperty(Setup::apiToken)

    var projectName by property(projectName)
    fun projectNameProperty() = getProperty(Setup::projectName)

    var pipelineName by property(pipelineName)
    fun pipelineNameProperty() = getProperty(Setup::pipelineName)

    var language by property(language)
    fun languageProperty() = getProperty(Setup::language)

    fun hasAnyNullProperties() : Boolean{
        if (url != null) {
            if (apiToken != null) {
                if (projectName != null) {
                    if (pipelineName != null) {
                        if (language != null) {
                            return false
                        }
                    }
                }
            }
        }
        return true
    }
}

class Input {
}

class Analysis {
}

class SetupModel(setup: Setup) : ItemViewModel<Setup>(setup) {
    val url: Property<String> = bind(Setup::urlProperty)
    val apiToken: Property<String> = bind(Setup::apiTokenProperty)
    val projectName: Property<String> = bind(Setup::projectNameProperty)
    val pipelineName: Property<String> = bind(Setup::pipelineNameProperty)
    val language: Property<String> = bind(Setup::languageProperty)
}

class InputModel {
}

class AnalysisModel {
}