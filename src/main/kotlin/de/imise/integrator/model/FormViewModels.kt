package de.imise.integrator.model

import javafx.beans.property.Property
import javafx.collections.ObservableList
import tornadofx.*

class Setup(
    url: String = "",
    apiToken: String? = "",
    projectName: String? = "",
    pipelineName: String? = "",
    language: String? = ""
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

class Input(
    inputData: String? = ""
) {
    var inputData by property(inputData)
    fun inputDataProperty() = getProperty(Input::inputData)

    fun hasNoNullProperties() : Boolean {
        if (!inputData.isNullOrBlank()) {
            return true
        }
        return false
    }
}

class Analysis(
    outputData: String? = "",
    annotationValues: MutableList<String> = mutableListOf<String>().asObservable()
) {
    var outputData by property(outputData)
    fun outputDataProperty() = getProperty(Analysis::outputData)

    var annotationValues by property(annotationValues)
    fun annotationValuesProperty() = getProperty(Analysis::annotationValues)

    fun hasNoNullProperties() : Boolean {
        if (!outputData.isNullOrBlank()) {
            return true
        }
        return false
    }
}

class SetupModel(setup: Setup) : ItemViewModel<Setup>(setup) {
    val url: Property<String> = bind(Setup::urlProperty)
    val apiToken: Property<String> = bind(Setup::apiTokenProperty)
    val projectName: Property<String> = bind(Setup::projectNameProperty)
    val pipelineName: Property<String> = bind(Setup::pipelineNameProperty)
    val language: Property<String> = bind(Setup::languageProperty)
}

class InputModel(input: Input): ItemViewModel<Input>(input) {
    val input: Property<String> = bind(Input::inputDataProperty)
}

class AnalysisModel(analysis: Analysis): ItemViewModel<Analysis>(analysis) {
    val output: Property<String> = bind(Analysis::outputDataProperty)
    val annotationValues: Property<MutableList<String>> = bind(Analysis::annotationValuesProperty)
}