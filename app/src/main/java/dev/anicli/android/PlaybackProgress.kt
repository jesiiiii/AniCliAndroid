package dev.anicli.android

import android.content.Context

/**
 * Small device-local playback checkpoint. It stores only episode positions,
 * never stream URLs or account information.
 */
object PlaybackProgress {
    private const val PREFS = "uchiha_playback_progress"

    private fun key(animeId: String, episode: String) =
        "position_${animeId.trim()}_${episode.trim()}"

    fun get(context: Context, animeId: String, episode: String): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(key(animeId, episode), 0L)
            .coerceAtLeast(0L)

    fun save(context: Context, animeId: String, episode: String, positionMs: Long, durationMs: Long) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val progressKey = key(animeId, episode)
        // Near the end, treat the episode as completed instead of resuming the final seconds.
        if (durationMs > 0L && positionMs >= durationMs - 30_000L) {
            prefs.edit().remove(progressKey).apply()
        } else if (positionMs >= 5_000L) {
            prefs.edit().putLong(progressKey, positionMs).apply()
        }
    }
}
