package com.navvistest.mobilechallenge.data

/**
 * Where the app gets its data from. Set per product flavor through BuildConfig.
 */
data class ApiConfig(
    val baseUrl: String,
    val useMock: Boolean,
)
