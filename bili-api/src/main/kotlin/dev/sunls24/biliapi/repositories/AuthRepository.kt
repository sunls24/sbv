package dev.sunls24.biliapi.repositories

import dev.sunls24.biliapi.http.BiliPassportHttpApi
import dev.sunls24.biliapi.http.entity.login.qr.AppTokenRefreshData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single

@Single
class AuthRepository(
    private val channelRepository: ChannelRepository
) {
    data class AppToken(
        val accessToken: String,
        val refreshToken: String,
        val expiresAt: Long,
        val refreshTokenExpiresAt: Long?
    )

    var sessionData: String? = null
        private set
    var biliJct: String? = null
        private set
    var accessToken: String? = null
        private set
    var mid: Long? = null
        private set
    var buvid3: String? = null
        private set
    var buvid: String? = null
        private set

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedInFlow = _isLoggedIn.asStateFlow()
    val isLoggedIn: Boolean get() = _isLoggedIn.value

    private var refreshToken: String? = null
    private var accessTokenExpiresAt: Long? = null
    private var refreshTokenExpiresAt: Long? = null
    private val refreshMutex = Mutex()

    var onAppTokenUpdated: (suspend (AppToken) -> Unit)? = null

    fun updateSession(
        sessionData: String?,
        biliJct: String?,
        mid: Long?,
        accessToken: String?,
        refreshToken: String?,
        accessTokenExpiresAt: Long?,
        refreshTokenExpiresAt: Long?,
        buvid: String,
        buvid3: String
    ) {
        this.sessionData = sessionData
        this.biliJct = biliJct
        this.mid = mid
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.accessTokenExpiresAt = accessTokenExpiresAt
        this.refreshTokenExpiresAt = refreshTokenExpiresAt
        this.buvid = buvid
        this.buvid3 = buvid3
        _isLoggedIn.value = !sessionData.isNullOrEmpty()
        channelRepository.initDefaultChannel(accessToken, buvid)
    }

    suspend fun refreshAppTokenIfNeeded(bufferMillis: Long = 10 * 60 * 1000L) {
        refreshMutex.withLock {
            if (!needsRefresh(bufferMillis)) return
            val currentAccessToken = accessToken ?: return
            val currentRefreshToken = refreshToken ?: return
            var data: AppTokenRefreshData? = null
            for (version in listOf(3, 2)) {
                try {
                    val response = BiliPassportHttpApi.refreshAccessToken(
                        accessToken = currentAccessToken,
                        refreshToken = currentRefreshToken,
                        version = version
                    )
                    data = response.data.takeIf { response.code == 0 }
                    if (data != null) break
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                }
            }
            data ?: return

            val normalized = data.normalizedToken()
            val now = System.currentTimeMillis()
            val token = AppToken(
                accessToken = normalized.accessToken ?: currentAccessToken,
                refreshToken = normalized.refreshToken ?: currentRefreshToken,
                expiresAt = normalized.accessTokenExpiresIn
                    ?.let { now + it * 1000L }
                    ?: accessTokenExpiresAt
                    ?: return,
                refreshTokenExpiresAt = normalized.refreshTokenExpiresIn
                    ?.let { now + it * 1000L }
                    ?: refreshTokenExpiresAt
            )
            if (refreshToken != currentRefreshToken) return
            accessToken = token.accessToken
            refreshToken = token.refreshToken
            accessTokenExpiresAt = token.expiresAt
            refreshTokenExpiresAt = token.refreshTokenExpiresAt
            channelRepository.initDefaultChannel(token.accessToken, buvid.orEmpty())
            onAppTokenUpdated?.invoke(token)
        }
    }

    private fun needsRefresh(bufferMillis: Long): Boolean {
        if (accessToken.isNullOrEmpty() || refreshToken.isNullOrEmpty()) return false
        val expiresAt = accessTokenExpiresAt ?: return false
        val refreshExpiresAt = refreshTokenExpiresAt
        val now = System.currentTimeMillis()
        return expiresAt <= now + bufferMillis && (refreshExpiresAt == null || refreshExpiresAt > now)
    }
}
