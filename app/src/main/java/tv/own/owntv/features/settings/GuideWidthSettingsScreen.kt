package tv.own.owntv.features.settings

import tv.own.owntv.ui.theme.StageColors
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import org.koin.androidx.compose.koinViewModel
import tv.own.owntv.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import tv.own.owntv.ui.theme.mpx
import tv.own.owntv.core.settings.GuideWidthLimits
import tv.own.owntv.core.settings.GuideWidthShares
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.components.trapAllFocusExit
import tv.own.owntv.features.epg.GuideLayoutPrefs

/** Layout setting for the Guide's pinned channel column and scrollable programme timeline. */
@Composable
fun GuideWidthSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm: SettingsViewModel = koinViewModel()
    val enabled by vm.guideWidthEnabled.collectAsStateWithLifecycle()
    val shares by vm.guideWidthShares.collectAsStateWithLifecycle()
    val current = shares ?: GuideWidthLimits.defaults
    val rowFocus = remember { FocusRequester() }
    var showDialog by remember { mutableStateOf(false) }
    // Row height and text size of the guide (this app's own store, see GuideLayoutPrefs).
    GuideLayoutPrefs.init(androidx.compose.ui.platform.LocalContext.current)
    val rowPct by GuideLayoutPrefs.rowPct.collectAsStateWithLifecycle()
    val textPct by GuideLayoutPrefs.textPct.collectAsStateWithLifecycle()
    val lockNow by GuideLayoutPrefs.lockNow.collectAsStateWithLifecycle()
    val lockPos by GuideLayoutPrefs.lockPos.collectAsStateWithLifecycle()
    var sizeEdit by remember { mutableStateOf<GuideSizeEdit?>(null) }

    LaunchedEffect(Unit) { runCatching { rowFocus.requestFocus() } }
    LaunchedEffect(showDialog) {
        if (!showDialog) runCatching { rowFocus.requestFocus() }
    }
    // The TV Guide row and Reset; the panel draws the two columns at their widths.
    val rules = stringResource(R.string.settings_guide_width_help, *NO_ARGS)
    StageFullPage(
        parents = listOf(stringResource(R.string.settings_group_layout)),
        title = stringResource(R.string.settings_guide_width),
        count = pluralStringResource(R.plurals.settings_setting_count, 6, 6),
        onBack = onBack,
        modifier = modifier,
    ) {
        val title = stringResource(R.string.content_epg_title)
        val value = SettingValue.Opens(stringResource(if (enabled) R.string.settings_live_latency_custom else R.string.settings_subtitle_default))
        StageSettingRow(
            icon = OwnTVIcon.EPG,
            title = title,
            desc = stringResource(R.string.settings_guide_width_summary, current.channels, current.epg),
            value = value,
            onClick = { showDialog = true },
            help = SettingHelp(
                title, rules,
                hints = settingHints(value, pinnable = false),
                extra = { androidx.compose.foundation.layout.Box(Modifier.padding(top = 18.mpx)) { GuideWidthDiagram(current) } },
            ),
            modifier = Modifier.focusRequester(rowFocus),
        )
        val percent: @Composable (Int) -> String = { stringResource(R.string.common_percent, it) }
        val preview: @Composable () -> Unit = { GuideLayoutPreview(current, rowPct, textPct) }
        val rowTitle = stringResource(R.string.settings_guide_row_height)
        val rowDesc = stringResource(R.string.settings_guide_row_height_description)
        val rowValue = SettingValue.Stepper(percent(rowPct))
        val stepRow = { d: Int -> GuideLayoutPrefs.setRow(rowPct + d * GuideLayoutPrefs.STEP) }
        StageSettingRow(
            icon = OwnTVIcon.EPG,
            title = rowTitle,
            desc = rowDesc,
            value = rowValue,
            onClick = { sizeEdit = GuideSizeEdit.ROW },
            onStep = stepRow,
            help = SettingHelp(rowTitle, rowDesc, hints = settingHints(rowValue, pinnable = false),
                extra = { Box(Modifier.padding(top = 18.mpx), content = { preview() }) }),
        )
        val textTitle = stringResource(R.string.settings_guide_text_size)
        val textDesc = stringResource(R.string.settings_guide_text_size_description)
        val textValue = SettingValue.Stepper(percent(textPct))
        val stepText = { d: Int -> GuideLayoutPrefs.setText(textPct + d * GuideLayoutPrefs.STEP) }
        StageSettingRow(
            icon = OwnTVIcon.TEXT_SIZE,
            title = textTitle,
            desc = textDesc,
            value = textValue,
            onClick = { sizeEdit = GuideSizeEdit.TEXT },
            onStep = stepText,
            help = SettingHelp(textTitle, textDesc, hints = settingHints(textValue, pinnable = false),
                extra = { Box(Modifier.padding(top = 18.mpx), content = { preview() }) }),
        )
        val lockTitle = stringResource(R.string.aura_guide_lock_now)
        val lockDesc = stringResource(R.string.aura_guide_lock_now_description)
        StageSettingRow(
            icon = OwnTVIcon.NOW,
            title = lockTitle,
            desc = lockDesc,
            value = SettingValue.Switch(lockNow),
            onClick = { GuideLayoutPrefs.setLockNow(!lockNow) },
            help = SettingHelp(lockTitle, lockDesc, hints = settingHints(SettingValue.Switch(lockNow), pinnable = false)),
        )
        val posTitle = stringResource(R.string.aura_guide_lock_pos)
        val posDesc = stringResource(R.string.aura_guide_lock_pos_description)
        val posValue = SettingValue.Stepper(percent(lockPos))
        StageSettingRow(
            icon = OwnTVIcon.NOW,
            title = posTitle,
            desc = posDesc,
            value = posValue,
            onClick = { GuideLayoutPrefs.setLockPos(GuideLayoutPrefs.LOCK_DEFAULT) },
            onStep = { d -> GuideLayoutPrefs.setLockPos(lockPos + d * GuideLayoutPrefs.LOCK_STEP) },
            help = SettingHelp(posTitle, posDesc, hints = settingHints(posValue, pinnable = false)),
        )
        val reset = stringResource(R.string.common_reset)
        StageSettingRow(
            icon = OwnTVIcon.REFRESH,
            title = reset,
            desc = stringResource(R.string.settings_guide_width_summary, GuideWidthLimits.defaults.channels, GuideWidthLimits.defaults.epg),
            value = null,
            onClick = { vm.setGuideWidths(false, GuideWidthLimits.defaults); GuideLayoutPrefs.reset() },
            help = SettingHelp(reset, rules, hints = settingHints(null, pinnable = false)),
        )
        when (sizeEdit) {
            GuideSizeEdit.ROW -> panelEditor({ sizeEdit = null }) {
                PanelStepper(percent(rowPct), onStep = stepRow, onReset = { GuideLayoutPrefs.setRow(GuideLayoutPrefs.DEFAULT) },
                    onDone = { sizeEdit = null }, preview = preview)
            }
            GuideSizeEdit.TEXT -> panelEditor({ sizeEdit = null }) {
                PanelStepper(percent(textPct), onStep = stepText, onReset = { GuideLayoutPrefs.setText(GuideLayoutPrefs.DEFAULT) },
                    onDone = { sizeEdit = null }, preview = preview)
            }
            null -> Unit
        }
        // In the page's panel.
        if (showDialog) panelEditor({ showDialog = false }) {
            GuideWidthDialog(savedEnabled = enabled, savedShares = current, onSave = vm::setGuideWidths, onDismiss = { showDialog = false })
        }
    }

}

