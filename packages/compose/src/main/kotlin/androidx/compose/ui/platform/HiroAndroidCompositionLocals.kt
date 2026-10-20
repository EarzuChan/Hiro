@file:JvmName("AndroidCompositionLocals_androidKt")

package androidx.compose.ui.platform

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

// TIPS：与 AndroidX 同名同义，由 HiroComposeView 所在的安卓 Context 提供。组合跑在 Hiro 渲染线程，这里的 Context 只宜用于读取资源等线程安全的操作

/** 宿主 HiroComposeView 的 Context，通常是 Activity */
val LocalContext = staticCompositionLocalOf<Context> { noLocalProvidedFor("LocalContext") }

/** 当前生效的安卓 Configuration，配置变化（语言、深色模式等）时随之更新 */
val LocalConfiguration = compositionLocalOf<Configuration> { noLocalProvidedFor("LocalConfiguration") }

/** 当前 Context 的 Resources */
val LocalResources = compositionLocalOf<Resources> { noLocalProvidedFor("LocalResources") }

private fun noLocalProvidedFor(name: String): Nothing = error("$name 未提供，它只在 Hiro Compose 场景中可用")
