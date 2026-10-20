package me.earzuchan.hiro.example.jbres

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.earzuchan.hiro.compose.setHiroComposeContent
import me.earzuchan.hiro.jbres_example.generated.resources.Res
import me.earzuchan.hiro.jbres_example.generated.resources.compose_star
import me.earzuchan.hiro.jbres_example.generated.resources.dancing_script
import me.earzuchan.hiro.jbres_example.generated.resources.greeting
import me.earzuchan.hiro.jbres_example.generated.resources.landscape
import me.earzuchan.hiro.jbres_example.generated.resources.outside
import me.earzuchan.hiro.jbres_example.generated.resources.seasons
import me.earzuchan.hiro.jbres_example.generated.resources.stars
import me.earzuchan.hiro.jbres_example.generated.resources.title
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

class JbResActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setHiroComposeContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) { JbResShowcase() }
            }
        }
    }
}

@Composable
private fun JbResShowcase() {
    var outside by remember { mutableStateOf("…") }
    LaunchedEffect(Unit) { outside = getString(Res.string.outside, getString(Res.string.greeting, "Res")) }

    Column(Modifier.safeContentPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(Res.string.greeting, "Hiro"))
        Text(pluralStringResource(Res.plurals.stars, 1, 1) + " / " + pluralStringResource(Res.plurals.stars, 3, 3))
        Text(stringArrayResource(Res.array.seasons).joinToString())
        Text(outside)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(Res.drawable.compose_star), contentDescription = null)
            Image(painterResource(Res.drawable.landscape), contentDescription = null, modifier = Modifier.size(160.dp, 94.dp))
        }

        Text("Dancing Script 来自 composeResources", fontFamily = FontFamily(Font(Res.font.dancing_script)), fontSize = 26.sp)
    }
}
