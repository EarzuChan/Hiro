package me.earzuchan.hiro.example.resources

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.integerArrayResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.earzuchan.hiro.compose.setHiroComposeContent

class ResourcesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setHiroComposeContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) { ResourcesShowcase() }
            }
        }
    }
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
private fun ResourcesShowcase() {
    val configuration = LocalConfiguration.current
    val assets = LocalContext.current.assets
    val padding = dimensionResource(R.dimen.card_padding)

    Column(Modifier.safeContentPadding().verticalScroll(rememberScrollState()).padding(padding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.title), style = MaterialTheme.typography.headlineSmall)
        Text("Configuration：locale=${configuration.locales[0]}，orientation=${configuration.orientation}，night=${configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK}")

        Section("链接与触感")
        val haptic = LocalHapticFeedback.current
        Text(buildAnnotatedString {
            append("打开 ")
            withLink(LinkAnnotation.Url("https://github.com/EarzuChan/Hiro")) { append("Hiro 仓库") }
        })
        Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }) { Text("长按式触感") }

        Section("字符串")
        Text(stringResource(R.string.greeting, "Hiro"))
        Text(pluralStringResource(R.plurals.apples, 1, 1) + " / " + pluralStringResource(R.plurals.apples, 5, 5))
        Text(stringArrayResource(R.array.planets).joinToString())

        Section("基础值")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(32.dp).background(colorResource(R.color.accent)))
            Text("dimen=$padding，integer=${integerResource(R.integer.answer)}，bool=${booleanResource(R.bool.is_hiro)}，primes=${integerArrayResource(R.array.primes).joinToString()}")
        }

        Section("矢量（渐变填充）与位图")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_gradient_star), contentDescription = null)
            Image(painterResource(R.drawable.landscape), contentDescription = null, modifier = Modifier.size(160.dp, 94.dp), contentScale = ContentScale.FillBounds)
        }
        Text("imageResource 尺寸：" + ImageBitmap.imageResource(R.drawable.landscape).let { "${it.width}x${it.height}" })

        Section("动画矢量（点击切换）")
        var atEnd by remember { mutableStateOf(false) }
        Image(rememberAnimatedVectorPainter(AnimatedImageVector.animatedVectorResource(R.drawable.avd_spinner), atEnd), contentDescription = null, modifier = Modifier.clickable { atEnd = !atEnd })

        Section("九宫格")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(240.dp, 72.dp).paint(painterResource(R.drawable.bubble), contentScale = ContentScale.FillBounds), contentAlignment = Alignment.Center) { Text("拉伸到 240x72", color = Color.White) }
            Box(Modifier.size(90.dp, 120.dp).paint(painterResource(R.drawable.bubble), contentScale = ContentScale.FillBounds), contentAlignment = Alignment.Center) { Text("90x120", color = Color.White) }
        }

        Section("形状")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.shape_card), contentDescription = null)
            Image(painterResource(R.drawable.shape_dot), contentDescription = null)
        }

        Section("字体")
        Text("Dancing Script 来自 res/font", fontFamily = FontFamily(Font(R.font.dancing_script)), fontSize = 26.sp)
        Text("Cutive Mono from assets 0123", fontFamily = FontFamily(Font("fonts/CutiveMono.ttf", assets)), fontSize = 20.sp)
    }
}

@Composable
private fun Section(title: String) {
    HorizontalDivider()
    Text(title, style = MaterialTheme.typography.titleMedium)
}
