package de.imise.integrator.controller

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import de.imise.integrator.view.MainView
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
import kotlin.reflect.jvm.isAccessible


interface AverbisJsonEntry {
    val begin: Int
    val end: Int
    val id: Int
    val coveredText: String
    val type: String

    fun asTextboundAnnotation(): String {
        return "T${id}\t" +
               "${type.substringAfterLast('.')} " +
               "$begin $end\t" +
               coveredText
    }
}

class AverbisPHIEntry(private val jsonObject: JsonObject) : AverbisJsonEntry {
    override val begin: Int
        get() = jsonObject["begin"] as Int
    override val end: Int
        get() = jsonObject["end"] as Int
    override val id: Int
        get() = jsonObject["id"] as Int
    override val coveredText: String
        get() = jsonObject["coveredText"] as String
    override val type: String
        get() = jsonObject["type"] as String
}

class AverbisResponse(file: File) {

    var jsonResponse: JsonArray<JsonObject>? = null
    val parser = Parser.default()
    val annotationBaseString: String = "annotationDtos"
    var outputTransform: OutputTransformationController =  OutputTransformationController.Builder().build()
    val inputFileName: String = file.name
    val inputFilePath: String = file.parent
    var documentText: String = ""

    fun setAnnotationValues(values: List<String>) {
        outputTransform = OutputTransformationController.Builder()
            .annotationValues(values)
            .build()
    }

    fun transformToType(type: TransformationTypes): String {
        return when (type) {
            TransformationTypes.STRING -> outputTransform.jsonToString(jsonResponse)
            TransformationTypes.BRAT -> outputTransform.jsonToBrat(jsonResponse)
        }
    }

    fun readJson(jsonString: String) {
        jsonResponse = readJson(jsonString.byteInputStream())
    }

    private fun readJson(jsonStream: InputStream): JsonArray<JsonObject> {
        val json: JsonObject = parser.parse(jsonStream) as JsonObject
        val jsonArray = json.array<JsonObject>(annotationBaseString)

        if (jsonArray != null) {
            return jsonArray
        }
        return JsonArray(listOf())
    }
}

class AverbisController(private val url: String? = null): Controller() {
    private val mainView: MainView by inject()
    private val client = OkHttpClient().newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    fun postDocuments(documents: List<File>): List<AverbisResponse> {
        val responseList = mutableListOf<AverbisResponse>()
        documents.forEach {
            responseList.add(postDocument(it.absolutePath).apply {
                setAnnotationValues(mainView.analysisModel.annotationValues.value) })
        }
        return responseList.toList()
    }

    private fun postDocument(document_path: String,
                             encoding: Charset = Charsets.UTF_8
    ): AverbisResponse {
        val responseObj = AverbisResponse(File(document_path))
        val postBody = File(document_path).readText(encoding)
        val request = Request.Builder()
            .url(url?: buildFinalUrl())
            .addHeader(API_HEADER_STRING, if (this::mainView.isAccessible) { mainView.apiTokenField.text } else { "" })
            .addHeader(ACCEPT_HEADER_STRING, ACCEPT_HEADER_VAL)
            .post(postBody.toRequestBody(MEDIA_TYPE_TXT))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            responseObj.readJson(response.body?.string() ?: "")
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