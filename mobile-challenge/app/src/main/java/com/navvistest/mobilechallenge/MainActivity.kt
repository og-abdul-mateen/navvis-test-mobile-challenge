package com.navvistest.mobilechallenge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.navvistest.mobilechallenge.ui.StreamRoute
import com.navvistest.mobilechallenge.ui.StreamViewModel
import com.navvistest.mobilechallenge.ui.theme.MobileChallengeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MobileChallengeApp).container
        setContent {
            MobileChallengeTheme {
                val viewModel: StreamViewModel = viewModel(factory = StreamViewModel.factory(container))
                StreamRoute(viewModel = viewModel, useMock = container.apiConfig.useMock)
            }
        }
    }
}
