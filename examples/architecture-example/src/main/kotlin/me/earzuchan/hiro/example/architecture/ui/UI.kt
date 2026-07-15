package me.earzuchan.hiro.example.architecture.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import me.earzuchan.hiro.example.architecture.viewmodel.ArchitectureViewModel
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun ArchitecturePage(title: String, activityViewModelId: String, rootViewModelId: String, onNavigate: (() -> Unit)? = null, onBack: (() -> Unit)? = null) {
    val viewModel = koinViewModel<ArchitectureViewModel>()
    val count by viewModel.count.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
    val storeOwner = checkNotNull(LocalViewModelStoreOwner.current)

    check(viewModel.instanceId != activityViewModelId) { "Activity 与 Navigation 页面错误地共享了 ViewModel" }
    check(viewModel.instanceId != rootViewModelId) { "Hiro 根节点与 Navigation 页面错误地共享了 ViewModel" }

    LaunchedEffect(viewModel) {
        check(Thread.currentThread().name != "main") { "Navigation 页面错误地运行在 Android 主线程" }
        Log.i(ArchitectureViewModel.TAG, "PAGE_VM title=$title id=${viewModel.instanceId} thread=${Thread.currentThread().name} store=${System.identityHashCode(storeOwner)}")
    }

    DisposableEffect(viewModel) {
        onDispose { Log.i(ArchitectureViewModel.TAG, "PAGE_DISPOSE title=$title id=${viewModel.instanceId} thread=${Thread.currentThread().name}") }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF1F2937)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatusText("$title / Lifecycle $lifecycleState", Color(0xFFF9FAFB), 22)
        StatusText("页面 VM ${viewModel.instanceId} / ${viewModel.creationThread}", Color(0xFFC4B5FD))
        StatusText("SavedStateHandle 计数 $count", Color(0xFF86EFAC))
        ActionButton("向页面 VM 计数增值", viewModel::increment)
        onNavigate?.let { ActionButton("进入详情页", it) }
        onBack?.let { ActionButton("返回并清理详情 VM", it) }
    }
}

@Composable
fun ActionButton(label: String, onClick: () -> Unit) = Box(Modifier.fillMaxWidth().background(Color(0xFF0F766E)).clickable(onClick = onClick).padding(16.dp, 13.dp), Alignment.Center) {
    BasicText(label, style = TextStyle(color = Color.White, fontSize = 16.sp))
}

@Composable
fun StatusText(text: String, color: Color, size: Int = 14) = Row(Modifier.fillMaxWidth()) { BasicText(text, style = TextStyle(color = color, fontSize = size.sp)) }