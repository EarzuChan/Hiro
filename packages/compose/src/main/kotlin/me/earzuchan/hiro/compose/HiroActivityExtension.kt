package me.earzuchan.hiro.compose

import android.app.Activity
import androidx.compose.runtime.Composable
import me.earzuchan.hiro.compose.savable.HiroSavableStateConfiguration

fun Activity.setHiroComposeContent(content: @Composable () -> Unit) = setHiroComposeContent(HiroComposeConfiguration.DEFAULT, content)

fun Activity.setHiroComposeContent(configuration: HiroComposeConfiguration, content: @Composable () -> Unit): HiroComposeView {
    val composeView = HiroComposeView(this, configuration, ACTIVITY_CONTENT_SAVED_STATE_KEY)
    composeView.id = R.id.hiro_compose_view
    composeView.setContent(content)
    setContentView(composeView)
    return composeView
}

fun Activity.setHiroComposeContent(savableStateConfiguration: HiroSavableStateConfiguration, content: @Composable () -> Unit) = setHiroComposeContent(
    configuration = hiroComposeConfiguration { savableState(savableStateConfiguration) },
    content = content,
)

private const val ACTIVITY_CONTENT_SAVED_STATE_KEY = "activity-content"
