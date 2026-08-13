package dev.sunls24.sbv.viewmodel.user

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sunls24.biliapi.entity.user.FollowedUser
import dev.sunls24.biliapi.repositories.UserRepository
import dev.sunls24.biliapi.repositories.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class FollowViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    val followedUsers = mutableStateListOf<FollowedUser>()
    var updating by mutableStateOf(false)
        private set
    var hasMore by mutableStateOf(true)
        private set
    var loadFailed by mutableStateOf(false)
        private set

    private var nextPage = 1

    init {
        loadMore()
    }

    fun loadMore() {
        if (updating || !hasMore) return
        val userId = authRepository.mid ?: run {
            hasMore = false
            return
        }
        updating = true
        loadFailed = false
        viewModelScope.launch {
            try {
                val page = withContext(Dispatchers.IO) {
                    userRepository.getFollowedUsers(
                        mid = userId,
                        page = nextPage
                    )
                }
                followedUsers.addAll(page.items)
                nextPage = page.nextPage
                hasMore = page.hasMore
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                loadFailed = true
            } finally {
                updating = false
            }
        }
    }
}
