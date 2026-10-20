package me.earzuchan.hiro.compose.internal.res

import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.util.TypedValue
import android.util.Xml
import androidx.compose.ui.graphics.vector.ImageVector
import java.lang.ref.WeakReference
import me.earzuchan.hiro.compose.internal.res.vector.AndroidVectorParser
import me.earzuchan.hiro.compose.internal.res.vector.createVectorImageBuilder
import me.earzuchan.hiro.compose.internal.res.vector.isAtEnd
import me.earzuchan.hiro.compose.internal.res.vector.parseCurrentVectorNode
import me.earzuchan.hiro.compose.internal.res.vector.seekToStartTag

// TIPS：按主题与资源 ID 缓存解析结果，配置变化时只在资源确实依赖该配置项时才重新解析，规则与 AndroidX ImageVectorCache 一致
internal object HiroVectorResourceLoader {
    private data class Key(val theme: Resources.Theme?, val id: Int)

    private class Entry(val imageVector: ImageVector, val configFlags: Int, val configuration: Configuration)

    private val cache = HashMap<Key, WeakReference<Entry>>()

    fun load(res: Resources, theme: Resources.Theme?, id: Int): ImageVector = synchronized(cache) {
        val key = Key(theme, id)
        val current = res.configuration

        cache[key]?.get()?.takeIf { !Configuration.needNewResources(current.diff(it.configuration), it.configFlags) }?.let { return it.imageVector }

        val value = TypedValue()
        res.getValue(id, value, true)
        val (imageVector, configFlags) = parse(res, theme, res.getXml(id).apply { seekToStartTag() }, value.changingConfigurations)

        cache[key] = WeakReference(Entry(imageVector, configFlags, Configuration(current)))
        imageVector
    }

    fun parse(res: Resources, theme: Resources.Theme?, parser: XmlResourceParser, changingConfigurations: Int): Pair<ImageVector, Int> {
        require(parser.name == "vector") { "${parser.positionDescription}：根标签须为 <vector>，实际为 <${parser.name}>" }

        val attrs = Xml.asAttributeSet(parser)
        val resourceParser = AndroidVectorParser(parser)
        val builder = resourceParser.createVectorImageBuilder(res, theme, attrs)

        var nestedGroups = 0
        while (!parser.isAtEnd()) {
            nestedGroups = resourceParser.parseCurrentVectorNode(res, attrs, theme, builder, nestedGroups)
            parser.next()
        }

        return builder.build() to (changingConfigurations or resourceParser.config)
    }
}
