@file:JvmName("AndroidFont_androidKt")

package androidx.compose.ui.text.font

import android.content.res.AssetManager
import androidx.compose.ui.text.platform.Font as SkiaFont

/** 从安卓 Assets 加载字体文件，与 AndroidX 同名同义 */
fun Font(path: String, assetManager: AssetManager, weight: FontWeight = FontWeight.Normal, style: FontStyle = FontStyle.Normal, variationSettings: FontVariation.Settings = FontVariation.Settings(weight, style)): Font = SkiaFont(identity = "android-asset:$path", getData = { assetManager.open(path).use { it.readBytes() } }, weight = weight, style = style, variationSettings = variationSettings)
