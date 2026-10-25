package dev.anicli.android.util

import android.content.Context
import dev.anicli.android.StreamOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for the app-level user preferences. Stored in the existing
 * "uchiha_prefs" shared-preferences file (same file as onboarding) so nothing unrelated
 * is erased and no second store is introduced. Volatile state mirrors into StateFlows so
 * Compose hosts can collect them while SharedPreferences keeps the durable store.
 */
object UchihaSettings {
    private const val FILE = "uchiha_prefs"
    private const val KEY_OLED = "oled_dark"
    private const val KEY_AUTO_NEXT = "auto_next_episode"
    private const val KEY_QUALITY = "preferred_quality"

    const val QUALITY_AUTO = "auto"
    const val QUALITY_1080P = "1080p"
    const val QUALITY_720P = "720p"
    const val QUALITY_480P = "480p"
    const val QUALITY_360P = "360p"

    fun oledDark(context: Context): Boolean = oledStateFlow.value

    private val oledInternalFlow = MutableStateFlow(false)
    val oledStateFlow: StateFlow<Boolean> get() = oledInternalFlow

    fun setOledDark(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_OLED, enabled).apply()
        oledInternalFlow.value = enabled
    }

    fun autoNextEpisode(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_AUTO_NEXT, true)

    fun setAutoNextEpisode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO_NEXT, enabled).apply()
    }

    /** Defaults to AUTO; stale/unknown stored values collapse back to AUTO. */
    fun preferredQuality(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_QUALITY, null)
            ?.takeIf { it in setOf(QUALITY_AUTO, QUALITY_1080P, QUALITY_720P, QUALITY_480P, QUALITY_360P) }
            ?: QUALITY_AUTO

    fun setPreferredQuality(context: Context, quality: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_QUALITY, quality).apply()
    }

    /**
     * Re-orders resolved streams so the user's preferred quality's nearest-match starts
     * playback first. Never drops streams: if the exact preference doesn't exist, a
     * graceful nearest-available fallback keeps playback alive.
     */
    fun orderedByPreference(streams: List<StreamOption>, preferred: String): List<StreamOption> {
        if (preferred == QUALITY_AUTO) return streams
        val target = preferred.toIntOrNull() ?: return streams
        val scored = streams.mapIndexed { index, stream ->
            val rank = stream.rank()
            Triple(rank, index, stream)
        }
        // Nearest quality wins; ties break by the original (best-first) ordering.
        val sorted = scored.sortedWith(
            compareBy(
                { kotlin.math.abs(it.first * 180 - target) },
                { it.second }
            )
        )
        return sorted.map { it.third }
    }

    /** Seed the in-memory mirrors from durable storage once per process. */
    fun bootstrap(context: Context) {
        oledInternalFlow.value = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_OLED, false)
    }
}
