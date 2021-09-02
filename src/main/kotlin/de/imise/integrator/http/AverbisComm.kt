package de.imise.integrator.http

import com.palominolabs.http.url.UrlBuilder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.nio.charset.Charset

class AverbisComm(private val hd_url: String, private val scheme: String? = null) {
    private val client = OkHttpClient()
    private val url_version = "v1"
    private val url_endpoint = "rest"
    private val url_analysis_type = "textanalysis"
    private val url_projects = "projects"
    private val url_pipelines = "pipelines"
    private val url_analysis = "analyseText"
    private val url_param_lang = "language"
    private var finalUrl: String


    fun postDocument(document_path: String,
                     encoding: Charset = Charsets.UTF_8
    ): String {
        val postBody = File(document_path).readText(encoding)

        val request = Request.Builder()
            .url(finalUrl)
            .post(postBody.toRequestBody(MEDIA_TYPE_TXT))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            //ToDo: add stuff to do with json response
            return response.body?.string() ?: ""
        }
    }

    private fun buildFinalUrl(scheme: String, host: String): String {
        return UrlBuilder.forHost(scheme, host)
            .pathSegments(url_endpoint, url_version, url_analysis_type)
            .pathSegments(url_projects, "test-project")
            .pathSegments(url_pipelines, "deid")
            .pathSegment(url_analysis)
            .queryParam(url_param_lang, "de")
            .toUrlString()
    }

    init {
        var buildScheme = "http"
        val buildHost: String
        if (hd_url.startsWith("http")) {
            scheme?:let {
                buildScheme = hd_url.substringBefore(":")
            }
            buildHost = hd_url.substringAfter("://")
        } else {
            buildScheme = scheme ?: "http"
            buildHost = hd_url
        }
        finalUrl = buildFinalUrl(buildScheme, buildHost)
    }

    companion object {
        val MEDIA_TYPE_TXT = "text/plain; charset=utf-8".toMediaType()
    }
}