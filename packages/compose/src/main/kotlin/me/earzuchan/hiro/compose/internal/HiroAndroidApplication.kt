package me.earzuchan.hiro.compose.internal

import android.content.Context

// TIPS：供组合之外、拿不到 LocalContext 的场合使用，如字体加载与 Compose 资源的非组合读取
internal object HiroAndroidApplication {
    @Volatile
    private var applicationContext: Context? = null

    val context: Context get() = checkNotNull(applicationContext) { "Hiro 尚未取得安卓 Application Context，请先创建 HiroComposeView" }

    fun attach(context: Context) {
        if (applicationContext == null) applicationContext = context.applicationContext
    }
}
