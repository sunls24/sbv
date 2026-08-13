package dev.sunls24.sbv.entity

import kotlinx.serialization.Serializable

@Serializable
data class AuthData(
    val uid: Long,
    val biliJct: String,
    val sessData: String,
    val sessDataExpiresAt: Long,
    val accessToken: String,
    val accessTokenExpiresAt: Long,
    val refreshToken: String,
    val refreshTokenExpiresAt: Long? = null
)
