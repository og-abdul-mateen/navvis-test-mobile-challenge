package com.navvistest.mobilechallenge.ui

import android.os.SystemClock
import android.util.Log

/**
 * Logs how long a session took to reach each milestone, once per milestone.
 * Read with `adb logcat -s StreamTiming`. The README's performance numbers come from here.
 */
internal class SessionTimer(private val name: String) {

    private val startedAt = SystemClock.elapsedRealtime()
    private val reached = mutableSetOf<String>()

    fun mark(milestone: String) {
        if (!reached.add(milestone)) return
        Log.i(TAG, "$name: $milestone after ${SystemClock.elapsedRealtime() - startedAt} ms")
    }

    private companion object {
        const val TAG = "StreamTiming"
    }
}
