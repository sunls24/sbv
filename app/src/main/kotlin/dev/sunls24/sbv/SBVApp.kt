package dev.sunls24.sbv

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import android.webkit.WebView
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dev.sunls24.biliapi.http.BiliHttpApi
import dev.sunls24.biliapi.http.util.BiliAppConf
import dev.sunls24.biliapi.http.util.BiliWebConf
import dev.sunls24.biliapi.repositories.BiliApiModule
import dev.sunls24.sbv.util.Prefs
import dev.sunls24.sbv.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.logger.Level
import org.koin.plugin.module.dsl.startKoin

@KoinApplication(modules = [AppModule::class])
class SBVApp : Application(), KoinComponent {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var initializationJob: Job? = null

    companion object {
        private val _initializationState = MutableStateFlow<AppInitializationState>(
            AppInitializationState.Loading
        )
        val initializationState = _initializationState.asStateFlow()

        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
            private set

        lateinit var settingsDataStore: DataStore<Preferences>
            private set

    }

    override fun onCreate() {
        super.onCreate()

        context = this.applicationContext

        initCoreLibraries()
        initialize()
    }

    fun retryInitialization() {
        if (_initializationState.value is AppInitializationState.Error) initialize()
    }

    private fun initialize() {
        if (initializationJob?.isActive == true) return
        _initializationState.value = AppInitializationState.Loading
        initializationJob = applicationScope.launch {
            runCatching {
                Prefs.init()
                initDeviceInfo()
                BiliHttpApi.init(buvid3 = Prefs.buvid3)
                get<UserRepository>().restoreSession()
            }.onSuccess {
                _initializationState.value = AppInitializationState.Ready
                runCatching { get<UserRepository>().refreshAppTokenIfNeeded() }
            }.onFailure { error ->
                _initializationState.value = AppInitializationState.Error(
                    error.message ?: error::class.java.simpleName
                )
            }
        }
    }

    private fun initCoreLibraries() {
        settingsDataStore = applicationContext.dataStore

        startKoin<SBVApp> {
            androidLogger(Level.NONE)
            androidContext(this@SBVApp)
        }
    }

    private fun initDeviceInfo() {
        BiliAppConf.osVersion = Build.VERSION.RELEASE
        BiliAppConf.model = Build.MODEL
        BiliWebConf.webViewVersion = runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@runCatching null
            WebView.getCurrentWebViewPackage()?.versionName
                ?.substringBefore(".")?.toInt()
        }.getOrDefault(144) ?: 144
    }
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "Settings")

@Module(includes = [BiliApiModule::class])
@ComponentScan
class AppModule

sealed interface AppInitializationState {
    data object Loading : AppInitializationState
    data object Ready : AppInitializationState
    data class Error(val message: String) : AppInitializationState
}
