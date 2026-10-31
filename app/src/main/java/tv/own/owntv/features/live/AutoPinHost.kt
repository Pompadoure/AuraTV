package tv.own.owntv.features.live

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.player.LiveTuneController
import tv.own.owntv.player.PlayerFailureReason

/**
 * Wraps core's host to teach the app one lesson: a channel whose audio ExoPlayer cannot decode (Eurosport's
 * codec, say) is pinned to mpv after its first handover. Without it every zap to such a channel opens on
 * ExoPlayer, waits for the track list, tears the video surface down and reloads the stream on mpv, so the
 * picture changes twice and takes seconds longer. The pin is core's own per-channel "compatibility mode",
 * so it shows in the player menu and can be cleared there or with the settings reset.
 */
internal class AutoPinHost(
    private val inner: LiveTuneController.CoreHost,
    private val scope: CoroutineScope,
) : LiveTuneController.Host by inner {

    @Volatile
    private var lastTuned: ChannelEntity? = null

    // Asked once per tune with the channel being tuned, which is how a later handover finds its channel.
    override suspend fun enginePin(channel: ChannelEntity): Boolean? {
        lastTuned = channel
        return inner.enginePin(channel)
    }

    override fun recordLadderEvent(onExo: Boolean, reason: PlayerFailureReason, detail: String) {
        inner.recordLadderEvent(onExo, reason, detail)
        if (reason != PlayerFailureReason.LIVE_FALLBACK || !detail.contains(NO_AUDIO)) return
        val channel = lastTuned ?: return
        // A newer zap may have replaced the channel by now; the detail names the one that fell back.
        if (!detail.startsWith("'${channel.name}'")) return
        scope.launch { inner.pin(channel, onMpv = true) }
    }

    private companion object {
        /** The reason text core's ExoPlayer watchdog hands over with. */
        const val NO_AUDIO = "no decodable audio track"
    }
}
