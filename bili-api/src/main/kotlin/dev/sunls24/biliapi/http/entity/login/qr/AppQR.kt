package dev.sunls24.biliapi.http.entity.login.qr

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppQRDataRequest(
    val url: String,
    @SerialName("auth_code")
    val authCode: String
)

@Serializable
data class AppQRLoginData(
    @SerialName("is_new")
    val isNew: Boolean = false,
    val mid: Long? = null,
    @SerialName("access_token")
    val accessToken: String? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("expires_in")
    val expiresIn: Int? = null,
    @SerialName("token_info")
    val tokenInfo: TokenInfo? = null,
    @SerialName("refresh_token_info")
    val refreshTokenInfo: RefreshTokenInfo? = null,
    @SerialName("cookie_info")
    val cookieInfo: CookieInfo? = null,
    val sso: List<String> = emptyList()
) {
    @Serializable
    data class TokenInfo(
        val mid: Long? = null,
        @SerialName("expires_in")
        val expiresIn: Int? = null,
        @SerialName("access_token")
        val accessToken: String? = null,
        @SerialName("refresh_token")
        val refreshToken: String? = null
    )

    @Serializable
    data class RefreshTokenInfo(
        @SerialName("expires_in")
        val expiresIn: Int? = null
    )

    @Serializable
    data class CookieInfo(
        val cookies: List<Cookie>,
        val domains: List<String>
    ) {
        @Serializable
        data class Cookie(
            var name: String,
            var value: String,
            @SerialName("http_only")
            var httpOnly: Int,
            val expires: Int,
            var secure: Int
        )
    }

    fun normalizedToken() = NormalizedAppToken(
        accessToken = accessToken ?: tokenInfo?.accessToken,
        refreshToken = refreshToken ?: tokenInfo?.refreshToken,
        accessTokenExpiresIn = tokenInfo?.expiresIn ?: expiresIn,
        refreshTokenExpiresIn = refreshTokenInfo?.expiresIn
    )
}

@Serializable
data class AppTokenRefreshData(
    @SerialName("access_token")
    val accessToken: String? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("expires_in")
    val expiresIn: Int? = null,
    @SerialName("token_info")
    val tokenInfo: TokenInfo? = null,
    @SerialName("refresh_token_info")
    val refreshTokenInfo: RefreshTokenInfo? = null
) {
    @Serializable
    data class TokenInfo(
        @SerialName("expires_in")
        val expiresIn: Int? = null,
        @SerialName("access_token")
        val accessToken: String? = null,
        @SerialName("refresh_token")
        val refreshToken: String? = null
    )

    @Serializable
    data class RefreshTokenInfo(
        @SerialName("expires_in")
        val expiresIn: Int? = null
    )

    fun normalizedToken() = NormalizedAppToken(
        accessToken = accessToken ?: tokenInfo?.accessToken,
        refreshToken = refreshToken ?: tokenInfo?.refreshToken,
        accessTokenExpiresIn = tokenInfo?.expiresIn ?: expiresIn,
        refreshTokenExpiresIn = refreshTokenInfo?.expiresIn
    )
}

data class NormalizedAppToken(
    val accessToken: String?,
    val refreshToken: String?,
    val accessTokenExpiresIn: Int?,
    val refreshTokenExpiresIn: Int?
)
