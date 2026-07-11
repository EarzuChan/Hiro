package me.earzuchan.hiro.example.material3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import me.earzuchan.hiro.compose.setHiroComposeContent

class MambaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setHiroComposeContent {
            var useDark by remember { mutableStateOf(true) }
            val scrollState = rememberScrollState()

            MaterialTheme(if (useDark) darkColorScheme() else lightColorScheme()) {
                Column(Modifier.verticalScroll(scrollState).background(MaterialTheme.colorScheme.background).safeContentPadding().fillMaxSize(), Arrangement.spacedBy(40.dp), Alignment.CenterHorizontally) {
                    Text("下面有个 400DP 的水波纹区域给你玩", color = MaterialTheme.colorScheme.onBackground)

                    Box(Modifier.size(400.dp).clickable { })

                    Button({ useDark = !useDark }) { Text("切换颜色模式") }

                    for (i in 1..20) Text("测试，为了能滚动。这是第${i}行", color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}
