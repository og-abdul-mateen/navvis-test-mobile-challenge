package com.navvistest.mobilechallenge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.navvistest.mobilechallenge.ui.PlaceholderScreen
import com.navvistest.mobilechallenge.ui.StreamViewModel
import com.navvistest.mobilechallenge.ui.summary
import com.navvistest.mobilechallenge.ui.theme.MobileChallengeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MobileChallengeApp).container
        setContent {
            MobileChallengeTheme {
                val viewModel: StreamViewModel = viewModel(factory = StreamViewModel.factory(container))
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PlaceholderScreen(
                        useMock = container.apiConfig.useMock,
                        summary = uiState.summary(),
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
