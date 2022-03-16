package de.imise.integrator.controller

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.beust.klaxon.json
import de.imise.integrator.extensions.ResponseType
import de.imise.integrator.extensions.ResponseTypeEntry
import de.imise.integrator.extensions.splitAfterBytes
import de.imise.integrator.model.AverbisAnalysis
import de.imise.integrator.view.MainView
import javafx.collections.ObservableList
import javafx.scene.control.RadioButton
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import tornadofx.Controller
import tornadofx.FXTask
import tornadofx.asObservable
import tornadofx.isInt
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.UnknownHostException
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.any
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.contains
import kotlin.collections.drop
import kotlin.collections.filter
import kotlin.collections.first
import kotlin.collections.forEach
import kotlin.collections.forEachIndexed
import kotlin.collections.isNotEmpty
import kotlin.collections.last
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.collections.toList

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

class AverbisResponse(val srcFileName: String, private val srcFilePath: String, private val index: Int? = null): ResponseType {
    private var jsonResponse: MutableMap<Int, JsonObject> = mutableMapOf()
    private val parser = Parser.default()
    val jsonEntryFilter: (Map.Entry<Int, JsonObject>) -> Boolean = { entry ->
        annotationValues.any { it == entry.value.string(JSON_TYPE_KEY_STRING) } ||
        annotationValues.isEmpty()
    }
    var documentText: String = ""
    var documentTextId: Int = -1
    var documentLanguage: String = ""
    var documentAverbisVersion: String = ""
    var annotationValues: MutableList<String> = mutableListOf()
    var errorMessage: String? = null
    override val basename: String
        get() {
            if (index != null) {
                return srcFileName.substringBeforeLast(".") + "_part-${index.toString().padStart(2, '0')}"
            }
            return srcFileName.substringBeforeLast(".")
        }
    override val additionalColumn = this.srcFilePath
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

    fun setAnnotations(values: Map<String, List<String>>) {
       values.forEach{ (_, v) ->
           annotationValues.addAll(v)
       }
    }

    fun readJson(jsonString: String) {
        readJson(jsonString.byteInputStream()).forEach {
            if (it[JSON_ID_STRING] != null) {
                val id = it[JSON_ID_STRING] as Int
                jsonResponse[id] = it
                if (it.string(JSON_TYPE_KEY_STRING).toString() == DOCUMENT_TEXT_TYPE) {
                    documentText = it.string(JSON_COVERED_TEXT_KEY_STRING).toString()
                    documentTextId = id
                    documentLanguage = it.string("language").toString()
                    documentAverbisVersion = it.string("version").toString()
                }
            }
        }
    }

    fun jsonToBrat(bratAnnotationValues: List<String>): String {
        val sb = StringBuilder()
        jsonResponse
            .filter { bratAnnotationValues.contains(it.value.string(JSON_TYPE_KEY_STRING)) && jsonEntryFilter(it) }
            .forEach { sb.append(AverbisJsonEntry(it.value, this).asTextboundAnnotation()).append("\n") }
        return sb.toString().removeSuffix("\n")
    }

    fun filteredJson() : String {
        val filteredObj = jsonResponse
            .filter { jsonEntryFilter(it) || it.value.string(JSON_TYPE_KEY_STRING) == DOCUMENT_TEXT_TYPE}
            .values
            .toList()
        return json {
            obj(ANNOTATION_ARRAY_KEY_STRING to array( filteredObj ))
        }.toJsonString(prettyPrint = true)
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
        const val DOCUMENT_TEXT_TYPE: String = "de.averbis.types.health.DocumentAnnotation"
        const val JSON_COVERED_TEXT_KEY_STRING = "coveredText"
        const val JSON_TYPE_KEY_STRING = "type"
        const val ANNOTATION_ARRAY_KEY_STRING = "annotationDtos"
    }
}

