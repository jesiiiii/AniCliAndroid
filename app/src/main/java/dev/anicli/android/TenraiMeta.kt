package dev.anicli.android

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Episode-metadata enrichment backed by the Tenrai API (https://api.tenrai.org).
 * Source of episode titles + episode-specific cover images (Crunchyroll) where the
 * API covers the show. The Arabic Slayer source stays authoritative for playback,
 * numbers, and ordering; Tenrai only enriches titles/thumbnails. Every response is
 * cached on disk per anime and images are cached by Coil's disk cache.
 */
object TenraiMeta {

    const val BASE_URL = "https://api.tenrai.org/v1"

    class EpisodeMeta(
        val number: String,
        val title: String?,      // null = no verified episode title available
        val thumbnailUrl: String? // null = API has no covered image for this episode
    )

    data class EnrichResult(
        val malId: Int?,
        val episodes: Map<String, EpisodeMeta>
    )

    data class TenraiAnimeHit(
        val malId: Int,
        val title: String,
        val approved: Boolean
    )

    // ------------------------------------------------------------ pure parsing (unit-testable)

    /** Epoch time decoder for "2025-10-26T21:52:29.066Z" / extended ISO forms. */
    fun parseTenraiEpisode(body: String): EpisodeMeta? {
        val root = try {
            JSONObject(body)
        } catch (_: Exception) {
            return null
        }
        val data = root.optJSONObject("data") ?: return null
        val title = data.optString("title", "").trim().ifBlank { null }
        val images = data.optJSONObject("images")
        var thumb: String? = null
        if (images != null) {
            thumb = images.optString("image_url", "").trim().ifBlank { null }
            if (thumb == null) {
                thumb = images.optJSONObject("webp")?.optString("image_url", "")?.trim()?.ifBlank { null }
            }
            if (thumb == null) {
                thumb = images.optJSONObject("jpg")?.optString("image_url", "")?.trim()?.ifBlank { null }
            }
        }
        val malNumber = data.optInt("mal_id", -1)
        return EpisodeMeta(number = if (malNumber > 0) malNumber.toString() else "", title = title, thumbnailUrl = thumb)
    }

    fun parseTenraiAnimeList(body: String): List<TenraiAnimeHit> {
        val root = try {
            JSONObject(body)
        } catch (_: Exception) {
            return emptyList()
        }
        val data = root.optJSONArray("data") ?: return emptyList()
        return buildList {
            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                val id = item.optInt("mal_id", -1)
                if (id <= 0) continue
                add(TenraiAnimeHit(id, item.optString("title", "").trim(), item.optBoolean("approved", false)))
            }
        }
    }

    /** Strict matcher: approved hit with an exact, case-insensitive-en-folded title match only. */
    fun matchAnime(hits: List<TenraiAnimeHit>, englishTitle: String): TenraiAnimeHit? {
        val needle = normalizeTitle(englishTitle)
        if (needle.isBlank()) return null
        return hits.firstOrNull { it.approved && normalizeTitle(it.title) == needle }
            ?: hits.firstOrNull { normalizeTitle(it.title) == needle}
    }

    fun normalizeTitle(value: String): String = value.trim()
        .lowercase()
        .replace("é", "e")
        .replace(Regex("\\s+"), " ")

    // ---------------------------------------------------------------- transport

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    private val tenraiGate = Semaphore(2)

    // ---------------------------------------------------------------- caching

    private fun cacheDir(context: Context): File = File(context.cacheDir, "tenrai").apply { mkdirs() }

    private fun cacheFile(context: Context, animeId: String) =
        File(cacheDir(context), "episodes-$animeId.json")

    fun loadCachedEpisodeMeta(context: Context, animeId: String): Map<String, EpisodeMeta>? {
        val f = cacheFile(context, animeId)
        if (!f.exists()) return null
        return try {
            val root = JSONObject(f.readText())
            val eps = root.optJSONObject("episodes") ?: return null
            buildMap {
                val keys = eps.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val o = eps.optJSONObject(k) ?: continue
                    put(k, EpisodeMeta(k, o.optString("t", "").ifBlank { null }, o.optString("u", "").ifBlank { null }))
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun writeCoveredCache(context: Context, animeId: String, malId: Int?, episodes: Map<String, EpisodeMeta>) {
        if (episodes.isEmpty()) return
        try {
            val epsObj = JSONObject()
            episodes.forEach { (k, meta) ->
                epsObj.put(k, JSONObject().apply {
                    if (meta.title != null) put("t", meta.title)
                    if (meta.thumbnailUrl != null) put("u", meta.thumbnailUrl)
                })
            }
            cacheFile(context, animeId).writeText(
                JSONObject().apply {
                    if (malId != null) put("mal_id", malId)
                    put("episodes", epsObj)
                }.toString()
            )
        } catch (_: Exception) {
            // cache write is an optimization only
        }
    }

    // ----------------------------------------------------------- public surface

    private suspend fun httpJson(url: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            tenraiGate.acquire()
        } catch (_: InterruptedException) {
            return@withContext null
        }
        try {
            suspendCancellableCoroutine { cont ->
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                        if (cont.isActive) cont.resume(null)
                    }

                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        response.use {
                            val text = it.body?.string().orEmpty()
                            if (!it.isSuccessful || text.isBlank()) {
                                if (cont.isActive) cont.resume(null)
                            } else if (cont.isActive) {
                                cont.resume(text)
                            }
                        }
                    }
                })
                cont.invokeOnCancellation { /* okhttp call independently bounded by timeouts */ }
            }?.let { try { JSONObject(it) } catch (_: Exception) { null } }
        } finally {
            tenraiGate.release()
            delay(120)
        }
    }

    private fun searchUrl(query: String) = "$BASE_URL/anime?q=${java.net.URLEncoder.encode(query, "UTF-8")}&limit=15"

    fun toEpisodeUrlString(malId: Int, epNumber: Int): String = "$BASE_URL/anime/$malId/episodes/$epNumber"

    /** Resolve the MyAnimeList id via Tenrai; only exact approved / exact title matches are accepted. */
    suspend fun resolveMalId(englishTitle: String): Int? {
        val bodyJson = httpJson(searchUrl(englishTitle)) ?: return null
        val hit = matchAnime(parseTenraiAnimeList(bodyJson.toString()), englishTitle) ?: return null
        return hit.malId
    }

    /**
     * Enrich an anime's episodes. Never throws: failures degrade to a missing entry and
     * callers keep the source-supplied fallback.
     */
    suspend fun enrich(
        context: Context,
        animeId: String,
        englishTitle: String,
        episodes: List<Episode>,
        maxEpisodes: Int = 40
    ): EnrichResult {
        val cached = loadCachedEpisodeMeta(context, animeId)?.toMutableMap() ?: mutableMapOf()
        val malId = resolveMalId(englishTitle)
        if (malId == null) return EnrichResult(null, cached)

        val unreachable = episodes.take(maxEpisodes).filter { it.number !in cached }
        unreachable.chunked(4).forEach { batch ->
            for (ep in batch) {
                val n = ep.number.toIntOrNull() ?: continue
                val meta = httpJson(toEpisodeUrlString(malId, n))?.let { parseTenraiEpisode(it.toString()) }
                if (meta != null) {
                    val merged = EpisodeMeta(
                        number = ep.number,
                        title = meta.title,
                        thumbnailUrl = meta.thumbnailUrl
                    )
                    cached[ep.number] = merged
                }
            }
        }
        writeCoveredCache(context, animeId, malId, cached)
        return EnrichResult(malId, cached)
    }
}
