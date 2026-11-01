package tv.own.owntv.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/** How long a channel may take to start before a loading spinner is worth showing at all. */
const val SPINNER_DELAY_MS = 6_000L

/** True only once [active] has held continuously for [delayMs]; drops back to false the moment it ends. */
@Composable
fun rememberDelayedFlag(active: Boolean, delayMs: Long = SPINNER_DELAY_MS): Boolean {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(active) {
        shown = false
        if (active) { delay(delayMs); shown = true }
    }
    return shown && active
}
