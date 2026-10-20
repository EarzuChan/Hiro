@file:JvmName("PrimitiveResources_androidKt")

package androidx.compose.ui.res

import androidx.annotation.ArrayRes
import androidx.annotation.BoolRes
import androidx.annotation.DimenRes
import androidx.annotation.IntegerRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import me.earzuchan.hiro.compose.internal.res.currentResources

@Composable
@ReadOnlyComposable
fun integerResource(@IntegerRes id: Int): Int = currentResources().getInteger(id)

@Composable
@ReadOnlyComposable
fun integerArrayResource(@ArrayRes id: Int): IntArray = currentResources().getIntArray(id)

@Composable
@ReadOnlyComposable
fun booleanResource(@BoolRes id: Int): Boolean = currentResources().getBoolean(id)

// TIPS：按安卓系统密度换回资源中声明的 Dp，即使 Hiro 场景使用了自定义密度策略也保持原值
@Composable
@ReadOnlyComposable
fun dimensionResource(@DimenRes id: Int): Dp {
    val resources = currentResources()
    return Dp(resources.getDimension(id) / resources.displayMetrics.density)
}
