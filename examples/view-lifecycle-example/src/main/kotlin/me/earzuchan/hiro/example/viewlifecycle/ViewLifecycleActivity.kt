package me.earzuchan.hiro.example.viewlifecycle

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import me.earzuchan.hiro.compose.HiroComposeConfiguration
import me.earzuchan.hiro.compose.HiroComposeView
import java.util.*
import android.graphics.Color as AndroidColor

class ViewLifecycleActivity : ComponentActivity() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var welten: LinearLayout
    private val reattachingViews = mutableSetOf<HiroComposeView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        welten = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(AndroidColor.BLACK)
        }

        val worldA = createWorld("阿尔法世界线", "weltlinie-a", Color(0xFF173B57))
        val worldB = createWorld("贝塔世界线", "weltlinie-b", Color(0xFF3B244F))
        welten.addView(worldA, weightedLayoutParams())
        welten.addView(worldB, weightedLayoutParams())
        setContentView(welten)
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun createWorld(title: String, savedStateKey: String, background: Color): HiroComposeView {
        val view = HiroComposeView(this, HiroComposeConfiguration.DEFAULT, savedStateKey)
        view.setContent { WeltlinieContent(title = title, background = background, onDetachAndReattach = { detachAndReattach(view) }) }
        return view
    }

    private fun detachAndReattach(view: HiroComposeView) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { detachAndReattach(view) }
            return
        }

        if (!reattachingViews.add(view)) return

        val index = welten.indexOfChild(view).coerceAtLeast(0)
        welten.removeView(view)

        mainHandler.postDelayed({
            reattachingViews.remove(view)
            if (!isFinishing && !isDestroyed) welten.addView(view, index.coerceAtMost(welten.childCount), weightedLayoutParams())
        }, 550L)
    }

    private fun weightedLayoutParams() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
}

@Composable
private fun WeltlinieContent(title: String, background: Color, onDetachAndReattach: () -> Unit) = MaterialTheme {
    Surface(Modifier.fillMaxSize(), color = background, contentColor = Color.White) {
        Column(Modifier.fillMaxSize().safeContentPadding().padding(18.dp), Arrangement.spacedBy(8.dp)) {
            val weltlinieViewModel = viewModel<WeltlinieViewModel>()
            var rememberCount by remember { mutableStateOf(0) }
            var savedCount by rememberSaveable { mutableStateOf(0) }
            
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text("同一个 Activity 里的不同 Hiro 世界线")
            Text("ViewModel：${weltlinieViewModel.instanceId}，计数 ${weltlinieViewModel.count}")
            Text("remember：$rememberCount")
            Text("rememberSaveable：$savedCount")

            Button(onClick = { weltlinieViewModel.increment() }) { Text("ViewModel 里增加") }
            Button(onClick = { rememberCount++ }) { Text("Remember 里增加") }
            Button(onClick = { savedCount++ }) { Text("RememberSaveable 里增加") }
            Button(onClick = onDetachAndReattach) { Text("移除并重新挂载这个 View") }
        }
    }
}

internal class WeltlinieViewModel : ViewModel() {
    val instanceId = UUID.randomUUID().toString().take(8)

    var count by mutableStateOf(0)
        private set

    fun increment() {
        count++
    }
}
