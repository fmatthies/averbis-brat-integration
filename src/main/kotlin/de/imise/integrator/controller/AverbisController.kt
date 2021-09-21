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

//ToDo: I need to make sure that "DocumentAnnotation" and "DeidentifiedDocument" are updated according
// to the replacements by `removeNewlines`
class AverbisPHIEntry(private val jsonObject: JsonObject, private val responseRef: AverbisResponse) : AverbisJsonEntry {
    override val begin: Int
        get() = jsonObject["begin"] as Int
    override val end: Int
        get() = jsonObject["end"] as Int
    override val id: Int
        get() = jsonObject["id"] as Int
    override val coveredText: String
        get() = removeNewlines(jsonObject["coveredText"] as String, responseRef)
    override val type: String
        get() = jsonObject["type"] as String

    private fun removeNewlines(s: String, responseRef: AverbisResponse): String {
        if (s.contains("\n")) {
            if (s.contains("\r")) {
                alignDocumentText(begin, end, 2, responseRef)
                return s.replace("\r\n", "  ")
            }
            alignDocumentText(begin, end, 1, responseRef)
            return s.replace("\n", " ")
        }
        else if (s.contains("\r")) {
            alignDocumentText(begin, end, 1, responseRef)
            return s.replace("\r", " ")
        }
        return s
    }

    //ToDo: that is so stupid (and doesnt work) -- I need a watching class that replaces all in one fell swoop after
    // all annotations were replaced
    private fun alignDocumentText(begin: Int, end: Int, span: Int, responseRef: AverbisResponse) {
        responseRef.documentText = StringBuilder(responseRef.documentText).also {
            it.setRange(begin, end, responseRef.documentText.substring(IntRange(begin, end - span)).replace("\\n", " ".repeat(span)))
        }.toString()
//        responseRef.documentText = responseRef.documentText
//            .replaceRange(begin, end + 1, responseRef.documentText.substring(IntRange(begin, end - span)))
//        responseRef.deidedDocumentText = responseRef.deidedDocumentText
//            .replaceRange(begin, end + 1, responseRef.deidedDocumentText.substring(IntRange(begin, end - span)))
    }
}

class AverbisResponse(file: File) {

    var jsonResponse: JsonArray<JsonObject>? = null
    val parser = Parser.default()
    var outputTransform: OutputTransformationController =  OutputTransformationController.Builder().build()
    val inputFileName: String = file.nameWithoutExtension
    val inputFilePath: String = file.parent
    var documentText: String = ""
    var deidedDocumentText: String = ""

    fun setAnnotationValues(values: List<String>) {
        outputTransform = OutputTransformationController.Builder()
            .annotationValues(values)
            .build()
    }

    fun transformToType(type: TransformationTypes): String {
        return when (type) {
//            TransformationTypes.STRING -> outputTransform.jsonToString(jsonResponse)
            TransformationTypes.BRAT -> outputTransform.jsonToBrat(jsonResponse, this)
//            TransformationTypes.JSON -> outputTransform.keepJson(jsonResponse)
        }
    }

    fun readJson(jsonString: String) {
        jsonResponse = readJson(jsonString.byteInputStream())
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

//    companion object {
//        const val DEIDED_DOCUMENT_TEXT_TYPE: String = "de.averbis.types.health.DeidentifiedDocument"
//        const val DOCUMENT_TEXT_TYPE: String = "de.averbis.types.health.DocumentAnnotation"
//    }
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
            .addHeader(API_HEADER_STRING, mainView.apiTokenField.text)
            .addHeader(ACCEPT_HEADER_STRING, ACCEPT_HEADER_VAL)
            .post(postBody.toRequestBody(MEDIA_TYPE_TXT))
            .build()

        LoggingController().log(request.toString())
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