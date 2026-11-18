package com.navvistest.mobilechallenge.di

import com.navvistest.mobilechallenge.data.ApiConfig
import com.navvistest.mobilechallenge.data.StreamApi
import com.navvistest.mobilechallenge.data.StreamRepository
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Holds the app's shared objects. Created once in [com.navvistest.mobilechallenge.MobileChallengeApp].
 *
 * Plain constructor injection is enough for the handful of classes in this app,
 * so there's no DI framework.
 */
class AppContainer(val apiConfig: ApiConfig) {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        // Upper limit for the whole request, including reading the body.
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    private val streamApi = StreamApi(httpClient, apiConfig.baseUrl.toHttpUrl())

    val streamRepository = StreamRepository(streamApi)
}
