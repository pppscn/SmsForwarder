package com.idormy.sms.forwarder.utils

import android.annotation.SuppressLint
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.*

@Suppress("unused")
@SuppressLint("ALL")
object CertUtils {

    //获取这个SSLSocketFactory
    val sSLSocketFactory: SSLSocketFactory
        get() = try {
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustManager, SecureRandom())
            sslContext.socketFactory
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    //获取TrustManager
    private val trustManager: Array<TrustManager>
        get() = arrayOf(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            }
        )

    //获取HostnameVerifier
    val hostnameVerifier: HostnameVerifier
        get() = HostnameVerifier { _: String?, _: SSLSession? -> true }
    val x509TrustManager: X509TrustManager?
        get() {
            var trustManager: X509TrustManager? = null
            try {
                val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                trustManagerFactory.init(null as KeyStore?)
                val trustManagers = trustManagerFactory.trustManagers
                check(!(trustManagers.size != 1 || trustManagers[0] !is X509TrustManager)) { "Unexpected default trust managers:" + Arrays.toString(trustManagers) }
                trustManager = trustManagers[0] as X509TrustManager
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return trustManager
        }
}