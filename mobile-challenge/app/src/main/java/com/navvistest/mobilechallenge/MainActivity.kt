package com.navvistest.mobilechallenge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.navvistest.mobilechallenge.ui.PlaceholderScreen
import com.navvistest.mobilechallenge.ui.theme.MobileChallengeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MobileChallengeApp).container
        setContent {
            MobileChallengeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PlaceholderScreen(
                        useMock = container.apiConfig.useMock,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
