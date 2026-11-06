package tv.own.owntv.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import tv.own.owntv.R
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.features.epg.GuideProgrammeBlock
import tv.own.owntv.features.epg.GuideVideo
import tv.own.owntv.player.LivePreviewEngine
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.stage.StageButton
import tv.own.owntv.ui.theme.mpx

/**
 * Aura: the Live-TV card at the top of Start. It is the Guide's own top strip (the same video box through
 * Live TV's one preview player, the same programme block), so it looks like the rest of the app and costs
 * no second decoder. OK on Watch opens the channel full screen.
 */
@Composable
fun HomeLiveNow(
    title: String,
    channel: ChannelEntity,
    nowTitle: String?,
    engine: LivePreviewEngine,
    showVideo: Boolean,
    onWatch: () -> Unit,
    onFocus: () -> Unit,
    firstItemFocusRequester: FocusRequester?,
    start: Dp,
    track: (FocusRequester) -> Unit,
    modifier: Modifier = Modifier,
) {
    val watchFocus = firstItemFocusRequester ?: remember { FocusRequester() }
    Column(modifier) {
        HomeRowHeader(title, small = null, start = start)
        Row(
            Modifier.fillMaxWidth().padding(start = start, end = 64.mpx, top = 6.mpx),
            horizontalArrangement = Arrangement.spacedBy(48.mpx),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GuideVideo(
                channel = channel,
                channelLine = channel.name,
                nowTitle = nowTitle,
                previewEngine = engine,
                showVideo = showVideo,
                modifier = Modifier.width(560.mpx),
            )
            Column(Modifier.weight(1f)) {
                GuideProgrammeBlock(
                    eyebrow = stringResource(R.string.content_live_now),
                    title = nowTitle ?: channel.name,
                    details = null,
                    synopsis = null,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.mpx)) {
                    StageButton(
                        stringResource(R.string.content_epg_watch_channel), onClick = onWatch, icon = OwnTVIcon.LIVE_TV,
                        height = 56.mpx, textSize = 19,
                        modifier = Modifier
                            .focusRequester(watchFocus)
                            .tracked(watchFocus, track)
                            .onFocusChanged { if (it.hasFocus) onFocus() },
                    )
                }
            }
        }
    }
}

/** The programme airing on [channel] now, looked up once per channel by [lookup]; null until known or when none. */
@Composable
fun rememberNowTitle(channel: ChannelEntity?, lookup: suspend (ChannelEntity) -> String?): String? {
    val title by produceState<String?>(null, channel?.id) {
        value = null
        value = channel?.let { runCatching { lookup(it) }.getOrNull() }
    }
    return title
}
