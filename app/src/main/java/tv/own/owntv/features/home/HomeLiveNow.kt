package tv.own.owntv.features.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import tv.own.owntv.R
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.home.GuideSliceState
import tv.own.owntv.core.parser.XtEpgEntry
import tv.own.owntv.features.shell.reportPreviewBounds
import tv.own.owntv.player.LivePreviewEngine
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.format.rememberSystemTimeFormatter
import tv.own.owntv.ui.stage.StageButton
import tv.own.owntv.ui.stage.StageFocus
import tv.own.owntv.ui.stage.StageLiveBadge
import tv.own.owntv.ui.stage.StageSurface
import tv.own.owntv.ui.theme.StageColors
import tv.own.owntv.ui.theme.StageRadii
import tv.own.owntv.ui.theme.mpx
import tv.own.owntv.ui.theme.mpxSp
import tv.own.owntv.ui.theme.stageAccent
import tv.own.owntv.ui.theme.stageText

private val HeroBase = Color(0xFF0E1016)

/**
 * Aura: the Live-TV card at the top of Start, drawn as in the approved sketch — one wide rounded card, the
 * live picture on its right fading into the dark left half, and over it LIVE, the channel, what is on, its
 * times and progress, and the two buttons. The picture is Live TV's one preview player (no second decoder).
 *
 * Focus anywhere inside brings the whole card into view: without it the list could rest with the card
 * scrolled off the top and no way back up, because nothing above it takes focus.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeLiveNow(
    channel: ChannelEntity,
    onNow: XtEpgEntry?,
    engine: LivePreviewEngine,
    showVideo: Boolean,
    onWatch: () -> Unit,
    onGuide: () -> Unit,
    onFocus: () -> Unit,
    firstItemFocusRequester: FocusRequester?,
    start: Dp,
    track: (FocusRequester) -> Unit,
    modifier: Modifier = Modifier,
) {
    val watchFocus = firstItemFocusRequester ?: remember { FocusRequester() }
    val guideFocus = remember { FocusRequester() }
    val bring = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val previewState by engine.state.collectAsStateWithLifecycle()
    val playing = showVideo && previewState != LivePreviewEngine.State.ERROR && previewState != LivePreviewEngine.State.IDLE
    val formatTime = rememberSystemTimeFormatter()
    val a = stageAccent
    val now = System.currentTimeMillis()
    val progress = onNow?.let { ((now - it.startMs).toFloat() / (it.stopMs - it.startMs).coerceAtLeast(1L)).coerceIn(0f, 1f) }
    val cornerRadius = 28.mpx

    Box(
        modifier
            .padding(start = start, end = 64.mpx)
            .fillMaxWidth()
            .height(400.mpx)
            .bringIntoViewRequester(bring)
            .onFocusChanged { if (it.hasFocus) { onFocus(); scope.launch { bring.bringIntoView() } } }
            .focusGroup()
            .clip(RoundedCornerShape(cornerRadius))
            .background(HeroBase),
    ) {
        // The picture: right-aligned, full card height, 16:9.
        Box(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight().aspectRatio(16f / 9f).reportPreviewBounds(cornerRadius),
        ) {
            if (playing) tv.own.owntv.player.ExoPreviewSurface(engine = engine, modifier = Modifier.fillMaxSize())
        }
        // Fade from the dark left half into the picture.
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to HeroBase, 0.42f to HeroBase.copy(alpha = 0.92f), 0.7f to HeroBase.copy(alpha = 0.35f), 1f to Color.Transparent,
                ),
            ),
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(start = 48.mpx, bottom = 40.mpx).width(820.mpx),
            verticalArrangement = Arrangement.spacedBy(14.mpx),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.mpx), verticalAlignment = Alignment.CenterVertically) {
                StageLiveBadge(stringResource(R.string.player_live))
                Text(
                    channel.name,
                    style = stageText(21, 700), color = Color.White.copy(alpha = 0.82f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                onNow?.title ?: channel.name,
                style = stageText(56, 800, (-1.5).mpxSp), color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (onNow != null) {
                Text(
                    stringResource(R.string.content_live_time_range_plain, formatTime(onNow.startMs), formatTime(onNow.stopMs)),
                    style = stageText(20, 500), color = Color.White.copy(alpha = 0.78f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (progress != null) {
                Box(Modifier.fillMaxWidth().height(6.mpx).clip(RoundedCornerShape(3.mpx)).background(Color.White.copy(alpha = 0.2f))) {
                    Box(
                        Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(3.mpx))
                            .background(Brush.horizontalGradient(listOf(a.accent, a.focus))),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.mpx)) {
                StageButton(
                    stringResource(R.string.content_epg_watch_channel), onClick = onWatch, icon = OwnTVIcon.PLAY, iconFilled = true,
                    height = 58.mpx, textSize = 20, tinted = true,
                    modifier = Modifier.focusRequester(watchFocus).tracked(watchFocus, track),
                )
                StageButton(
                    stringResource(R.string.common_nav_guide_short), onClick = onGuide,
                    height = 58.mpx, textSize = 20,
                    modifier = Modifier.focusRequester(guideFocus).tracked(guideFocus, track),
                )
            }
        }
    }
}

/**
 * Aura: the favourite channels as a strip of chips (logo, channel, what is on, a progress bar), as in the
 * sketch. A chip with no guide data shows just the channel. OK tunes the channel.
 */
