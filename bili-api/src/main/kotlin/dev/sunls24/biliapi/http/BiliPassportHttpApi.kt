package dev.sunls24.biliapi.http

import dev.sunls24.biliapi.http.entity.BiliResponse
import dev.sunls24.biliapi.http.entity.login.qr.AppQRDataRequest
import dev.sunls24.biliapi.http.entity.login.qr.AppQRLoginData
import dev.sunls24.biliapi.http.entity.login.qr.AppTokenRefreshData
import dev.sunls24.biliapi.http.plugins.BiliUserAgent
import dev.sunls24.biliapi.http.util.encApiSign
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object BiliPassportHttpApi {
    private lateinit var client: HttpClient

    init {
        createClient()
    }

    private fun createClient() {
        client = HttpClient(OkHttp) {
            BiliUserAgent()
            install(ContentNegotiation) {
                json(Json {
                    coerceInputValues = true
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }
            install(ContentEncoding) {
                deflate(1.0F)
                gzip(0.9F)
            }
            defaultRequest {
                url {
                    host = "passport.bilibili.com"
                    protocol = URLProtocol.HTTPS
                }
            }
        }.apply {
            encApiSign()
        }
    }

    /**
     * 申请二维码（App）
     */
    suspend fun getAppQRUrl(): BiliResponse<AppQRDataRequest> =
        client.post("/x/passport-tv-login/qrcode/auth_code") {
            setBody(FormDataContent(
                Parameters.build {
                    append("local_id", "0")
                    append("ts", "0")
                }
            ))
        }.body()


    /**
     * 使用[authCode]进行二维码登录
     */
    suspend fun loginWithAppQR(authCode: String): BiliResponse<AppQRLoginData> =
        client.post("/x/passport-tv-login/qrcode/poll") {
            setBody(FormDataContent(
                Parameters.build {
                    append("auth_code", authCode)
                    append("local_id", "0")
                    append("ts", "0")
                }
            ))
        }.body()

    suspend fun refreshAccessToken(
        accessToken: String,
        refreshToken: String,
        version: Int = 3
    ): BiliResponse<AppTokenRefreshData> =
        client.post("/api/v$version/oauth2/refresh_token") {
            setBody(FormDataContent(
                Parameters.build {
                    append("access_token", accessToken)
                    append("refresh_token", refreshToken)
                    append("ts", "${System.currentTimeMillis() / 1000}")
                }
            ))
        }.body()

}
