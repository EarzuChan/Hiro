package me.earzuchan.hiro.example.configuration

import android.os.Bundle
import android.os.Looper
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import me.earzuchan.hiro.compose.HiroComposeConfiguration
import me.earzuchan.hiro.compose.HiroComposeView

class ConfigurationActivity : ComponentActivity() {
    private lateinit var container: LinearLayout
    private var previewView: HiroComposeView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        container.addView(HiroComposeView(this, HiroComposeConfiguration.DEFAULT, "configuration-control").apply {
            setContent { ConfigurationControlPanel(::requestPreviewConfiguration) } // 在这里面构建啊一个配置
        }, weightedLayoutParams(1.35f))

        rebuildPreview(ConfigurationState())

        setContentView(container)
    }

    override fun onDestroy() {
        previewView?.close()
        super.onDestroy()
    }

    private fun requestPreviewConfiguration(state: ConfigurationState) = if (Looper.myLooper() == Looper.getMainLooper()) rebuildPreview(state)
    else runOnUiThread { rebuildPreview(state) }

    private fun rebuildPreview(state: ConfigurationState) {
        if (isFinishing || isDestroyed) return

        previewView?.let {
            it.close()
            container.removeView(it)
        }

        previewView = createPreview(state)
        container.addView(checkNotNull(previewView), weightedLayoutParams(1f))
    }

    private fun createPreview(state: ConfigurationState): HiroComposeView = HiroComposeView(this, state.toHiroConfiguration(), "configuration-preview").also { it.setContent { ConfigurationPreview() } }

    private fun weightedLayoutParams(weight: Float) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, weight)
}
