package me.earzuchan.hiro.example.thirdparty

import android.app.Activity
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat.enableEdgeToEdge
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import me.earzuchan.hiro.compose.setHiroComposeContent

class MambaActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(window)
        setHiroComposeContent {
            Box(Modifier.fillMaxSize()) {
                val backgroundColor = Color.White

                val backdrop = rememberLayerBackdrop {
                    drawRect(backgroundColor)
                    drawContent()
                }

                val rowColors = listOf(Color(0xFFFF3B5A), Color(0xFFFF9F3F), Color(0xFF3BCA5A), Color(0xFF00BFCE))
                val totalItems = 48
                val columnsCount = 4


                LazyVerticalGrid(
                    GridCells.Fixed(columnsCount), Modifier.layerBackdrop(backdrop).fillMaxSize().background(backgroundColor),
                    horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = WindowInsets.safeContent.asPaddingValues() + PaddingValues(16.dp),
                ) {
                    items(totalItems) { index ->
                        val colorIndex = index % columnsCount
                        val backgroundColor = rowColors[colorIndex]

                        Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(backgroundColor), Alignment.Center) {
                            BasicText((index + 1).toString(), style = TextStyle(Color.White, 18.sp))
                        }
                    }
                }

                Box(Modifier.safeContentPadding().padding(32.dp).drawBackdrop(backdrop, { CircleShape }, {
                    vibrancy()
                    blur(6.dp.toPx())
                    lens(16.dp.toPx(), 32.dp.toPx())
                }, onDrawSurface = { drawRect(Color.White.copy(0.5f)) }).height(80.dp).fillMaxWidth().align(Alignment.BottomCenter))
            }
        }
    }
}