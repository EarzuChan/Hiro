package me.earzuchan.hiro.example.architecture

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import me.earzuchan.hiro.compose.setHiroComposeContent
import me.earzuchan.hiro.example.architecture.viewmodel.ArchitectureViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.atomic.AtomicBoolean

class ArchitectureActivity : ComponentActivity() {
    private val activityViewModel by viewModel<ArchitectureViewModel>()
    private val recreationRequested = AtomicBoolean()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        activityViewModel.increment()
        val activityIdentity = ViewModelIdentity(instanceId = activityViewModel.instanceId, creationThread = activityViewModel.creationThread, count = activityViewModel.count.value)

        Log.i(ArchitectureViewModel.TAG, "ACTIVITY_VM id=${activityIdentity.instanceId} thread=${activityIdentity.creationThread} count=${activityIdentity.count}")

        setHiroComposeContent { ArchitectureApp(activityIdentity = activityIdentity, onRecreateActivity = ::requestRecreation) }
    }

    override fun onDestroy() {
        Log.i(ArchitectureViewModel.TAG, "ACTIVITY_DESTROY changingConfigurations=$isChangingConfigurations thread=${Thread.currentThread().name}")

        super.onDestroy()
    }

    private fun requestRecreation() {
        if (recreationRequested.compareAndSet(false, true)) runOnUiThread(::recreate)
    }
}

internal data class ViewModelIdentity(val instanceId: String, val creationThread: String, val count: Int)