@Composable
private fun GuideWidthDialog(
    savedEnabled: Boolean,
    savedShares: GuideWidthShares,
    onSave: (Boolean, GuideWidthShares) -> Unit,
    onDismiss: () -> Unit,
) {
    // The two columns always add up to 100%, so there is one number to set: the channel column's
    // width; the guide takes the rest. In the page's panel, saved as it changes.
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(60); runCatching { first.requestFocus() } }
    androidx.compose.foundation.layout.Column(Modifier.trapAllFocusExit().focusGroup()) {
        tv.own.owntv.ui.stage.StagePopupOption(
            title = stringResource(R.string.settings_panel_width_customize), onClick = { onSave(!savedEnabled, savedShares) },
            modifier = Modifier.focusRequester(first),
            trailing = { tv.own.owntv.ui.stage.StageSwitch(savedEnabled) },
        )
        if (savedEnabled) {
            StepRow(stringResource(R.string.settings_guide_width_channels), savedShares.channels, GuideWidthLimits.MIN, GuideWidthLimits.MAX, GuideWidthLimits.STEP) {
                onSave(true, GuideWidthShares(it, GuideWidthLimits.TOTAL - it))
            }
            Box(Modifier.padding(horizontal = 8.mpx, vertical = 14.mpx)) { GuideWidthDiagram(savedShares) }
        }
        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(top = 8.mpx), horizontalArrangement = Arrangement.spacedBy(12.mpx, Alignment.End)) {
            tv.own.owntv.ui.stage.StageButton(stringResource(R.string.common_reset), onClick = { onSave(savedEnabled, GuideWidthLimits.defaults) }, height = 52.mpx, textSize = 18)
            tv.own.owntv.ui.stage.StageButton(stringResource(R.string.common_done), onClick = onDismiss, height = 52.mpx, textSize = 18, tinted = true)
        }
    }
}

