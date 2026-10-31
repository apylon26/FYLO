package com.apylon26.fylo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private var visible by mutableStateOf(false)
    private var refresh by mutableStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FyloApp(visible, refresh) }
    }
    override fun onResume() { super.onResume(); refresh++; visible = true }
    override fun onPause() { visible = false; super.onPause() }
}
