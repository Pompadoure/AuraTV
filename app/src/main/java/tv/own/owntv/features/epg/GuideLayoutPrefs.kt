package tv.own.owntv.features.epg

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * How big the programme guide draws: its row height and its text, as a percentage of the stock layout
 * (100). Kept in this app's own SharedPreferences, because the settings repository belongs to the core
 * library. The guide reads it through [rememberGuideScale]; Settings › Guide layout writes it.
 */
internal object GuideLayoutPrefs {
    const val MIN = 80
    const val MAX = 160
    const val STEP = 10
    const val DEFAULT = 100

    private const val FILE = "aura_guide_layout"
    private const val KEY_ROW = "row_pct"
    private const val KEY_TEXT = "text_pct"

    private val _row = MutableStateFlow(DEFAULT)
    private val _text = MutableStateFlow(DEFAULT)
    val rowPct: StateFlow<Int> = _row.asStateFlow()
    val textPct: StateFlow<Int> = _text.asStateFlow()

    @Volatile private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        _row.value = p.getInt(KEY_ROW, DEFAULT).coerceIn(MIN, MAX)
        _text.value = p.getInt(KEY_TEXT, DEFAULT).coerceIn(MIN, MAX)
        prefs = p
    }

    fun setRow(pct: Int) {
        val v = pct.coerceIn(MIN, MAX)
        _row.value = v
        prefs?.edit()?.putInt(KEY_ROW, v)?.apply()
    }

    fun setText(pct: Int) {
        val v = pct.coerceIn(MIN, MAX)
        _text.value = v
        prefs?.edit()?.putInt(KEY_TEXT, v)?.apply()
    }

    fun reset() {
        setRow(DEFAULT)
        setText(DEFAULT)
    }
}

/**
 * The guide's two scale factors. [row] is never smaller than [text], so the text always fits in its row
 * (the row grows with it); at 100 / 100 everything is exactly the stock size.
 */
internal data class GuideScale(val rowFactor: Float, val text: Float) {
    val row: Float get() = maxOf(rowFactor, text)
}

@Composable
internal fun rememberGuideScale(): GuideScale {
    GuideLayoutPrefs.init(LocalContext.current)
    val row by GuideLayoutPrefs.rowPct.collectAsStateWithLifecycle()
    val text by GuideLayoutPrefs.textPct.collectAsStateWithLifecycle()
    return GuideScale(row / 100f, text / 100f)
}
