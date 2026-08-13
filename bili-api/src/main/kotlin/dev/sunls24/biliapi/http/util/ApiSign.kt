package dev.sunls24.biliapi.http.util

import dev.sunls24.biliapi.http.BiliHttpApi
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.utils.EmptyContent
import io.ktor.http.HttpMethod
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.encodedPath
import io.ktor.http.plus
import java.net.URLEncoder
import java.security.MessageDigest

private const val APP_KEY = "dfca71928277209b"
private const val APP_SEC = "b5475a8825547a4fc26c7d518eaaa02e"
private const val TV_APP_KEY = "4409e2ce8ffd12b8"
private const val TV_APP_SEC = "59b43e04ad6965f34319062b478f83dd"
private val mixinKeyEncTab = listOf(
    46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
    33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40, 61,
    26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36,
    20, 34, 44, 52
)

private fun String.md5(): String =
    MessageDigest.getInstance("MD5")
        .digest(toByteArray())
        .joinToString("") { "%02x".format(it) }

private fun Map<String, String>.toSortedQueryString(): String =
    toSortedMap()
        .map { (k, v) -> "$k=${URLEncoder.encode(v, "utf-8")}" }
        .joinToString("&")

private fun getMixinKey(orig: String): String =
    mixinKeyEncTab.fold("") { s, i -> s + orig[i] }.substring(0, 32)

private val HttpRequestBuilder.isAppRequest: Boolean
    get() = url.parameters.contains("access_key") || url.host == "app.bilibili.com"

private fun HttpRequestBuilder.encAppPost(appKey: String, appSec: String) {
    var parameters = (body as FormDataContent).formData
    parameters += Parameters.build { append("appkey", appKey) }

    val sortedQueryString = parameters.entries()
        .associate { it.key to it.value.first() }
        .toSortedQueryString()

    val sign = (sortedQueryString + appSec).md5()
    parameters += Parameters.build { append("sign", sign) }
    setBody(FormDataContent(parameters))
}

private fun HttpRequestBuilder.encTvAppPost() = encAppPost(TV_APP_KEY, TV_APP_SEC)

fun HttpRequestBuilder.encAppGet() {
    parameter("appkey", APP_KEY)

    val sortedQueryString = url.encodedParameters.entries()
        .associate { it.key to it.value.first() }
        .toSortedQueryString()

    val sign = (sortedQueryString + APP_SEC).md5()
    parameter("sign", sign)
}

suspend fun HttpRequestBuilder.encWbi() {
    if (BiliHttpApi.wbiImgKey == null || BiliHttpApi.wbiSubKey == null) {
        BiliHttpApi.updateWbi(cookieHeader = headers[HttpHeaders.Cookie])
    }
    val mixinKey = getMixinKey(
        requireNotNull(BiliHttpApi.wbiImgKey) { "wbiImgKey can't be null!" } +
                requireNotNull(BiliHttpApi.wbiSubKey) { "wbiSubKey can't be null!" }
    )

    val wts = (System.currentTimeMillis() / 1000).toInt()
    parameter("wts", wts)

    val sortedParams = url.encodedParameters.entries()
        .associate { it.key to it.value.first() }
        .toSortedMap()
        .map { (key, value) ->
            // 过滤特殊字符 !"!'()*
            val filteredValue = value.filter { c -> c !in setOf('!', '\'', '(', ')', '*') }
            "$key=$filteredValue"
        }
        .joinToString("&")

    val wRid = (sortedParams + mixinKey).md5()
    parameter("w_rid", wRid)
}

fun HttpClient.encApiSign() = plugin(HttpSend)
    .intercept { request ->
        // skip when using grpc proxy
        if (request.url.encodedPath.startsWith("bilibili.")) {
            return@intercept execute(request)
        }

        when (request.method) {
            HttpMethod.Get -> {
                val isWbiRequest = request.url.encodedPath.contains("wbi") ||
                        request.url.encodedPath.contains("/pgc/player/web/playurl") ||
                        request.url.encodedPath.contains("/pgc/player/web/v2/playurl")
                if (isWbiRequest) {
                    request.encWbi()
                } else if (request.isAppRequest) {
                    request.encAppGet()
                }
            }

            HttpMethod.Post -> {
                if (request.body is EmptyContent) return@intercept execute(request)
                val parameters = (request.body as FormDataContent).formData
                val isParametersContainKeywords = parameters.contains("access_key")
                val isTvAuthRequest = request.url.encodedPath.startsWith("/x/passport-tv-login/") ||
                        request.url.encodedPath.endsWith("oauth2/refresh_token")
                if (isTvAuthRequest) {
                    request.encTvAppPost()
                } else if (isParametersContainKeywords) {
                    request.encAppPost(APP_KEY, APP_SEC)
                }
            }
        }
        execute(request)
    }

fun HttpClient.injectBuvid3Cookie() = plugin(HttpSend).intercept { request ->
    val isPlayUrlRequest =
        request.url.encodedPath.contains("/x/player/playurl") ||
                request.url.encodedPath.contains("/x/player/wbi/playurl")

    if (!request.isAppRequest && !isPlayUrlRequest) {
        val buvid3 = BiliHttpApi.buvid3
        if (buvid3.isNotBlank()) {
            val existing = request.headers["Cookie"] ?: ""
            if (!existing.contains("buvid3=")) {
                request.headers["Cookie"] =
                    if (existing.isNotBlank()) "buvid3=$buvid3; $existing" else "buvid3=$buvid3"
            }
        }
    }
    execute(request)
}
