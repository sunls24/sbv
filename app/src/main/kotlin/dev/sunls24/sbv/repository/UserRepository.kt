package dev.sunls24.sbv.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sunls24.biliapi.http.BiliHttpApi
import dev.sunls24.biliapi.http.entity.user.MyInfoData
import dev.sunls24.biliapi.repositories.AuthRepository
import dev.sunls24.sbv.entity.AuthData
import dev.sunls24.sbv.util.Prefs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single

@Single
class UserRepository(
    private val authRepository: AuthRepository
) {
    val isLogin get() = authRepository.isLoggedIn
    val isLoginFlow = authRepository.isLoggedInFlow
    val userId get() = authRepository.mid
    val sessionData get() = authRepository.sessionData
    var username by mutableStateOf(Prefs.username)
    var avatar by mutableStateOf(Prefs.avatar)
    var profile: MyInfoData? by mutableStateOf(null)
        private set
    private val profileMutex = Mutex()

    suspend fun addUser(authData: AuthData) {
        Prefs.saveAuthData(authData)
        profile = null
        updateSession(authData)
        updateProfile()
    }

    suspend fun logout() {
        Prefs.saveAuthData(null)
        username = ""
        avatar = ""
        profile = null
        Prefs.username = ""
        Prefs.avatar = ""
        updateSession(null)
    }

    suspend fun updateProfile(force: Boolean = false): Result<MyInfoData> =
        profileMutex.withLock {
            profile?.takeIf { !force }?.let { return@withLock Result.success(it) }
            runCatching {
                BiliHttpApi.getUserSelfInfo(
                    sessData = authRepository.sessionData.orEmpty()
                ).getResponseData()
            }.onSuccess {
                profile = it
                username = it.name
                avatar = it.face
                Prefs.username = it.name
                Prefs.avatar = it.face
            }
        }

    suspend fun restoreSession() {
        authRepository.onAppTokenUpdated = { token ->
            Prefs.authData?.let { current ->
                val updated = current.copy(
                    accessToken = token.accessToken,
                    refreshToken = token.refreshToken,
                    accessTokenExpiresAt = token.expiresAt,
                    refreshTokenExpiresAt = token.refreshTokenExpiresAt
                )
                Prefs.saveAuthData(updated)
            }
        }
        updateSession(Prefs.authData)
    }

    suspend fun refreshAppTokenIfNeeded() {
        authRepository.refreshAppTokenIfNeeded()
    }

    private fun updateSession(authData: AuthData?) {
        authRepository.updateSession(
            sessionData = authData?.sessData,
            biliJct = authData?.biliJct,
            mid = authData?.uid,
            accessToken = authData?.accessToken,
            refreshToken = authData?.refreshToken,
            accessTokenExpiresAt = authData?.accessTokenExpiresAt,
            refreshTokenExpiresAt = authData?.refreshTokenExpiresAt,
            buvid = Prefs.buvid,
            buvid3 = Prefs.buvid3
        )
    }

}
