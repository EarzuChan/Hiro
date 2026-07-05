package me.earzuchan.hiro.samples.fullscreen

import android.app.Activity
import android.os.Bundle
import android.view.Choreographer
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import me.earzuchan.hiro.skiko.HiroSkiaLayer
import me.earzuchan.hiro.skiko.HiroSkiaRenderDelegate
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class P0SkiaActivity : Activity() {
    private val layer = HiroSkiaLayer()
    private var running = false
    private var startNanos = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            layer.needRender()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )

        val root = FrameLayout(this)
        setContentView(root)

        layer.renderDelegate = HiroSkiaRenderDelegate { canvas, width, height, nanoTime ->
            if (startNanos == 0L) startNanos = nanoTime
            val seconds = (nanoTime - startNanos) / 1_000_000_000f
            val side = min(width, height).toFloat()
            val radius = side * 0.16f
            val cx = width * 0.5f + cos(seconds * 1.7f) * side * 0.22f
            val cy = height * 0.5f + sin(seconds * 1.3f) * side * 0.22f

            val paint = Paint()
            try {
                canvas.clear(0xFF10131A.toInt())

                paint.color = 0xFF1F2937.toInt()
                canvas.drawRect(Rect.makeWH(width.toFloat(), height.toFloat()), paint)

                paint.color = 0xFF38BDF8.toInt()
                canvas.drawCircle(cx, cy, radius, paint)

                paint.color = 0xFFE879F9.toInt()
                canvas.drawCircle(width - cx, height - cy, radius * 0.72f, paint)

                paint.color = 0x99FFFFFF.toInt()
                canvas.drawRect(
                    Rect.makeXYWH(width * 0.12f, height * 0.12f, width * 0.76f, height * 0.08f),
                    paint,
                )
            } finally {
                paint.close()
            }
        }

        layer.attachTo(root).contentDescription = "Hiro P0.1 Skia 画布"
    }

    override fun onResume() {
        super.onResume()
        running = true
        layer.onHostResume()
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    override fun onPause() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        layer.onHostPause()
        super.onPause()
    }

    override fun onDestroy() {
        layer.close()
        super.onDestroy()
    }
}
