package de.imise.integrator.controller

import com.palominolabs.http.url.UrlBuilder
import de.imise.integrator.view.MainView
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import tornadofx.*
import java.io.File
import java.io.IOException
import java.nio.charset.Charset


class AverbisController: Controller() {
    private val mainView: MainView by inject()
    private val client = OkHttpClient()

    fun postDocument(document_path: String,
                     encoding: Charset = Charsets.UTF_8
    ): String {
        val postBody = File(document_path).readText(encoding)
        val request = Request.Builder()
            .url(buildFinalUrl())
            .post(postBody.toRequestBody(MEDIA_TYPE_TXT))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            return response.body?.string() ?: ""
        }
    }

    fun buildFinalUrl(): String {
        val schemeHost = buildUrlBasePath()
        val finalUrl = UrlBuilder.forHost(schemeHost.scheme, schemeHost.host)
            .pathSegments(URL_ENDPOINT, URL_VERSION, URL_ANALYSIS_TYPE)
            .pathSegments(URL_PROJECTS, mainView.projectNameField.text)
            .pathSegments(URL_PIPELINES, mainView.pipelineNameField.text)
            .pathSegment(URL_ANALYSIS)
            .queryParam(URL_PARAM_LANG, "de")
            .toUrlString()
        return finalUrl
    }

    private fun buildUrlBasePath(): SchemeHost {
        //ToDo: port (:8445) is a problem for the UrlBuilder...
        val buildScheme: String
        val buildHost: String
        val urlField = mainView.urlField.text

        if (urlField.startsWith("http")) {
            buildScheme = urlField.substringBefore(":")
            buildHost = urlField.substringAfter("://")
        } else {
            buildScheme = "http"
            buildHost = urlField
        }
        return SchemeHost(buildScheme, buildHost)
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

data class SchemeHost(val scheme: String, val host: String)