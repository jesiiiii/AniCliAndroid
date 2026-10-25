package dev.anicli.android

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Fallback resolver for direct video URLs stored in episode_urls.
 * Some API entries were parsed as direct servers but dropped by SlayerApi.resolveStreams.
 */
object DirectEpisodeStreams {
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/132.0.0.0 Safari/537.36"

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
                val sourceUrl = server.optString("episode_url", "").trim()
                val serverName = server.optString("episode_server_name", "Direct").trim()
                    .ifBlank { "Direct" }
                val url = resolveCandidate(sourceUrl) ?: continue
                val path = Uri.parse(url).encodedPath.orEmpty().lowercase()
                val quality = when {
                    "1080" in url -> "1080p"
                    "720" in url -> "720p"
                    "480" in url -> "480p"
                    path.endsWith(".m3u8") || path.endsWith(".mpd") -> "Auto"
                    else -> "Auto"
                }
                if (!found.containsKey(url)) found[url] = StreamOption("$serverName • $quality", url, quality)
            }
            found.values.toList()
        }

    private fun resolveCandidate(url: String): String? {
        if (isDirectMediaUrl(url, "")) return url
        return when {
            url.contains("ok.ru") || url.contains("odnoklassniki") -> resolveOkru(url)
            url.contains("mp4upload") -> resolveMp4Upload(url)
            url.contains("goodstream") -> resolveGoodstream(url)
            else -> null
        }
    }

    private fun fetchText(url: String): String? = try {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).get().build()
        http.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body?.string() else null
        }
    } catch (_: Exception) {
        null
    }

    private fun resolveOkru(link: String): String? {
        val id = Regex("""/video/(\d+)""").find(link)?.groupValues?.get(1) ?: return null
        val html = fetchText("https://ok.ru/videoembed/$id") ?: return null
        val options = Regex("""data-options=["'](\{.*?\})["']""").find(html)?.groupValues?.get(1)
            ?: return null
        return runCatching {
            val json = JSONObject(options.replace("&quot;", "\"").replace("&amp;", "&"))
            val videos = json.optJSONArray("videos") ?: return null
            (0 until videos.length()).asSequence()
                .mapNotNull { videos.optJSONObject(it)?.optString("url") }
                .firstOrNull { it.startsWith("http") }
        }.getOrNull()
    }

    private fun resolveMp4Upload(link: String): String? {
        val html = fetchText(link) ?: return null
        return Regex("""player\.src\(\{\s*type:\s*["']video/mp4["'],\s*src:\s*["'](https?://[^"']+)["']""")
            .find(html)?.groupValues?.get(1)
            ?: Regex("""src:\s*["'](https?://[^"']+\.mp4[^"']*)["']""").find(html)?.groupValues?.get(1)
    }

    private fun resolveGoodstream(link: String): String? {
        val html = fetchText(link) ?: return null
        return Regex("""["'](https?://[^"']+\.m3u8[^"']*)["']""").find(html)?.groupValues?.get(1)
    }

    private fun isDirectMediaUrl(url: String, serverName: String): Boolean {
        if (!url.startsWith("https://") && !url.startsWith("http://")) return false
        val path = runCatching { Uri.parse(url).encodedPath.orEmpty().lowercase() }.getOrDefault("")
        val name = serverName.lowercase()
        return path.endsWith(".mp4") || path.endsWith(".m3u8") ||
            path.endsWith(".mpd") || path.endsWith(".m4v") || path.endsWith(".webm") ||
            url.contains("ab-hunter", ignoreCase = true) ||
            name in setOf("direct", "mp4", "hls", "m3u8", "dash")
    }
}
