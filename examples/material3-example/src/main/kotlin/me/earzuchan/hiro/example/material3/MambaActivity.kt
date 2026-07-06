package me.earzuchan.hiro.example.material3

import android.app.Activity
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat.enableEdgeToEdge
import me.earzuchan.hiro.compose.setHiroComposeContent

class MambaActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(window)
        setHiroComposeContent {
            var useDark by remember { mutableStateOf(true) }

            MaterialTheme(if (useDark) darkColorScheme() else lightColorScheme()) {
                Column(Modifier.background(MaterialTheme.colorScheme.background).safeContentPadding().fillMaxSize(), Arrangement.spacedBy(40.dp), Alignment.CenterHorizontally) {
                    Box(Modifier.size(400.dp).clickable { })

                    Button({ useDark = !useDark }) { Text("切换颜色模式") }
                }
            }
        }
    }
}