@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package androidx.compose.ui.text.font

import androidx.compose.ui.text.font.FontLoadingStrategy.Companion.Async
import androidx.compose.ui.text.font.FontLoadingStrategy.Companion.Blocking
import androidx.compose.ui.text.font.FontLoadingStrategy.Companion.OptionalLocal
import androidx.compose.ui.text.platform.FontCache
import androidx.compose.ui.text.platform.FontLoadResult
import androidx.compose.ui.text.platform.PlatformFont
import me.earzuchan.hiro.compose.internal.res.HiroFontResources
import org.jetbrains.skia.paragraph.FontCollection

// TIPS：顶替上游同名类，唯一差别是先把通用的 ResourceFont（安卓 R.font）转换为 Skia 可加载的字体，其余行为保持一致
internal class SkiaFontLoader(fontCacheProvider: () -> FontCache) : PlatformFontLoader {
    constructor(fontCache: FontCache = FontCache()) : this(fontCacheProvider = { fontCache })

    private val fontCache: FontCache by lazy(fontCacheProvider)

    val fontCollection: FontCollection
        get() = fontCache.fonts

    override fun loadBlocking(font: Font): FontLoadResult? {
        val platformFont = if (font is ResourceFont) HiroFontResources.toPlatformFont(font) else font

        if (platformFont !is PlatformFont) {
            if (platformFont.loadingStrategy != OptionalLocal) throw IllegalArgumentException("Unsupported font type: $platformFont")
            return null
        }

        // TIPS：安卓资源字体本地可读，异步策略也按同步加载处理
        val strategy = if (font is ResourceFont && font.loadingStrategy == Async) Blocking else platformFont.loadingStrategy
        return when (strategy) {
            Blocking -> fontCache.load(platformFont)
            OptionalLocal -> runCatching { fontCache.load(platformFont) }.getOrNull()
            Async -> throw UnsupportedOperationException("Unsupported Async font load path")
            else -> throw IllegalArgumentException("Unknown loading type ${platformFont.loadingStrategy}")
        }
    }

    // TIPS：上游 ui-text 按其模块名修饰后的方法名调用，这里须保持一致
    @JvmName("loadPlatformTypes-RetOiIg\$ui_text")
    internal fun loadPlatformTypes(fontFamily: FontFamily, fontWeight: FontWeight = FontWeight.Normal, fontStyle: FontStyle = FontStyle.Normal): FontLoadResult = fontCache.loadPlatformTypes(fontFamily, fontWeight, fontStyle)

    override suspend fun awaitLoad(font: Font): FontLoadResult? = loadBlocking(font)

    override val cacheKey: Any
        get() = fontCache
}
