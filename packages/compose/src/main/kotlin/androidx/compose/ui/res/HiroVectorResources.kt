@file:JvmName("VectorResources_androidKt")

package androidx.compose.ui.res

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import me.earzuchan.hiro.compose.internal.res.HiroVectorResourceLoader
import me.earzuchan.hiro.compose.internal.res.currentResources

/** 解析安卓 VectorDrawable 资源为 ImageVector，渐变填充与描边同样支持 */
@Composable
fun ImageVector.Companion.vectorResource(@DrawableRes id: Int): ImageVector {
    val res = currentResources()
    val theme = LocalContext.current.theme
    return remember(id, res, theme, LocalConfiguration.current) { vectorResource(theme, res, id) }
}

fun ImageVector.Companion.vectorResource(theme: Resources.Theme? = null, res: Resources, resId: Int): ImageVector = HiroVectorResourceLoader.load(res, theme, resId)
