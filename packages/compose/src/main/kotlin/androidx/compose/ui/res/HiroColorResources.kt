@file:JvmName("ColorResources_androidKt")

package androidx.compose.ui.res

import androidx.annotation.ColorRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat
import me.earzuchan.hiro.compose.internal.res.currentResources

@Composable
@ReadOnlyComposable
fun colorResource(@ColorRes id: Int): Color = Color(ResourcesCompat.getColor(currentResources(), id, LocalContext.current.theme))
