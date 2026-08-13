package dev.sunls24.sbv.player

import android.content.Context
import okhttp3.OkHttpClient
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object OkHttpUtil {
    @Volatile
    private var client: OkHttpClient? = null

    fun generateCustomSslOkHttpClient(context: Context): OkHttpClient {
        client?.let { return it }
        return synchronized(this) {
            client ?: createCustomSslOkHttpClient(context.applicationContext).also { client = it }
        }
    }

    private fun createCustomSslOkHttpClient(context: Context): OkHttpClient {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val customCaMap = mapOf(
            "custom:r5" to "GlobalSign ECC Root CA R5.crt"
        )

        val keyStoreType = KeyStore.getDefaultType()
        val systemKeyStore = KeyStore.getInstance("AndroidCAStore").apply {
            load(null, null)
        }
        val customKeyStore = KeyStore.getInstance(keyStoreType).apply {
            load(null, null)

            systemKeyStore.aliases().toList().forEach {
                setCertificateEntry(it, systemKeyStore.getCertificate(it))
            }
            customCaMap.forEach { (alias, caFilename) ->
                val certificate = context.assets.open(caFilename).use {
                    certificateFactory.generateCertificate(it)
                }
                setCertificateEntry(alias, certificate)
            }
        }

        val tmfAlgorithm: String = TrustManagerFactory.getDefaultAlgorithm()
        val trustManagerFactory: TrustManagerFactory =
            TrustManagerFactory.getInstance(tmfAlgorithm).apply {
                init(customKeyStore)
            }

        val sslContext: SSLContext = SSLContext.getInstance("TLS").apply {
            init(null, trustManagerFactory.trustManagers, null)
        }

        return OkHttpClient.Builder()
            .sslSocketFactory(
                sslContext.socketFactory,
                trustManagerFactory.trustManagers[0] as X509TrustManager
            )
            .hostnameVerifier { hostname, session ->
                val verifier = HttpsURLConnection.getDefaultHostnameVerifier()
                verifier.verify(hostname, session) || when {
                    hostname.endsWith(".bilivideo.cn") -> verifier.verify(
                        hostname.removeSuffix(".bilivideo.cn") + ".bilivideo.com",
                        session,
                    )
                    hostname.endsWith(".bilivideo.com") -> verifier.verify(
                        hostname.removeSuffix(".bilivideo.com") + ".bilivideo.cn",
                        session,
                    )
                    else -> false
                }
            }
            .build()
    }
}
