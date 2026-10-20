@file:JvmName("PainterResources_androidKt")

package androidx.compose.ui.res

import android.graphics.BitmapFactory
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import me.earzuchan.hiro.compose.internal.res.HiroNinePatchChunk
import me.earzuchan.hiro.compose.internal.res.HiroNinePatchPainter
import me.earzuchan.hiro.compose.internal.res.HiroShapePainter
import me.earzuchan.hiro.compose.internal.res.HiroShapeParser
import me.earzuchan.hiro.compose.internal.res.currentResources
import me.earzuchan.hiro.compose.internal.res.vector.seekToStartTag
import me.earzuchan.hiro.compose.internal.util.toComposeImageBitmap

/**
 * 加载安卓 drawable 资源为 Painter
 *
 * 支持矢量图（vector）、位图（PNG、JPG、WebP 等）、九宫格（.9.png）与形状（shape）；
 * 动画矢量请用 AnimatedImageVector.animatedVectorResource，selector、layer-list 等其他 XML Drawable 暂不支持
 */
@Composable
fun painterResource(@DrawableRes id: Int): Painter {
    val res = currentResources()
    val theme = LocalContext.current.theme
    val configuration = LocalConfiguration.current
    val value = remember(id, configuration) { TypedValue().also { res.getValue(id, it, true) } }
    val path = value.string?.toString() ?: throw ResourceResolutionException("资源 0x${id.toString(16)} 不是文件类资源", IllegalArgumentException())

    return when {
        path.endsWith(".xml") -> {
            val rootTag = remember(path) { res.getXml(id).use { it.seekToStartTag().name } }
            when (rootTag) {
                "vector" -> rememberVectorPainter(ImageVector.vectorResource(id))

                "shape" -> {
                    val density = LocalDensity.current
                    remember(path, theme, configuration, density) { HiroShapePainter(res.getXml(id).use { HiroShapeParser.parse(res, theme, it.apply { seekToStartTag() }) }, density) }
                }

                else -> throw IllegalArgumentException("painterResource 暂不支持 <$rootTag> 类型的 Drawable（$path）；动画矢量请用 AnimatedImageVector.animatedVectorResource")
            }
        }

        path.endsWith(".9.png") -> remember(path, theme) { loadNinePatchPainter(path, res, id) }

        else -> {
            val imageBitmap = remember(path, id, theme) { loadImageBitmapResource(path, res, id) }
            remember(imageBitmap) { BitmapPainter(imageBitmap) }
        }
    }
}

private fun loadImageBitmapResource(path: CharSequence, res: android.content.res.Resources, id: Int): ImageBitmap = try {
    ImageBitmap.imageResource(res, id)
} catch (exception: Exception) {
    throw ResourceResolutionException("加载资源出错：$path", exception)
}

private fun loadNinePatchPainter(path: CharSequence, res: android.content.res.Resources, id: Int): Painter = try {
    val bitmap = BitmapFactory.decodeResource(res, id)
    val chunk = bitmap.ninePatchChunk
    val image = bitmap.toComposeImageBitmap()
    if (chunk == null) BitmapPainter(image) else HiroNinePatchPainter(image, HiroNinePatchChunk.parse(chunk))
} catch (exception: Exception) {
    throw ResourceResolutionException("加载九宫格资源出错：$path", exception)
}

class ResourceResolutionException(message: String, cause: Throwable) : RuntimeException(message, cause)
