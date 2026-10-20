@file:JvmName("StringResources_androidKt")

package androidx.compose.ui.res

import androidx.annotation.ArrayRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import me.earzuchan.hiro.compose.internal.res.currentResources

// TIPS：与 AndroidX 同名同义，读取安卓 R 资源，语言等配置变化时自动重组

@Composable
@ReadOnlyComposable
fun stringResource(@StringRes id: Int): String = currentResources().getString(id)

@Composable
@ReadOnlyComposable
fun stringResource(@StringRes id: Int, vararg formatArgs: Any): String = currentResources().getString(id, *formatArgs)

@Composable
@ReadOnlyComposable
fun stringArrayResource(@ArrayRes id: Int): Array<String> = currentResources().getStringArray(id)

@Composable
@ReadOnlyComposable
fun pluralStringResource(@PluralsRes id: Int, count: Int): String = currentResources().getQuantityString(id, count)

@Composable
@ReadOnlyComposable
fun pluralStringResource(@PluralsRes id: Int, count: Int, vararg formatArgs: Any): String = currentResources().getQuantityString(id, count, *formatArgs)
