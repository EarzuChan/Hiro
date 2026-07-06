package me.earzuchan.hiro.compose

import android.app.Activity
import androidx.compose.runtime.Composable

fun Activity.setHiroComposeContent(content: @Composable () -> Unit): HiroComposeView {
    val view = HiroComposeView(this)

    view.setContent(content)
    setContentView(view)

    return view
}