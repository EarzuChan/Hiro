package me.earzuchan.hiro.example.material3

import android.app.Activity
import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.core.view.WindowCompat.enableEdgeToEdge
import me.earzuchan.hiro.compose.setHiroComposeContent

class MambaActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(window)
        setHiroComposeContent {
            MaterialTheme {
                Surface {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button({}) { Text("Hello M3") }
                    }
                }
            }
        }
    }
}