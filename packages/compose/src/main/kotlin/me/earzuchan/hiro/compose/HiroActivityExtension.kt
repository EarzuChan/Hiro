package me.earzuchan.hiro.compose

import android.app.Activity
import androidx.compose.runtime.Composable
import me.earzuchan.hiro.compose.savable.HiroSavableStateConfiguration

// CHECK：参数没有组合上下文（浮木）
fun Activity.setHiroComposeContent(content: @Composable () -> Unit): HiroComposeView {
    return setHiroComposeContent(HiroSavableStateConfiguration.DEFAULT, content)
}

fun Activity.setHiroComposeContent(
    savableStateConfiguration: HiroSavableStateConfiguration,
    content: @Composable () -> Unit,
): HiroComposeView {
    // TODO：原有没有顶替复用

    val composeView = HiroComposeView(this, savableStateConfiguration)

    composeView.setContent(content)

    // CHECK：没有设SavedStateOwner？会不会有问题？
    setContentView(composeView)

    return composeView
}
