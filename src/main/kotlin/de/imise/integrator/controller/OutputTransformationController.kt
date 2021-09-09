package de.imise.integrator.controller

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import tornadofx.*
import java.io.InputStream

const val JSON_TYPE_STRING = "type"
const val JSON_DATE_TYPE_STRING = "de.averbis.types.health.Date"
const val JSON_TEXT_TYPE_STRING = "coveredText"

class OutputTransformationController(
    val parser: Parser,
    val annotationBaseString: String,
    val annotationKey: String,
    val annotationValues: List<String>?
    ): Controller() {

    fun readJson(jsonString: String): JsonArray<JsonObject> {
        return readJson(jsonString.byteInputStream())
    }

    fun readJson(jsonStream: InputStream): JsonArray<JsonObject> {
        val json: JsonObject = parser.parse(jsonStream) as JsonObject
        val jsonArray = json.array<JsonObject>(annotationBaseString)

        if (jsonArray != null) {
            return jsonArray
        }
        return JsonArray(listOf())
    }

    fun queryArrayBy(
        jsonArray: JsonArray<JsonObject>,
        annotationKey: String,
        annotationValues: List<String>,
    ) : List<JsonObject>{
        return jsonArray.filter { jsonObj ->
            annotationValues.any { it == jsonObj.string(annotationKey) }
        }
    }

    fun getResults(jsonResponseString: String) : String {
        val sb = StringBuilder()
        readJson(jsonResponseString).run {
            if (annotationValues != null) {
                queryArrayBy(this, annotationKey, annotationValues)
            } else {
                this.toList()
            }
        }.forEach { sb.append(it.string(JSON_TEXT_TYPE_STRING)).append("\n") }
        return sb.toString().removeSuffix("\n")
    }

    open class Builder {
        internal var parser: Parser = Parser.default()
        internal var annotationBaseString: String = "annotationDtos"
        internal var annotationKey: String = "type"
        internal var annotationValues: List<String> = listOf("de.averbis.types.health.DocumentAnnotation")

        open fun annotationKey(key: String): Builder = apply {
            this.annotationKey = key
        }

        open fun annotationValues(values: List<String>) = apply {
            this.annotationValues = values
        }

        open fun build(): OutputTransformationController {
            return OutputTransformationController(
                parser,
                annotationBaseString,
                annotationKey,
                annotationValues
            )
        }
    }
}