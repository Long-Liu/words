package com.teacher.vocabcheck

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.teacher.vocabcheck.ui.MainScreen
import com.teacher.vocabcheck.ui.VocabTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val shortcut = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shortcut.value = intent?.data?.host
        setContent {
            VocabTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(shortcut = shortcut)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shortcut.value = intent.data?.host
    }
}
