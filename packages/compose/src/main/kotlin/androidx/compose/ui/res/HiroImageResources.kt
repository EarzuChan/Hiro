@file:JvmName("ImageResources_androidKt")

package androidx.compose.ui.res

import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import me.earzuchan.hiro.compose.internal.res.currentResources
import me.earzuchan.hiro.compose.internal.util.toComposeImageBitmap

/** 以安卓的方式解码位图资源（按设备密度缩放），再转为 Skia 支撑的 ImageBitmap */
fun ImageBitmap.Companion.imageResource(res: Resources, @DrawableRes id: Int): ImageBitmap {
    val drawable = res.getDrawable(id, null) as? BitmapDrawable ?: throw IllegalArgumentException("资源 0x${id.toString(16)} 不是位图")
    return drawable.bitmap.toComposeImageBitmap()
}

@Composable
fun ImageBitmap.Companion.imageResource(@DrawableRes id: Int): ImageBitmap {
    val resources = currentResources()
    val value = remember { TypedValue() }
    resources.getValue(id, value, true)
    val key = checkNotNull(value.string) { "位图资源 0x${id.toString(16)} 缺少文件路径" }.toString()
    return remember(key) { imageResource(resources, id) }
}
