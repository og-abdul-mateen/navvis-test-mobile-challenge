package com.navvistest.mobilechallenge.di

import com.navvistest.mobilechallenge.data.ApiConfig

/**
 * Holds the app's shared objects. Created once in [com.navvistest.mobilechallenge.MobileChallengeApp].
 *
 * Plain constructor injection is enough for the handful of classes in this app,
 * so there's no DI framework.
 */
class AppContainer(val apiConfig: ApiConfig)
