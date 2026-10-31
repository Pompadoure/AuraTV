package tv.own.owntv.features.shell

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Where the Live TV preview picture sits in the window right now (null when none is on screen). The
 * full-screen player grows out of this rectangle and shrinks back into it, so "preview" and "full screen"
 * read as one picture changing size instead of a cut.
 */
internal object PreviewBounds {
    @Volatile
    var rect: Rect? = null
}

/** Reports the modified box as the preview rectangle; forgets it when the box leaves the screen. */
internal fun Modifier.reportPreviewBounds(): Modifier = composed {
    DisposableEffect(Unit) { onDispose { PreviewBounds.rect = null } }
    onGloballyPositioned { PreviewBounds.rect = it.boundsInWindow() }
}

/**
 * The full-screen player's frame while it travels between the preview rectangle ([from]) and the whole
 * window. [progress] 0 is the preview, 1 is full screen. Only layout changes per frame (the video surface
 * is resized, never scaled), so it costs one relayout of the player box and nothing else.
 */
@Stable
internal class FullscreenFrame {
    var progress by mutableFloatStateOf(1f)
    var from by mutableStateOf<Rect?>(null)
    /** The player box is following [progress] instead of filling the window. */
    var active by mutableStateOf(false)
    /** Travelling back to the preview: the guide behind fades in meanwhile. */
    var exiting by mutableStateOf(false)
    private var job: Job? = null

    fun animateTo(scope: CoroutineScope, to: Float, millis: Int, then: () -> Unit = {}): Job {
        job?.cancel()
        return scope.launch {
            animate(progress, to, animationSpec = tween(millis, easing = FastOutSlowInEasing)) { v, _ -> progress = v }
            then()
        }.also { job = it }
    }
}

/** Fills the window at progress 1, the [FullscreenFrame.from] rectangle at 0, black behind the picture. */
internal fun Modifier.fullscreenFrame(frame: FullscreenFrame): Modifier = this
    .layout { measurable, constraints ->
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val r = frame.from
        if (r == null) {
            val p = measurable.measure(Constraints.fixed(w, h))
            layout(w, h) { p.place(0, 0) }
        } else {
            val t = frame.progress.coerceIn(0f, 1f)
            val fw = (r.width + (w - r.width) * t).roundToInt().coerceAtLeast(1)
            val fh = (r.height + (h - r.height) * t).roundToInt().coerceAtLeast(1)
            val fx = (r.left * (1f - t)).roundToInt()
            val fy = (r.top * (1f - t)).roundToInt()
            val p = measurable.measure(Constraints.fixed(fw, fh))
            layout(w, h) { p.place(fx, fy) }
        }
    }
    .background(Color.Black)
