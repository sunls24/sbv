package dev.sunls24.sbv.viewmodel.login

import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sunls24.biliapi.entity.login.QrLoginState
import dev.sunls24.biliapi.repositories.LoginRepository
import dev.sunls24.sbv.SBVApp
import dev.sunls24.sbv.entity.AuthData
import dev.sunls24.sbv.repository.UserRepository
import dev.sunls24.sbv.util.toast
import qrcode.QRCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel
import java.io.ByteArrayInputStream

@KoinViewModel
class AppQrLoginViewModel(
    private val userRepository: UserRepository,
    private val loginRepository: LoginRepository
) : ViewModel() {
    var state by mutableStateOf(QrLoginState.Ready)
    private var loginUrl by mutableStateOf("")
    var qrImage by mutableStateOf(ImageBitmap(1, 1, ImageBitmapConfig.Argb8888))
    private var key = ""

    private var pollingJob: Job? = null

    fun requestQRCode() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            state = QrLoginState.RequestingQRCode
            try {
                val qrLoginData = withContext(Dispatchers.IO) {
                    loginRepository.requestAppQrLogin()
                }
                loginUrl = qrLoginData.url
                key = qrLoginData.key
                generateQRImage()

                while (isActive) {
                    delay(1_000)
                    if (checkLoginResult()) break
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                error.message?.toast(SBVApp.context)
                state = QrLoginState.Error
            }
        }
    }

    fun cancelPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun checkLoginResult(): Boolean {
        return try {
            val qrLoginResult = loginRepository.checkAppQrLoginState(key)
            state = qrLoginResult.state
            when (qrLoginResult.state) {
                QrLoginState.WaitingForScan -> {
                    false
                }

                QrLoginState.WaitingForConfirm -> {
                    false
                }

                QrLoginState.Expired -> {
                    true
                }

                QrLoginState.Success -> {

                    val cookies = requireNotNull(qrLoginResult.cookies)
                    val authData = AuthData(
                        uid = cookies.dedeUserId,
                        biliJct = cookies.biliJct,
                        sessData = cookies.sessData,
                        sessDataExpiresAt = cookies.sessDataExpiresAt,
                        accessToken = requireNotNull(qrLoginResult.accessToken),
                        accessTokenExpiresAt = requireNotNull(qrLoginResult.accessTokenExpiresAt),
                        refreshToken = requireNotNull(qrLoginResult.refreshToken),
                        refreshTokenExpiresAt = qrLoginResult.refreshTokenExpiresAt
                    )
                    userRepository.addUser(authData)
                    true
                }

                else -> {
                    false
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            error.message?.toast(SBVApp.context)
            state = QrLoginState.Error
            true
        }
    }

    private fun generateQRImage() {
        val qrBytes = QRCode.ofSquares().build(loginUrl).renderToBytes()
        qrImage = BitmapFactory.decodeStream(ByteArrayInputStream(qrBytes)).asImageBitmap()
    }
}
