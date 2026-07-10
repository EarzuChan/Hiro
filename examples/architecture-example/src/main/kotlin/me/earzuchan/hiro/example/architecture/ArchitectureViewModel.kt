package me.earzuchan.hiro.example.architecture

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import java.util.UUID

class ArchitectureViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    val instanceId: String = UUID.randomUUID().toString().take(8)
    val creationThread: String = Thread.currentThread().name
    val count = savedStateHandle.getStateFlow(COUNT_KEY, 0)

    init {
        Log.i(TAG, "VM_CREATED id=$instanceId thread=$creationThread")
    }

    fun increment() {
        savedStateHandle[COUNT_KEY] = count.value + 1
    }

    override fun onCleared() {
        Log.i(TAG, "VM_CLEARED id=$instanceId thread=${Thread.currentThread().name} count=${count.value}")
    }

    companion object {
        const val TAG = "HiroArchExample"
        private const val COUNT_KEY = "count"
    }
}
