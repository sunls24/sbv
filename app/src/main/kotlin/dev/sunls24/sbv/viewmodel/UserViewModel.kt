package dev.sunls24.sbv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sunls24.biliapi.http.entity.AuthFailureException
import dev.sunls24.biliapi.http.entity.user.MyInfoData
import dev.sunls24.sbv.SBVApp
import dev.sunls24.sbv.R
import dev.sunls24.sbv.repository.UserRepository
import dev.sunls24.sbv.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class UserViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    val isLogin get() = userRepository.isLogin
    val isLoginFlow = userRepository.isLoginFlow
    val username get() = userRepository.username
    val face get() = userRepository.avatar

    val responseData: MyInfoData? get() = userRepository.profile

    fun updateUserInfo(forceUpdate: Boolean = false) {
        if (!userRepository.isLogin) return
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.updateProfile(forceUpdate).onFailure {
                when (it) {
                    is AuthFailureException -> {
                        withContext(Dispatchers.Main) {
                            SBVApp.context.getString(R.string.exception_auth_failure)
                                .toast(SBVApp.context)
                        }
                        userRepository.logout()
                    }

                    else -> {
                        withContext(Dispatchers.Main) {
                            "获取用户信息失败：${it.message}".toast(SBVApp.context)
                        }
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.logout()
        }
    }
}
