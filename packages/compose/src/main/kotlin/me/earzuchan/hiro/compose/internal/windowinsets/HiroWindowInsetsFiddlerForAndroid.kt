package me.earzuchan.hiro.compose.internal.windowinsets

import android.graphics.Rect as AndroidRect
import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.core.graphics.Insets as AndroidXInsets
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat

internal class HiroWindowInsetsFiddlerForAndroid(private val boundTo: HiroMutablePlatformWindowInsets, private val changeWatcher: () -> Unit) {
    private var hostView: View? = null

    private val attachStateListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) = requestApplyInsets(view)

        override fun onViewDetachedFromWindow(view: View) {}
    }

    private val insetsListener = OnApplyWindowInsetsListener { _, insets ->
        applyInsets(insets)
        insets
    }

    private val animationCallback = object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
        override fun onProgress(insets: WindowInsetsCompat, runningAnimations: MutableList<WindowInsetsAnimationCompat>): WindowInsetsCompat {
            applyInsets(insets)
            return insets
        }
    }

    fun attach(view: View) {
        if (hostView === view) {
            requestApplyInsets(view)
            return
        }

        close()

        hostView = view
        view.addOnAttachStateChangeListener(attachStateListener)
        ViewCompat.setOnApplyWindowInsetsListener(view, insetsListener)
        ViewCompat.setWindowInsetsAnimationCallback(view, animationCallback)
        readCurrentInsets(view)
        requestApplyInsets(view)
    }

    fun detach(view: View) {
        if (hostView !== view) return

        close()
    }

    fun requestApplyInsets() {
        hostView?.let(::requestApplyInsets)
    }

    fun close() {
        val view = hostView ?: return

        ViewCompat.setWindowInsetsAnimationCallback(view, null)
        ViewCompat.setOnApplyWindowInsetsListener(view, null)
        view.removeOnAttachStateChangeListener(attachStateListener)
        hostView = null
    }

    private fun requestApplyInsets(view: View) {
        if (view.isAttachedToWindow) ViewCompat.requestApplyInsets(view)
    }

    private fun readCurrentInsets(view: View) = ViewCompat.getRootWindowInsets(view)?.let(::applyInsets)

    private fun applyInsets(insets: WindowInsetsCompat) {
        if (boundTo.update(insets.toHiroSnapshot())) changeWatcher()
    }
}

private fun WindowInsetsCompat.toHiroSnapshot() = HiroPlatformWindowInsetsSnapshot(
    captionBar = getInsets(WindowInsetsCompat.Type.captionBar()).toHiroInsetsValues(),
    displayCutout = getInsets(WindowInsetsCompat.Type.displayCutout()).toHiroInsetsValues(),
    ime = getInsets(WindowInsetsCompat.Type.ime()).toHiroInsetsValues(),
    mandatorySystemGestures = getInsets(WindowInsetsCompat.Type.mandatorySystemGestures()).toHiroInsetsValues(),
    navigationBars = getInsets(WindowInsetsCompat.Type.navigationBars()).toHiroInsetsValues(),
    statusBars = getInsets(WindowInsetsCompat.Type.statusBars()).toHiroInsetsValues(),
    systemBars = getInsets(WindowInsetsCompat.Type.systemBars()).toHiroInsetsValues(),
    systemGestures = getInsets(WindowInsetsCompat.Type.systemGestures()).toHiroInsetsValues(),
    tappableElement = getInsets(WindowInsetsCompat.Type.tappableElement()).toHiroInsetsValues(),
    waterfall = displayCutout?.waterfallInsets?.toHiroInsetsValues() ?: HiroInsetsValues.Zero,
    displayCutouts = displayCutout?.boundingRects.orEmpty().map(AndroidRect::toComposeRect),
)

private fun AndroidXInsets.toHiroInsetsValues() = HiroInsetsValues(left, top, right, bottom)

private fun AndroidRect.toComposeRect() = Rect(
    left = left.toFloat(),
    top = top.toFloat(),
    right = right.toFloat(),
    bottom = bottom.toFloat(),
)