@Composable
fun HomeChannelStrip(
    channels: List<ChannelEntity>,
    guide: GuideSliceState,
    onChannelClick: (Long, List<ChannelEntity>) -> Unit,
    onFocus: () -> Unit,
    firstItemFocusRequester: FocusRequester?,
    start: Dp,
    track: (FocusRequester) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shown = guide.channels.ifEmpty { channels }
    if (shown.isEmpty()) return
    val a = stageAccent
    val clock = if (guide.now > 0L) guide.now else System.currentTimeMillis()
    LazyRow(
        modifier.focusRestorer().focusGroup(),
        contentPadding = PaddingValues(start = start, end = 64.mpx, top = 6.mpx, bottom = 6.mpx),
        horizontalArrangement = Arrangement.spacedBy(14.mpx),
    ) {
        itemsIndexed(shown, key = { _, c -> c.id }) { index, channel ->
            val programme = currentProgramme(guide.programmes[channel.id].orEmpty(), clock)
            StageSurface(
                onClick = { onChannelClick(channel.id, shown) },
                radius = StageRadii.Row,
                modifier = Modifier
                    .width(330.mpx).height(92.mpx)
                    .then(if (index == 0 && firstItemFocusRequester != null) Modifier.focusRequester(firstItemFocusRequester) else Modifier)
                    .tracked(track = track)
                    .onFocusChanged { if (it.hasFocus) onFocus() },
                focusStyle = StageFocus.FX,
                idle = Modifier.background(StageColors.Ink.copy(alpha = 0.06f), RoundedCornerShape(StageRadii.Row)),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 16.mpx, vertical = 12.mpx),
                    horizontalArrangement = Arrangement.spacedBy(16.mpx),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ChannelPlate(channel, Modifier.size(72.mpx, 50.mpx))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.mpx)) {
                        Text(channel.name, style = stageText(20, 700), color = StageColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (programme != null) {
                            Text(programme.title, style = stageText(17, 500), color = StageColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Box(Modifier.fillMaxWidth().height(4.mpx).clip(RoundedCornerShape(2.mpx)).background(StageColors.Ink.copy(alpha = 0.12f))) {
                                Box(
                                    Modifier.fillMaxWidth(programmeProgress(programme, clock)).fillMaxHeight().clip(RoundedCornerShape(2.mpx))
                                        .background(Brush.horizontalGradient(listOf(a.accent, a.focus))),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The programme airing on [channel] now, looked up once per channel by [lookup]; null until known or when none. */
@Composable
fun rememberNowEntry(channel: ChannelEntity?, lookup: suspend (ChannelEntity) -> XtEpgEntry?): XtEpgEntry? {
    val entry by produceState<XtEpgEntry?>(null, channel?.id) {
        value = null
        value = channel?.let { runCatching { lookup(it) }.getOrNull() }
    }
    return entry
}