class AverbisController(private val url: String? = null): Controller() {
    private val logging: LoggingController by inject()
    private val mainView: MainView by inject()
    private val fileHandlingController: FileHandlingController by inject()
    private val client = OkHttpClient().newBuilder()
        .connectTimeout(app.config.getProperty(CONNECT_TIMEOUT).toLong(), TimeUnit.SECONDS)
        .readTimeout(app.config.getProperty(READ_TIMEOUT).toLong(), TimeUnit.SECONDS)
        .writeTimeout(app.config.getProperty(WRITE_TIMEOUT).toLong(), TimeUnit.SECONDS)
        .build()

    fun postDocuments(
        documents: List<File>,
        averbisResponseList: ObservableList<AverbisResponse>,
        analysis: AverbisAnalysis,
        bratAnnotationValues: List<String>,
        fxTask: FXTask<*>,
        encoding: Charset = Charsets.UTF_8
    ) {
        val maxDocs = documents.size
        var splitFiles = false
        documents.forEachIndexed { docIndex, file ->
            file.readText(encoding).run {
                val replacedTabs = this.replace("\t", "    ")
                val normalizedLinebreaks = replacedTabs.replace(Regex("\\r?\\n|\\r"), "\n")
                val replacedBom = normalizedLinebreaks.replace(UTF8_BOM, "")
                val maxByteSize: String? = app.config.getProperty(MAX_BYTE_SIZE)
                if (maxByteSize != null && maxByteSize.isInt() && maxByteSize.toInt() > 0) {
                    splitFiles = true
                    replacedBom.splitAfterBytes(app.config.getProperty(MAX_BYTE_SIZE).toInt(), encoding)
                } else { listOf( replacedBom ).asSequence() }
            }.forEachIndexed { contentIndex, content ->
                val cIndex = if (splitFiles) {contentIndex} else {null}
                averbisResponseList.add(postDocument(content, file, cIndex).apply {
                    setAnnotations(analysis.annotationValues)
                    if (analysis.outputIsProperPath() && mainView.outputMode.value == "Local") {
                        fileHandlingController.writeOutputToDisk(
                            listOf(this),
                            analysis.outputData!!,
                            bratAnnotationValues
                        )
                    }
                })
            }
            fxTask.updateProgress((docIndex + 1).toDouble(), maxDocs.toDouble())
        }
    }

    private fun postDocument(documentContent: String,
                             file: File,
                             index: Int?
    ): AverbisResponse {
        val responseObj = AverbisResponse(file.nameWithoutExtension, file.parent, index)
//        val postBody = File(document_path).readText(encoding).run {
//            val replacedTabs = this.replace("\t", "    ")
//            val normalizedLinebreaks = replacedTabs.replace(Regex("\\r?\\n|\\r"), "\n")
//            val replacedBom = normalizedLinebreaks.replace(UTF8_BOM, "")
//            replacedBom//.splitAfterBytes(20000, encoding)
//        }

        val request = Request.Builder()
            .url(url?: buildFinalUrl())
            .addHeader(API_HEADER_STRING, mainView.apiTokenField.text)
            .addHeader(ACCEPT_HEADER_STRING, ACCEPT_HEADER_VAL)
            .post(documentContent.toRequestBody(MEDIA_TYPE_TXT))
            .build()

        logging.logAverbis(request.toString())
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                val responseBodyString = response.body?.string() ?: ""
                responseObj.readJson(responseBodyString)
            }
        } catch (e: UnknownHostException) {
            responseObj.errorMessage = "UnknownHostException: ${e.message?: "without explicit message"}"
        } catch (e: IOException) {
            responseObj.errorMessage = e.message?: "IOException without explicit message"
        } finally {
            responseObj.errorMessage?.let {
                logging.logAverbis(it)
            }
            return responseObj
        }
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
        const val UTF8_BOM = "\uFEFF"
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
        const val CONNECT_TIMEOUT = "default_connect_timeout"
        const val READ_TIMEOUT = "default_read_timeout"
        const val WRITE_TIMEOUT = "default_write_timeout"
        const val MAX_BYTE_SIZE = "max_byte_size"
    }
}

data class SchemeHost(val scheme: String, val host: String, val port: String?, val args: List<String>)