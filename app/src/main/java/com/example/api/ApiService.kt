package com.example.api

import android.os.Build
import android.util.Log
import com.example.data.GeminiRequest
import com.example.data.GeminiResponse
import com.example.data.OpenMeteoResponse
import com.example.data.WhatIsTodayAnnivResponse
import com.example.data.WhatIsTodayFamousBirthdayResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import kotlinx.coroutines.delay
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.io.IOException
import java.net.Socket
import java.net.InetAddress
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

class Tls12SocketFactory(private val delegate: SSLSocketFactory) : SSLSocketFactory() {
    override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites
    override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites

    override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket {
        return enableTlsOnSocket(delegate.createSocket(s, host, port, autoClose))
    }

    override fun createSocket(host: String, port: Int): Socket {
        return enableTlsOnSocket(delegate.createSocket(host, port))
    }

    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket {
        return enableTlsOnSocket(delegate.createSocket(host, port, localHost, localPort))
    }

    override fun createSocket(host: InetAddress, port: Int): Socket {
        return enableTlsOnSocket(delegate.createSocket(host, port))
    }

    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket {
        return enableTlsOnSocket(delegate.createSocket(address, port, localAddress, localPort))
    }

    private fun enableTlsOnSocket(socket: Socket): Socket {
        if (socket is SSLSocket) {
            socket.enabledProtocols = arrayOf("TLSv1.1", "TLSv1.2")
        }
        return socket
    }
}

interface OpenMeteoService {
    @GET("forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double = 35.1284,
        @Query("longitude") longitude: Double = 136.0964,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum",
        @Query("timezone") timezone: String = "Asia/Tokyo"
    ): OpenMeteoResponse
}

interface GeminiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContentWithModel(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

suspend fun GeminiService.generateContentWithRetry(
    apiKey: String,
    request: GeminiRequest,
    preferredModel: String = "gemini-3.5-flash",
    fallbackModel: String = "gemini-flash-latest",
    maxRetriesPerModel: Int = 2
): GeminiResponse {
    var lastException: Exception? = null
    val modelsToTry = listOf(preferredModel, fallbackModel).distinct()

    for (model in modelsToTry) {
        for (attempt in 0..maxRetriesPerModel) {
            try {
                return generateContentWithModel(model, apiKey, request)
            } catch (e: HttpException) {
                lastException = e
                val code = e.code()
                // Transient server overload or rate limiting: 503 (Unavailable), 429 (Rate Limit), 500/502/504
                if (code == 503 || code == 429 || code == 500 || code == 502 || code == 504) {
                    if (attempt < maxRetriesPerModel) {
                        delay((attempt + 1) * 800L)
                        continue
                    }
                } else {
                    // Non-transient client error (e.g. 400 or 403)
                    throw e
                }
            } catch (e: IOException) {
                lastException = e
                if (attempt < maxRetriesPerModel) {
                    delay((attempt + 1) * 800L)
                    continue
                }
            }
        }
    }
    throw lastException ?: IOException("Failed to generate content with Gemini after retries")
}

interface WhatIsTodayService {
    @GET("v3/anniv/{mmdd}")
    suspend fun getAnniversaries(@Path("mmdd") mmdd: String): WhatIsTodayAnnivResponse

    @GET("v3/famousbirthday/{mmdd}")
    suspend fun getFamousBirthday(@Path("mmdd") mmdd: String): WhatIsTodayFamousBirthdayResponse
}

object ApiClient {
    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val okHttpClient = OkHttpClient.Builder().apply {
        retryOnConnectionFailure(true)
        connectTimeout(30, TimeUnit.SECONDS)
        readTimeout(30, TimeUnit.SECONDS)
        writeTimeout(30, TimeUnit.SECONDS)

        if (Build.VERSION.SDK_INT <= 22) {
            try {
                val trustAllCerts = object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }

                val sslContext = SSLContext.getInstance("TLSv1.2")
                sslContext.init(null, arrayOf(trustAllCerts), java.security.SecureRandom())

                sslSocketFactory(Tls12SocketFactory(sslContext.socketFactory), trustAllCerts)

                val spec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_1)
                    .build()
                connectionSpecs(listOf(spec, ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT))

                hostnameVerifier { _, _ -> true }
            } catch (e: Exception) {
                Log.e("ApiClient", "Error enabling TLS 1.2 on legacy device", e)
            }
        }
    }.build()

    val openMeteoService: OpenMeteoService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/v1/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoService::class.java)
    }

    val geminiService: GeminiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiService::class.java)
    }

    val whatIsTodayService: WhatIsTodayService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.whatistoday.cyou/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WhatIsTodayService::class.java)
    }
}
