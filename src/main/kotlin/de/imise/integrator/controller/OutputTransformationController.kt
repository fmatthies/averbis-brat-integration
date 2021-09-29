package de.imise.integrator.controller

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import de.imise.integrator.view.MainView
import tornadofx.*
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File

//const val JSON_TYPE_STRING = "type"
//const val JSON_COVERED_TEXT_KEY_STRING = "coveredText"
//const val JSON_DEIDED_TEXT_KEY_STRING = "deidentifiedText"
//const val JSON_DOCUMENT_ANNOTATION_STRING = "de.averbis.types.health.DocumentAnnotation"
//const val JSON_DEID_DOCUMENT_ANNOTATION_STRING = "de.averbis.types.health.DeidentifiedDocument"

//enum class TransformationTypes {
////    BRAT, STRING, JSON
//    BRAT
//}

data class OutputFileStream(val fileName: String, val extension: String, val content: String)

class OutputTransformationController(
    val annotationKey: String,
    val annotationValues: List<String>?
    ) {

//    val ext = mapOf(
//        TransformationTypes.BRAT.name to "ann",
////            TransformationTypes.STRING.name to "txt",
////            TransformationTypes.JSON.name to "json"
//    )

    companion object {
        fun transformToBrat(response: List<AverbisResponse>): List<Pair<OutputFileStream, OutputFileStream>> {
            return response.map {
                Pair(
                    OutputFileStream(
                        fileName = it.inputFileName, extension = "ann",
                        content = it.jsonToBrat().replace("\\r\\n?", "\n")
                    ),
                    OutputFileStream(
                        fileName = it.inputFileName, extension = "txt",
                        content = it.documentText.replace("\\r\\n?", "\n")
                    )
                )
            }
        }

        fun getFilteredJson(response: List<AverbisResponse>): List<OutputFileStream> {
            return response.map {
                OutputFileStream(
                    fileName = it.inputFileName, extension = "json",
                    content = it.filteredJson().replace("\\r\\n?", "\n")
                )
            }
        }
    }
//    fun jsonToString(jsonResponse: JsonArray<JsonObject>?) : String {
//        val sb = StringBuilder()
//
//        if ((annotationValues != null) and (jsonResponse != null)) {
//            queryArrayBy(jsonResponse!!, annotationKey, annotationValues!!)
//        } else {
//            listOf<JsonObject>()
//        }.forEach { sb.append(it.string(JSON_COVERED_TEXT_KEY_STRING)).append("\n") }
//
//        return sb.toString().removeSuffix("\n")
//    }

//    fun jsonToBrat(jsonResponse: MutableMap<String,JsonObject>?) : String {
//        val sb = StringBuilder()
////        responseRef.documentText =
////            queryArrayBy(jsonResponse!!, annotationKey, listOf(JSON_DOCUMENT_ANNOTATION_STRING))
////            .first().string(JSON_COVERED_TEXT_KEY_STRING)!!
////        responseRef.deidedDocumentText =
////            queryArrayBy(jsonResponse!!, annotationKey, listOf(JSON_DEID_DOCUMENT_ANNOTATION_STRING))
////            .first().string(JSON_DEIDED_TEXT_KEY_STRING)!!
//
//        if ((annotationValues != null) and (jsonResponse != null)) {
//            queryArrayBy(jsonResponse!!, annotationKey, annotationValues!!)
//        } else {
//            listOf<JsonObject>()
//        }.forEach { sb.append(AverbisPHIEntry(it, responseRef).asTextboundAnnotation()).append("\n") }
//
//        return sb.toString().removeSuffix("\n")
//    }

//    fun keepJson(jsonResponse: JsonArray<JsonObject>?): String {
//        val sb = StringBuilder().append("{").append("annotations: [\n")
//
//        if ((annotationValues != null) and (jsonResponse != null)) {
//            queryArrayBy(jsonResponse!!, annotationKey, annotationValues!!)
//        } else {
//            listOf<JsonObject>()
//        }.forEach { sb.append(it.toJsonString(prettyPrint = true, canonical = true)).append(",\n") }
//
//        return sb.toString().removeSuffix(",\n") + "\n]}"
//    }

//    open class Builder {
////        internal var annotationKey: String = JSON_TYPE_STRING
////        internal var annotationValues: List<String> = listOf(JSON_DOCUMENT_ANNOTATION_STRING)
//
//        open fun annotationKey(key: String): Builder = apply {
//            this.annotationKey = key
//        }
//
//        open fun annotationValues(values: List<String>) = apply {
//            this.annotationValues = values
//        }
//
//        open fun build(): OutputTransformationController {
//            return OutputTransformationController(
//                annotationKey,
//                annotationValues
//            )
//        }
//    }
}