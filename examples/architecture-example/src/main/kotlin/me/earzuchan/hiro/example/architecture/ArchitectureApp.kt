package me.earzuchan.hiro.example.architecture

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import me.earzuchan.hiro.compose.hiroComposeConfiguration
import me.earzuchan.hiro.compose.savable.hiroSavableStateSeDes
import me.earzuchan.hiro.compose.setHiroComposeContent
import me.earzuchan.hiro.example.architecture.navigation.Detail
import me.earzuchan.hiro.example.architecture.navigation.Home
import me.earzuchan.hiro.example.architecture.ui.ActionButton
import me.earzuchan.hiro.example.architecture.ui.ArchitecturePage
import me.earzuchan.hiro.example.architecture.ui.StatusText
import me.earzuchan.hiro.example.architecture.viewmodel.ArchitectureViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.viewmodel.koinViewModel
import java.util.concurrent.atomic.AtomicBoolean

internal data class ViewModelIdentity(val instanceId: String, val creationThread: String, val count: Int)

data class ThirdPartyCounter(val count: Int)

class ArchitectureActivity : ComponentActivity() {
    private val activityViewModel by viewModel<ArchitectureViewModel>()
    private val recreationRequested = AtomicBoolean()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        activityViewModel.increment()
        val activityIdentity = ViewModelIdentity(instanceId = activityViewModel.instanceId, creationThread = activityViewModel.creationThread, count = activityViewModel.count.value)

        Log.i(ArchitectureViewModel.TAG, "ACTIVITY_VM id=${activityIdentity.instanceId} thread=${activityIdentity.creationThread} count=${activityIdentity.count}")

        setHiroComposeContent(hiroComposeConfiguration {
            savableState {
                val sedesKey = "count"

                addSeDes(
                    hiroSavableStateSeDes<ThirdPartyCounter>(
                        serialize = { counter -> Bundle().apply { putInt(sedesKey, counter.count) } },
                        deserialize = { state -> ThirdPartyCounter(state.getInt(sedesKey)) }
                    )
                )
            }
        }) { ArchitectureApp(activityIdentity = activityIdentity, onRecreateActivity = ::requestRecreation) }
    }

    override fun onDestroy() {
        Log.i(ArchitectureViewModel.TAG, "ACTIVITY_DESTROY changingConfigurations=$isChangingConfigurations thread=${Thread.currentThread().name}")

        super.onDestroy()
    }

    private fun requestRecreation() {
        if (recreationRequested.compareAndSet(false, true)) runOnUiThread(::recreate)
    }
}

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

        Log.i(ArchitectureViewModel.TAG, "HIRO_ROOT vm=${rootViewModel.instanceId} thread=${Thread.currentThread().name} store=${System.identityHashCode(rootStoreOwner)} savedState=${System.identityHashCode(rootSavedStateOwner)}")
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

        ActionButton("向根 VM 与 Saveable 增值") {
            rootViewModel.increment()
            savableCounter++
        }

        ActionButton("重建 Activity 验证恢复", onRecreateActivity)

        SavableStateChecks()

        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxWidth().weight(1f),
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()), // CHECK：什么玩意
            onBack = popBackStack,
            entryProvider = entryProvider({ NavEntry(it) { page -> StatusText("未知页面：$page", Color.Red) } }) {
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
private fun SavableStateChecks() {
    var normalCounter by rememberSaveable { mutableIntStateOf(0) }
    var ultimateCounter by rememberSaveable { mutableStateOf(ThirdPartyCounter(0)) }

    LaunchedEffect(normalCounter, ultimateCounter) { Log.i(ArchitectureViewModel.TAG, "HIRO_SEDES normal=$normalCounter ultimate=${ultimateCounter.count} thread=${Thread.currentThread().name}") }

    StatusText("普通 $normalCounter / 特制 SeDes ${ultimateCounter.count}", Color(0xFFF0ABFC))

    ActionButton("向两类 SeDes 增值") {
        normalCounter++
        ultimateCounter = ThirdPartyCounter(ultimateCounter.count + 1)
    }
}
