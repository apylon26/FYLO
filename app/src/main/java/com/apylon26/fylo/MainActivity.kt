package com.apylon26.fylo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.apylon26.fylo.data.FyloStore
import com.apylon26.fylo.ui.FyloShell

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val store = remember { FyloStore(applicationContext) }
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFF37A9FF), secondary = Color(0xFFFF8B24), tertiary = Color(0xFFFF8B24),
                background = Color(0xFF09111E), surface = Color(0xFF142136),
                surfaceVariant = Color(0xFF20314A)
            )) { FyloShell(store) }
        }
    }
}
