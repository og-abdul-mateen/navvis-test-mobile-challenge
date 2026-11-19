package com.navvistest.mobilechallenge.di

import com.navvistest.mobilechallenge.data.ApiConfig
import com.navvistest.mobilechallenge.data.StreamApi
import com.navvistest.mobilechallenge.data.StreamRepository
import com.navvistest.mobilechallenge.data.mock.MockInterceptor
import com.navvistest.mobilechallenge.data.mock.MockScenarioType
import com.navvistest.mobilechallenge.domain.ResultComputer
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

    private val baseUrl = apiConfig.baseUrl.toHttpUrl()

    // The real server keeps one position per stream, so the app keeps one repository to match.
    private val realRepository by lazy { StreamRepository(StreamApi(httpClient, baseUrl)) }

    val resultComputer = ResultComputer()

    /** The scenario a mock session starts with. Null in the real build, which has no scenarios. */
    val defaultScenario: MockScenarioType? = if (apiConfig.useMock) MockScenarioType.Normal else null

    /**
     * The repository for a new session.
     *
     * With a scenario, it talks to a brand new mock server built from that scenario, so the
     * session starts from scratch. The mock answers inside OkHttp, before anything reaches
     * the network. Without one, it's always the same repository for the real server.
     */
    fun streamRepository(scenario: MockScenarioType?): StreamRepository {
        if (scenario == null) return realRepository
        // newBuilder() shares the connection pool and threads with the main client.
        val mockClient = httpClient.newBuilder()
            .addInterceptor(MockInterceptor(scenario.build()))
            .build()
        return StreamRepository(StreamApi(mockClient, baseUrl))
    }
}
