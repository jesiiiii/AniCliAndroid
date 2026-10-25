package dev.anicli.android

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Fallback resolver for direct video URLs stored in episode_urls.
 * Some API entries were parsed as direct servers but dropped by SlayerApi.resolveStreams.
 */
object DirectEpisodeStreams {
    suspend fun resolve(animeId: String, episodeNumber: String): List<StreamOption> =
        withContext(Dispatchers.IO) {
            val details = runCatching { SlayerApi.getAnimeWithEpisodes(animeId) }.getOrNull()
                ?: return@withContext emptyList()
            val requested = episodeNumber.trim().toIntOrNull()
            val episode = details.episodes.firstOrNull {
                it.number.trim() == episodeNumber.trim() ||
                    (requested != null && it.number.trim().toIntOrNull() == requested)
            } ?: return@withContext emptyList()

            val servers = runCatching { JSONArray(episode.serversJson) }.getOrNull()
                ?: return@withContext emptyList()
            val found = LinkedHashMap<String, StreamOption>()
            for (i in 0 until servers.length()) {
                val server = servers.optJSONObject(i) ?: continue
                val url = server.optString("episode_url", "").trim()
                if (!isDirectMediaUrl(url)) continue
                val serverName = server.optString("episode_server_name", "Direct").trim()
                    .ifBlank { "Direct" }
                val path = Uri.parse(url).encodedPath.orEmpty().lowercase()
                val quality = when {
                    "1080" in url -> "1080p"
                    "720" in url -> "720p"
                    "480" in url -> "480p"
                    path.endsWith(".m3u8") || path.endsWith(".mpd") -> "Auto"
                    else -> "Auto"
                }
                found.putIfAbsent(url, StreamOption("$serverName • $quality", url, quality))
            }
            found.values.toList()
        }

    private fun isDirectMediaUrl(url: String): Boolean {
        if (!url.startsWith("https://") && !url.startsWith("http://")) return false
        val path = runCatching { Uri.parse(url).encodedPath.orEmpty().lowercase() }.getOrDefault("")
        return path.endsWith(".mp4") || path.endsWith(".m3u8") ||
            path.endsWith(".mpd") || path.endsWith(".m4v") || path.endsWith(".webm")
    }
}
