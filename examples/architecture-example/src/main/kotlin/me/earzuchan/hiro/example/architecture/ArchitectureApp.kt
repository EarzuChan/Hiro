package me.earzuchan.hiro.example.architecture

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
private data object Home : NavKey

@Serializable
private data class Detail(val serial: Int) : NavKey

@Composable
internal fun ArchitectureApp(activityIdentity: ViewModelIdentity, onRecreateActivity: () -> Unit) {
    val rootViewModel = koinViewModel<ArchitectureViewModel>()
    val rootCount by rootViewModel.count.collectAsStateWithLifecycle()
    val rootLifecycleOwner = LocalLifecycleOwner.current
    val rootLifecycleState by rootLifecycleOwner.lifecycle.currentStateAsState()
    val childLifecycleOwner = rememberLifecycleOwner(maxLifecycle = Lifecycle.State.STARTED)
    val rootStoreOwner = checkNotNull(LocalViewModelStoreOwner.current)
    val rootSavedStateOwner = LocalSavedStateRegistryOwner.current
    val backStack = rememberNavBackStack(Home)
    val popBackStack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
    var savableCounter by rememberSaveable { mutableIntStateOf(0) }

    check(rootViewModel.instanceId != activityIdentity.instanceId) { "Activity 与 Hiro 根节点错误地共享了 ViewModel" }

    LaunchedEffect(rootViewModel) {
        check(Thread.currentThread().name != "main") { "Hiro Compose 根内容错误地运行在 Android 主线程" }
        Log.i(
            ArchitectureViewModel.TAG,
            "HIRO_ROOT vm=${rootViewModel.instanceId} thread=${Thread.currentThread().name} store=${System.identityHashCode(rootStoreOwner)} savedState=${System.identityHashCode(rootSavedStateOwner)}",
        )
    }

    DisposableEffect(rootLifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event -> Log.i(ArchitectureViewModel.TAG, "HIRO_LIFECYCLE event=$event thread=${Thread.currentThread().name}") }
        rootLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { rootLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF111827)).padding(horizontal = 18.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StatusText("Android VM ${activityIdentity.instanceId} / ${activityIdentity.creationThread} / ${activityIdentity.count}", Color(0xFFFBBF24))
        StatusText("Hiro 根 VM ${rootViewModel.instanceId} / ${rootViewModel.creationThread} / $rootCount", Color(0xFF67E8F9))
        StatusText("根 Lifecycle $rootLifecycleState / 子 Lifecycle ${childLifecycleOwner.lifecycle.currentState}", Color(0xFFA7F3D0))
        StatusText("返回栈 ${backStack.size} / Saveable $savableCounter", Color.White)

        ActionButton("增加根 VM 与 Saveable", "增加根状态") {
            rootViewModel.increment()
            savableCounter++
        }

        ActionButton("重建 Activity 验证恢复", "重建 Activity", onRecreateActivity)

        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxWidth().weight(1f),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            onBack = popBackStack,
            entryProvider = entryProvider({ NavEntry(it) { StatusText("未知页面：$it", Color.Red) } }) {
                entry<Home> {
                    ArchitecturePage(
                        title = "首页",
                        activityViewModelId = activityIdentity.instanceId,
                        rootViewModelId = rootViewModel.instanceId,
                        onNavigate = { backStack.add(Detail(backStack.size)) },
                    )
                }
                entry<Detail> { key ->
                    ArchitecturePage(
                        title = "详情 ${key.serial}",
                        activityViewModelId = activityIdentity.instanceId,
                        rootViewModelId = rootViewModel.instanceId,
                        onBack = popBackStack,
                    )
                }
            },
        )
    }
}

@Composable
private fun ArchitecturePage(title: String, activityViewModelId: String, rootViewModelId: String, onNavigate: (() -> Unit)? = null, onBack: (() -> Unit)? = null) {
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
        ActionButton("增加页面 VM 计数", "增加页面状态", viewModel::increment)
        onNavigate?.let { ActionButton("进入详情页", "进入详情页", it) }
        onBack?.let { ActionButton("返回并清理详情 VM", "返回首页", it) }
    }
}

@Composable
private fun ActionButton(label: String, description: String, onClick: () -> Unit) = Box(Modifier.fillMaxWidth().background(Color(0xFF0F766E)).semantics { contentDescription = description }.clickable(onClick = onClick).padding(16.dp, 13.dp), Alignment.Center) {
    BasicText(label, style = TextStyle(color = Color.White, fontSize = 16.sp))
}

@Composable
private fun StatusText(text: String, color: Color, size: Int = 14) = Row(Modifier.fillMaxWidth()) { BasicText(text, style = TextStyle(color = color, fontSize = size.sp)) }
