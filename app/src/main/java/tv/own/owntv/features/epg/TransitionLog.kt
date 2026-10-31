package tv.own.owntv.features.epg

import android.os.SystemClock
import android.util.Log

/**
 * Timestamps for the fullscreen -> guide switch, written to Logcat (tag OwnTV-Transition) so a slow
 * switch can be attributed to a step from a device log instead of guessed at.
 */
internal object TransitionLog {
    private const val TAG = "OwnTV-Transition"
    @Volatile private var startedAt = 0L

    fun begin(step: String) {
        startedAt = SystemClock.elapsedRealtime()
        Log.i(TAG, "$step +0ms")
    }

    fun mark(step: String) {
        val t0 = startedAt
        if (t0 == 0L) return
        Log.i(TAG, "$step +${SystemClock.elapsedRealtime() - t0}ms")
    }
}
