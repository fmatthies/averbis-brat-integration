package de.imise.integrator.controller

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
import java.nio.charset.Charset


class AverbisController(private val url: String? = null): Controller() {
    private val mainView: MainView by inject()
    private val client = OkHttpClient()

    fun postDocuments(documents: List<File>) {
        val response = postDocument(documents.first().absolutePath)
        mainView.outputField.text = response
    }

    fun postDocument(document_path: String,
                     encoding: Charset = Charsets.UTF_8
    ): String {
        val postBody = File(document_path).readText(encoding)
        val request = Request.Builder()
            .url(url?: buildFinalUrl())
            .post(postBody.toRequestBody(MEDIA_TYPE_TXT))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            return response.body?.string() ?: ""
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
        val MEDIA_TYPE_TXT = "text/plain; charset=utf-8".toMediaType()
        const val URL_VERSION = "v1"
        const val URL_ENDPOINT = "rest"
        const val URL_ANALYSIS_TYPE = "textanalysis"
        const val URL_PROJECTS = "projects"
        const val URL_PIPELINES = "pipelines"
        const val URL_ANALYSIS = "analyseText"
        const val URL_PARAM_LANG = "language"
    }
}

data class SchemeHost(val scheme: String, val host: String, val port: String?, val args: List<String>)