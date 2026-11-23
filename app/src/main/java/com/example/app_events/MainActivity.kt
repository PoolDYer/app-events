package com.example.app_events

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.app_events.presentation.EventlyApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF514095),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFE9E0FF),
                    onPrimaryContainer = Color(0xFF261350),
                    secondaryContainer = Color(0xFFE1EDCE),
                    onSecondaryContainer = Color(0xFF243214),
                    background = Color(0xFFF9F7FD),
                    surface = Color(0xFFF9F7FD),
                    surfaceContainerLow = Color(0xFFF1EDF9)
                )
            ) {
                EventlyApp()
            }
        }
    }
}
