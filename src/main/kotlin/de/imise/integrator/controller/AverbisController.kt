package de.imise.integrator.controller

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.beust.klaxon.json
import de.imise.integrator.extensions.ResponseType
import de.imise.integrator.extensions.ResponseTypeEntry
import de.imise.integrator.model.Analysis
import de.imise.integrator.view.MainView
import javafx.collections.ObservableList
import javafx.scene.control.RadioButton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl
import tornadofx.*
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import javax.json.JsonArrayBuilder
import javax.json.JsonBuilderFactory

const val JSON_ID_STRING = "id"

class AverbisJsonEntry(private val jsonObject: JsonObject, averbisResponse: AverbisResponse): ResponseTypeEntry {
    val begin: Int
        get() = jsonObject["begin"] as Int
    val end: Int
        get() = jsonObject["end"] as Int
    val id: Int
        get() = jsonObject["id"] as Int
    val coveredText: String
        get() = removeNewlines(jsonObject["coveredText"] as String)
    val typeFQN: String
        get() = jsonObject["type"] as String
    override val type: String
        get() = typeFQN.substringAfterLast(".")
    override val text: String
        get() = coveredText

    init {
        if (jsonObject["coveredText"] != coveredText) {
            averbisResponse.documentText = StringBuilder(averbisResponse.documentText).also {
                it.setRange(begin, end, coveredText) }.toString()
        }
    }

    private fun removeNewlines(s: String): String {
        if (s.contains("\n")) {
            if (s.contains("\r")) {
                return s.replace("\r\n", "  ")
            }
            return s.replace("\n", " ")
        }
        else if (s.contains("\r")) {
            return s.replace("\r", " ")
        }
        return s
    }

    fun asTextboundAnnotation(): String {
        return "T${id}\t$type $begin $end\t$coveredText"
    }
}

class AverbisResponse(file: File): ResponseType {
    private var jsonResponse: MutableMap<Int, JsonObject> = mutableMapOf()
    private val parser = Parser.default()
    val jsonEntryFilter: (Map.Entry<Int, JsonObject>) -> Boolean = { entry ->
        annotationValues.any { it == entry.value.string(JSON_TYPE_KEY_STRING)} || annotationValues.isEmpty()
    }
    val inputFileName: String = file.nameWithoutExtension
    val inputFilePath: String = file.parent
    var documentText: String = ""
    var documentTextId: Int = -1
    var annotationValues: List<String> = listOf()
    override val basename: String
        get() = inputFileName
    override val pathname: String
        get() = inputFilePath
    override val items: ObservableList<ResponseTypeEntry>
        get() {
            return jsonResponse
                .filter { jsonEntryFilter(it) }
                .map { AverbisJsonEntry(it.value, this) }
                .asObservable()
        }

    fun getData(): MutableMap<Int, JsonObject> {
        return jsonResponse
    }

    fun setAnnotations(values: List<String>) {
        annotationValues = values
    }

    fun readJson(jsonString: String) {
        readJson(jsonString.byteInputStream()).forEach {
            if (it[JSON_ID_STRING] != null) {
                val id = it[JSON_ID_STRING] as Int
                jsonResponse[id] = it
                if (it.string(JSON_TYPE_KEY_STRING).toString() == DOCUMENT_TEXT_TYPE) {
                    documentText = it.string(JSON_COVERED_TEXT_KEY_STRING).toString()
                    documentTextId = id
                }
            }
        }
    }

    fun jsonToBrat() : String {
        val sb = StringBuilder()
        jsonResponse
            .filter { jsonEntryFilter(it) }
            .forEach { sb.append(AverbisJsonEntry(it.value, this).asTextboundAnnotation()).append("\n") }
        return sb.toString().removeSuffix("\n")
    }

    fun filteredJson() : String {
        return json {
            ANNOTATION_ARRAY_KEY_STRING to
                array(jsonResponse
                    .filter { jsonEntryFilter(it) }
                    .values
                    .toList()
//                    .apply {
//                        this.plus( json {  } )
//                    } // ToDo: add DocumentAnnotation & DeidentifiedDocument entries
                )
        }.toString()
    }

    private fun readJson(jsonStream: InputStream): JsonArray<JsonObject> {
        val json: JsonObject = parser.parse(jsonStream) as JsonObject
        val jsonArray = JsonArray<JsonObject>()
        json.keys.forEach {
            json.array<JsonObject>(it)?.let { array -> jsonArray.addAll(array) }
        }
        if (jsonArray.isNotEmpty()) {
            return jsonArray
        }
        return JsonArray(listOf())
    }

