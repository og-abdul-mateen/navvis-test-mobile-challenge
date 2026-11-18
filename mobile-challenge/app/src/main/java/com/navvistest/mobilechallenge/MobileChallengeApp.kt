package com.navvistest.mobilechallenge

import android.app.Application
import com.navvistest.mobilechallenge.data.ApiConfig
import com.navvistest.mobilechallenge.di.AppContainer

class MobileChallengeApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(
            apiConfig = ApiConfig(
                baseUrl = BuildConfig.BASE_URL,
                useMock = BuildConfig.USE_MOCK,
            ),
        )
    }
}
