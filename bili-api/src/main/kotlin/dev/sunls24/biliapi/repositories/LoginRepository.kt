package dev.sunls24.biliapi.repositories

import dev.sunls24.biliapi.entity.login.QrLoginData
import dev.sunls24.biliapi.entity.login.QrLoginResult
import dev.sunls24.biliapi.entity.login.QrLoginState
import dev.sunls24.biliapi.entity.login.WebCookies
import dev.sunls24.biliapi.http.BiliPassportHttpApi
import org.koin.core.annotation.Single

@Single
class LoginRepository {
    /**
     * 请求扫码登录的二维码，支持 Http+gRPC 接口使用
     */
    suspend fun requestAppQrLogin(): QrLoginData {
        val response = BiliPassportHttpApi.getAppQRUrl().getResponseData()
        return QrLoginData(
            url = response.url,
            key = response.authCode
        )
    }

    /**
     * 检查扫码登录情况
     *
     * @param authCode 二维码内容
     */
    suspend fun checkAppQrLoginState(authCode: String): QrLoginResult {
        val response = BiliPassportHttpApi.loginWithAppQR(authCode)
        if (response.code == 0) {
            val data = response.getResponseData()
            val cookies = requireNotNull(data.cookieInfo) {
                "Cookie info not found"
            }.cookies.associateBy { it.name }
            val sessDataCookie = cookies["SESSDATA"]
                ?: throw IllegalArgumentException("Cookie SESSDATA not found")
            val token = data.normalizedToken()
            val expiresIn = requireNotNull(token.accessTokenExpiresIn) {
                "Access token expiry not found"
            }
            val now = System.currentTimeMillis()
            return QrLoginResult(
                state = QrLoginState.Success,
                accessToken = requireNotNull(token.accessToken) {
                    "Access token not found"
                },
                refreshToken = requireNotNull(token.refreshToken) {
                    "Refresh token not found"
                },
                accessTokenExpiresAt = now + expiresIn * 1000L,
                refreshTokenExpiresAt = token.refreshTokenExpiresIn
                    ?.let { now + it * 1000L },
                cookies = WebCookies(
                    dedeUserId = cookies["DedeUserID"]?.value?.toLong()
                        ?: throw IllegalArgumentException("Cookie DedeUserID not found"),
                    biliJct = cookies["bili_jct"]?.value
                        ?: throw IllegalArgumentException("Cookie bili_jct not found"),
                    sessData = sessDataCookie.value,
                    sessDataExpiresAt = sessDataCookie.expires * 1000L
                )
            )
        }

        val resultState = when (response.code) {
            86039 -> QrLoginState.WaitingForScan
            86090 -> QrLoginState.WaitingForConfirm
            86038 -> QrLoginState.Expired
            else -> QrLoginState.Unknown
        }
        return QrLoginResult(state = resultState)
    }

}
