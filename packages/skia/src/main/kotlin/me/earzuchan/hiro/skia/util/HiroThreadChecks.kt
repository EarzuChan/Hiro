package me.earzuchan.hiro.skia.util

import android.os.Looper

internal fun checkHiroMainThread() = check(Looper.myLooper() == Looper.getMainLooper()) { "Hiro Skia 宿主操作只能在安卓主线程执行" }