    companion object {
        const val DEIDED_DOCUMENT_TEXT_TYPE: String = "de.averbis.types.health.DeidentifiedDocument"
        const val DOCUMENT_TEXT_TYPE: String = "de.averbis.types.health.DocumentAnnotation"
        const val JSON_COVERED_TEXT_KEY_STRING = "coveredText"
        const val JSON_DEIDED_TEXT_KEY_STRING: String = "deidentifiedText"
        const val JSON_TYPE_KEY_STRING = "type"
        const val ANNOTATION_ARRAY_KEY_STRING = "annotationDtos"
    }
}

class AverbisController(private val url: String? = null): Controller() {
    private val logging: LoggingController by inject()
    private val mainView: MainView by inject()
    private val fileHandlingController: FileHandlingController by inject()
    private val client = OkHttpClient().newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    fun postDocuments(documents: List<File>, averbisResponseList: ObservableList<ResponseType>, analysis: Analysis) {
        documents.forEach {
            averbisResponseList.add(postDocument(it.absolutePath).apply {
                setAnnotations(mainView.analysisModel.annotationValues.value)
                if (analysis.outputIsProperPath() && mainView.outputMode.value == "Local") {
                    fileHandlingController.writeOutputToDisk(listOf(this), analysis.outputData!!) //ToDo: to conform to old method I need to transform this into a list... change it?
                }
            } )
        }
    }

    private fun postDocument(document_path: String,
                             encoding: Charset = Charsets.UTF_8
    ): AverbisResponse {
        val responseObj = AverbisResponse(File(document_path))
        val postBody = File(document_path).readText(encoding)
        val request = Request.Builder()
            .url(url?: buildFinalUrl())
            .addHeader(API_HEADER_STRING, mainView.apiTokenField.text)
            .addHeader(ACCEPT_HEADER_STRING, ACCEPT_HEADER_VAL)
            .post(postBody.toRequestBody(MEDIA_TYPE_TXT))
            .build()

        logging.logAverbis(request.toString())
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val responseBodyString = response.body?.string() ?: ""
            responseObj.readJson(responseBodyString)
            return responseObj
        } //ToDo: catch no connection
    }

    fun buildFinalUrl(): String {
        val schemeHost = buildUrlBasePath()
        val finalUrl = HttpUrl.Builder()
            .scheme(schemeHost.scheme)
            .host(schemeHost.host)
        schemeHost.args.forEach { finalUrl.addPathSegment(it) }
        if (schemeHost.port != null) { finalUrl.port(schemeHost.port.toInt()) }
        finalUrl
            .addPathSegments("$URL_ENDPOINT/$URL_VERSION/$URL_ANALYSIS_TYPE")
            .addPathSegments("$URL_PROJECTS/${mainView.projectNameField.text}")
            .addPathSegments("$URL_PIPELINES/${mainView.pipelineNameField.text}")
            .addPathSegment(URL_ANALYSIS)
            .addQueryParameter(URL_PARAM_LANG, (mainView.languageGroup.selectedToggle as RadioButton).text)
        return finalUrl.build().toString()
    }

    private fun buildUrlBasePath(): SchemeHost {
        fun extractHost(scheme: String, url: String): SchemeHost {
            val buildHost: String
            val buildArgs: List<String>
            var buildPort: String? = null
            val rest = url.split("/")

            if (rest.first().contains(":")) {
                buildHost = rest.first().split(":").first()
                buildPort = rest.first().split(":").last()
            } else {
                buildHost = rest.first()
            }
            buildArgs = rest.drop(1)

            return SchemeHost(scheme, buildHost, buildPort, buildArgs)
        }

        val urlField = mainView.urlField.text
        return if (urlField.startsWith("http")) {
            extractHost(urlField.substringBefore(":"), urlField.substringAfter("://"))
        } else {
            extractHost("http", urlField)
        }
    }

    companion object {
        val MEDIA_TYPE_TXT = "text/plain; charset=utf-8".toMediaType()
        const val URL_VERSION = "v1"
        const val URL_ENDPOINT = "rest"
        const val URL_ANALYSIS_TYPE = "textanalysis"
        const val URL_PROJECTS = "projects"
        const val URL_PIPELINES = "pipelines"
        const val URL_ANALYSIS = "analyseText"
        const val URL_PARAM_LANG = "language"
        const val API_HEADER_STRING = "api-token"
        const val ACCEPT_HEADER_STRING = "accept"
        const val ACCEPT_HEADER_VAL = "application/json"
    }
}

data class SchemeHost(val scheme: String, val host: String, val port: String?, val args: List<String>)