@Composable
private fun GuideWidthDiagram(shares: GuideWidthShares) {
    // The two columns at their widths, in the Stage colours: channels in accent, the guide plain.
    val a = tv.own.owntv.ui.theme.stageAccent
    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().height(52.mpx), horizontalArrangement = Arrangement.spacedBy(6.mpx)) {
        listOf(shares.channels to a.accent.copy(alpha = 0.32f), shares.epg to tv.own.owntv.ui.theme.StageColors.Ink.copy(alpha = 0.08f)).forEach { (w, fill) ->
            Box(Modifier.weight(w.toFloat()).fillMaxSize().background(fill, RoundedCornerShape(10.mpx)), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.common_percent, w), style = tv.own.owntv.ui.theme.stageText(15, 700), color = tv.own.owntv.ui.theme.StageColors.Text, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
    }
}

private enum class GuideSizeEdit { ROW, TEXT }

/**
 * A few guide rows as they will look: the channel column and two programme cells at the chosen widths,
 * row height and text size. Drawn in the panel at a third of the real size, so the proportions are the guide's.
 */
@Composable
private fun GuideLayoutPreview(shares: GuideWidthShares, rowPct: Int, textPct: Int) {
    val a = tv.own.owntv.ui.theme.stageAccent
    val text = textPct / 100f
    val row = maxOf(rowPct, textPct) / 100f
    Column(Modifier.fillMaxWidth().background(StageColors.Ink.copy(alpha = 0.05f), RoundedCornerShape(12.mpx)).padding(8.mpx), verticalArrangement = Arrangement.spacedBy(4.mpx)) {
        (0 until 3).forEach { i ->
            Row(Modifier.fillMaxWidth().height((36f * row).mpx), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.mpx)) {
                Box(Modifier.weight(shares.channels.toFloat()).fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    Text(stringResource(R.string.settings_guide_preview_channel, i + 1), style = tv.own.owntv.ui.theme.stageText(12 * text, 700), color = StageColors.Text, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                Row(Modifier.weight(shares.epg.toFloat()).fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(3.mpx)) {
                    (0 until 2).forEach { c ->
                        val focused = i == 0 && c == 0
                        Column(
                            Modifier.weight(1f).fillMaxSize()
                                .background(if (focused) a.accent else StageColors.Ink.copy(alpha = 0.08f), RoundedCornerShape(8.mpx))
                                .padding(horizontal = 6.mpx),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(stringResource(R.string.settings_guide_preview_programme), style = tv.own.owntv.ui.theme.stageText(12 * text, 700), color = if (focused) a.onAccent else StageColors.Text, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(String.format(java.util.Locale.ROOT, "%d:%02d", 19 + c, 30 * (i % 2)), style = tv.own.owntv.ui.theme.stageText(9 * text, 400), color = if (focused) a.onAccent else StageColors.Dim, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
