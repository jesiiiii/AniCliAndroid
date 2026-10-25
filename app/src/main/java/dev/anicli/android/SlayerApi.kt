package dev.anicli.android

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Anime Slayer provider port — same API contract as src/providers/anime_slayer.py
 * in the upstream ani-cli-arabic project (GPL-3.0). First-party CDN token flow,
 * RNCryptor v3 payload decryption, and third-party multi-server resolution are
 * ported 1:1. Nothing here needs user configuration.
 */
object SlayerApi {

    private const val BASE_URL = "https://anslayer.com/anime/public/"
    // Public constants published in the upstream open-source repository.
    private const val CLIENT_ID = "android-app2"
    private const val CLIENT_SECRET = "7befba6263cc14c90d2f1d6da2c5cf9b251bfbbd"
    private const val APK_CERT_SHA1 = "44D8B79265DDBB9C887320F64521A76D72F6D7D4"
    private const val RNC_PASSWORD = "android-app9>E>VBa=X%;[5BX~=Q~K"

    private val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36"

    private val apiHeaders = mapOf(
        "Client-Id" to CLIENT_ID,
        "Client-Secret" to CLIENT_SECRET,
        "Accept" to "application/json",
        "User-Agent" to UA
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val detailsCache = ConcurrentHashMap<String, AnimeWithEpisodes>()

    class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

    // ---------------------------------------------------------------- utilities

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")

    private fun httpGet(url: String, headers: Map<String, String> = apiHeaders, timeout: Long = 45L): String {
        val scoped = client.newBuilder().callTimeout(timeout, TimeUnit.SECONDS).build()
        val request = Request.Builder().url(url).apply {
            headers.forEach { (k, v) -> header(k, v) }
        }.get().build()
        scoped.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw ApiException("فشل الاتصال بالخادم (HTTP ${response.code}).")
            return body
        }
    }

    private fun httpPostJson(url: String, form: Map<String, String>, headers: Map<String, String> = apiHeaders, timeout: Long = 45L): String {
        val scoped = client.newBuilder().callTimeout(timeout, TimeUnit.SECONDS).build()
        val formBody = FormBody.Builder().apply { form.forEach { (k, v) -> add(k, v) } }.build()
        val request = Request.Builder().url(url).apply {
            headers.forEach { (k, v) -> header(k, v) }
        }.post(formBody).build()
        scoped.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw ApiException("فشل الاتصال بالخادم (HTTP ${response.code}).")
            return body
        }
    }

    /** GET an Anime Slayer public JSON-API endpoint. Returns parsed response JSON, null-safe rejection. */
    private fun apiGet(endpoint: String, jsonPayload: String? = null): JSONObject {
        val params = if (jsonPayload != null) "json=" + encode(jsonPayload) else ""
        val url = if (params.isBlank()) "$BASE_URL$endpoint" else "$BASE_URL$endpoint?$params"
        val body = httpGet(url)
        return try {
            JSONObject(body)
        } catch (_: JSONException) {
            throw ApiException("استجابة الخادم ليست JSON صالحاً.")
        }
    }

    // ---------------------------------------------------------------- parsing (pure, testable)

    private fun animeFromItem(it: JSONObject): Anime? {
        val id = it.optString("anime_id", "").trim()
        if (id.isBlank()) return null
        val titleAr = it.optString("anime_name", "").trim()
        val titleEn = it.optString("anime_english_title", "").trim().ifBlank { titleAr }
        return Anime(
            id = id,
            title = titleAr,
            titleEn = titleEn,
            posterUrl = it.optString("anime_cover_image_url", "")
                .takeIf { u -> u.startsWith("https://") },
            description = it.optString("anime_story", "").ifBlank {
                it.optString("anime_description", "")
            },
            animeType = it.optString("anime_type", "مسلسل").ifBlank { "مسلسل" },
            rating = it.optString("anime_rating", "").ifBlank { "" },
            genres = it.optString("anime_genres", "").ifBlank { "" },
            latestEpisode = it.optString("latest_episode_name", "").takeIf { e -> e.isNotBlank() }
        )
    }

    fun parseAnimeList(body: String): List<Anime> = try {
        val root = JSONObject(body)
        val items = when {
            root.has("response") && root.getJSONObject("response").get("data") is JSONArray ->
                root.getJSONObject("response").getJSONArray("data")
            root.has("response") && root.getJSONObject("response").get("data") is JSONObject -> JSONArray()
            root.has("data") -> root.getJSONArray("data")
            else -> JSONArray()
        }
        buildList {
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                animeFromItem(item)?.let { add(it) }
            }
        }
    } catch (_: JSONException) {
        emptyList()
    }

    private fun episodeTitleNumberRegex(): Pattern = Pattern.compile("\\d+")

    private val episodePrefixRegex: Pattern = Pattern.compile(
        "^(الحلقة|الحلقه|Episode|Ep|EP)\\s*[:\\s\\-]*\\d+\\s*[:\\s\\-]*",
        Pattern.CASE_INSENSITIVE
    )

    fun cleanEpisodeTitle(rawName: String, epNum: Int): String {
        var name = rawName.trim()
        if (name.isEmpty()) return ""
        name = episodePrefixRegex.matcher(name).replaceFirst("").trim()
        if (name.isEmpty() || name.matches(Regex("\\d+")) || name == epNum.toString()) return ""
        return name
    }

    fun parseAnimeDetails(body: String): AnimeWithEpisodes? = try {
        val root = JSONObject(body)
        val response = root.optJSONObject("response") ?: return null
        val anime = Anime(
            id = response.optString("anime_id", "").trim(),
            title = response.optString("anime_name", "").trim(),
            titleEn = response.optString("anime_english_title", "").trim(),
            posterUrl = response.optString("anime_cover_image_url", "").takeIf { it.startsWith("https://") },
            description = response.optString("anime_story", "").ifBlank {
                response.optString("anime_description", "").ifBlank { response.optString("anime_more_info", "") }
            },
            animeType = response.optString("anime_type", "مسلسل").ifBlank { "مسلسل" },
            rating = response.optString("anime_rating", "").ifBlank { "" },
            genres = response.optString("anime_genres", "").ifBlank { "" }
        ).let { if (it.id.isBlank()) null else it } ?: return null

        val epsRaw = response.opt("episodes")
        val epsData: JSONArray = when {
            epsRaw is JSONObject -> epsRaw.optJSONArray("data") ?: JSONArray()
            epsRaw is JSONArray -> epsRaw
            else -> JSONArray()
        }
        val episodes = buildList {
            for (i in 0 until epsData.length()) {
                val item = epsData.optJSONObject(i) ?: continue
                val numRaw = item.optString("episode_number", "").ifBlank { "1" }
                val epNum = try {
                    (numRaw.toDouble()).toInt()
                } catch (_: Exception) {
                    episodeTitleNumberRegex().matcher(numRaw).takeIf { it.find() }?.group()?.toInt() ?: 1
                }
                val servers = item.optJSONArray("episode_urls")
                add(
                    Episode(
                        number = epNum.toString(),
                        title = cleanEpisodeTitle(item.optString("episode_name", ""), epNum),
                        serversJson = if (servers != null) servers.toString() else ""
                    )
                )
            }
        }.sortedBy { it.number.toIntOrNull() ?: Int.MAX_VALUE }
        AnimeWithEpisodes(anime, episodes)
    } catch (_: JSONException) {
        throw ApiException("بيانات الأنمي غير صالحة من الخادم.")
    }

    // ---------------------------------------------------------------- public API

    suspend fun search(query: String): List<Anime> = parseAnimeList(
        fetchListBody(mapOf("list_type" to "anime_list", "anime_name" to query.trim(), "limit" to 30))
    )

    suspend fun trendingAnime(limit: Int = 20): List<Anime> = parseAnimeList(
        fetchListBody(mapOf("list_type" to "top_tv", "limit" to limit, "offset" to 0))
    )

    suspend fun topRatedAnime(limit: Int = 20): List<Anime> = parseAnimeList(
        fetchListBody(mapOf("list_type" to "top_anime", "limit" to limit, "offset" to 0))
    )

    suspend fun featuredAnime(limit: Int = 20): List<Anime> {
        val featured = parseAnimeList(
            fetchListBody(mapOf("list_type" to "featured", "limit" to limit))
        )
        return featured.ifEmpty { topRatedAnime(limit) }
    }

    suspend fun latestEpisodes(limit: Int = 20): List<LatestScreenEpisode> {
        val body = fetchListBody(mapOf("list_type" to "latest_episodes", "limit" to limit))
        return try {
            val items = JSONObject(body).getJSONObject("response").getJSONArray("data")
            buildList {
                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    val aid = item.optString("anime_id", "").trim()
                    val title = item.optString("anime_name", "").trim()
                    if (aid.isBlank() || title.isBlank()) continue
                    val m = episodeTitleNumberRegex().matcher(item.optString("latest_episode_name", ""))
                    val epNum = if (m.find()) m.group() else "1"
                    add(
                        LatestScreenEpisode(
                            animeId = aid,
                            animeTitle = title,
                            episodeNumber = epNum,
                            posterUrl = item.optString("anime_cover_image_url", "").takeIf { it.startsWith("https://") },
                            rating = item.optString("anime_rating", "").ifBlank { "" }
                        )
                    )
                }
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }

    private suspend fun fetchListBody(payload: Map<String, Any>): String = withContext(Dispatchers.IO) {
        val json = JSONObject().apply { payload.forEach { (k, v) -> put(k, v) } }.toString()
        apiGet("animes/get-published-animes", json).toString()
    }

    /**
     * Fetch a larger poster from Kitsu for the details hero only.
     * This is best-effort: the original Slayer poster remains the fallback.
     */
    suspend fun getHighResolutionPoster(anime: Anime): String? = withContext(Dispatchers.IO) {
        fun normalizeTitle(value: String): String = value.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]"), "")

        val knownTitles = listOf(anime.title, anime.titleEn)
            .map { normalizeTitle(it.trim()) }
            .filter { it.length >= 3 }
            .toSet()
        if (knownTitles.isEmpty()) return@withContext null

        val queries = listOf(anime.titleEn, anime.title)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        for (query in queries) {
            try {
                val url = "https://kitsu.io/api/edge/anime?filter[text]=${encode(query)}&page[limit]=10"
                val body = httpGet(
                    url,
                    headers = mapOf(
                        "Accept" to "application/vnd.api+json",
                        "User-Agent" to "UchihaAnime/1.0 (Android)"
                    ),
                    timeout = 12
                )
                val items = JSONObject(body).optJSONArray("data") ?: continue
                for (i in 0 until items.length()) {
                    val attributes = items.optJSONObject(i)?.optJSONObject("attributes") ?: continue
                    val titles = attributes.optJSONObject("titles")
                    val candidateTitles = listOf(
                        attributes.optString("canonicalTitle", ""),
                        titles?.optString("en", "").orEmpty(),
                        titles?.optString("en_jp", "").orEmpty(),
                        titles?.optString("ja_jp", "").orEmpty()
                    ).map { normalizeTitle(it) }.filter { it.isNotBlank() }

                    // Never replace the poster with an unrelated result from a fuzzy search.
                    val matches = candidateTitles.any { candidate ->
                        candidate in knownTitles || knownTitles.any { known ->
                            candidate.length >= 5 && known.length >= 5 &&
                                (candidate.contains(known) || known.contains(candidate))
                        }
                    }
                    if (!matches) continue

                    val poster = attributes.optJSONObject("posterImage") ?: continue
                    val candidate = poster.optString("original", "").ifBlank {
                        poster.optString("large", "")
                    }
                    if (candidate.startsWith("https://")) return@withContext candidate
                }
            } catch (_: Exception) {
                // Optional lookup must never block the details screen.
            }
        }
        null
    }

    suspend fun getAnimeWithEpisodes(animeId: String, forceRefresh: Boolean = false): AnimeWithEpisodes {
        val key = animeId.trim()
        if (!forceRefresh) detailsCache[key]?.let { return it }
        if (!key.all { it.isDigit() }) {
            val hits = search(key.replace("-", " "))
            if (hits.isNotEmpty()) {
                val resolved = detailsCache.getOrPut(hits.first().id) { loadDetails(hits.first().id, animeId) }
                detailsCache[key] = resolved
                return resolved
            }
        }
        return detailsCache.getOrPut(key) { loadDetails(key, key) }
    }

    private suspend fun loadDetails(animeId: String, cacheKey: String): AnimeWithEpisodes = withContext(Dispatchers.IO) {
        val body = httpGet(
            BASE_URL + "anime/get-anime-details?" +
                "anime_id=" + encode(animeId) +
                "&fetch_episodes=Yes" +
                "&more_info=Yes"
        )
        val parsed = parseAnimeDetails(body)
        parsed ?: AnimeWithEpisodes(Anime(id = cacheKey, title = cacheKey), emptyList())
    }

    // ---------------------------------------------------------- stream resolution

    data class ResolvedStreams(
        val streams: List<StreamOption>,
        val diagnostic: String? = null
    )

    suspend fun resolveStreams(animeId: String, episodeNumber: String): ResolvedStreams {
        val details = runCatching { getAnimeWithEpisodes(animeId) }
            .getOrElse { return ResolvedStreams(emptyList(), "تعذّر جلب بيانات الأنمي: ${it.message}") }
        val target = details.episodes.firstOrNull { it.number == episodeNumber.trim() }
            ?: details.episodes.lastOrNull()
            ?: return ResolvedStreams(emptyList(), "لا توجد حلقات متاحة لهذا الأنمي.")

        val servers: JSONArray = try {
            JSONArray(target.serversJson)
        } catch (_: JSONException) {
            JSONArray()
        }

        val cdnStreams = mutableListOf<StreamOption>()
        val multiUrls = mutableListOf<String>()
        val directUrls = mutableListOf<Pair<String, String>>() // label to url passthrough

        for (i in 0 until servers.length()) {
            val srv = servers.optJSONObject(i) ?: continue
            val name = srv.optString("episode_server_name", "").lowercase().trim()
            val url = srv.optString("episode_url", "").trim()
            if (url.isBlank()) continue
            when {
                name == "cdn" || url.contains("vq.php") || url.contains("v-qs.php") -> {
                    decryptCdnStream(url).forEach { cdnStreams.add(it) }
                }
                name == "muilt" || url.contains("/api/f") -> {
                    multiUrls.add(url)
                }
                else -> directUrls.add(name.uppercase() to url)
            }
        }

        val multiStreams = coroutineScope {
            val resolvedParts = multiUrls.map { url -> async(Dispatchers.IO) { resolveMultiServers(url) } }
            resolvedParts.awaitAll().flatten()
        }
        val combined = cdnStreams + multiStreams
        val sorted = combined.sortedByDescending { it.rank() }
        // de-duplicate by URL while keeping order
        val seen = HashSet<String>()
        val result = sorted.filter { it.url.isNotBlank() && seen.add(it.url) }
        return if (result.isEmpty()) ResolvedStreams(emptyList(), "لم يتم العثور على روابط مشاهدة لهذه الحلقة.")
        else ResolvedStreams(result)
    }

    data class CdnTokenParts(val f: String, val e: String)

    fun splitCdnUrl(url: String): CdnTokenParts? {
        val f = Regex("[?&]f=([^&]*)").find(url)?.groupValues?.get(1) ?: return null
        val e = Regex("[?&]e=([^&]*)").find(url)?.groupValues?.get(1) ?: ""
        if (f.isBlank()) return null
        return CdnTokenParts(f, e)
    }

    private fun buildNativeJson(): String = "{\"uhy\":\"com.anslayer\",\"dma\":47,\"mvd\":\"1.5.10\",\"vko\":\"$APK_CERT_SHA1\"}"

    /** Seed token: AES-ECB encrypt the native payload with (seed[0:10]+cert[0:22])[0:16]. */
    fun buildInfToken(seed: String): String {
        val key = (seed.take(10) + APK_CERT_SHA1.take(22)).take(16).toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        val encrypted = cipher.doFinal(buildNativeJson().toByteArray(Charsets.UTF_8))
        val b64 = java.util.Base64.getEncoder().encodeToString(encrypted)
        return JSONObject().apply {
            put("a", b64)
            put("b", seed)
        }.toString()
    }

    data class RncEnvelope(val keySalt: ByteArray, val iv: ByteArray, val ciphertext: ByteArray)

    /** Remote messages carry raw bytes; envelope format: auth(2)+salt(8)+1+16+... per RNCryptor v3. */
    fun parseRncEnvelope(bytes: ByteArray): RncEnvelope {
        if (bytes.size <= 66) throw ApiException("بيانات CDN غير صالحة.")
        return RncEnvelope(bytes.copyOfRange(2, 10), bytes.copyOfRange(18, 34), bytes.copyOfRange(34, bytes.size - 32))
    }

    fun decryptRncPayload(envelope: RncEnvelope): String {
        val spec = javax.crypto.spec.PBEKeySpec(
            RNC_PASSWORD.toCharArray(),
            envelope.keySalt,
            10000,
            256
        )
        val secretKeyFactory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val key = secretKeyFactory.generateSecret(spec).encoded
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(envelope.iv))
        val plain = cipher.doFinal(envelope.ciphertext)
        return String(plain, Charsets.UTF_8)
    }

    fun parseDecryptedStreams(jsonText: String): List<StreamOption> = try {
        val list = JSONArray(jsonText)
        buildList {
            for (i in 0 until list.length()) {
                val item = list.optJSONObject(i) ?: continue
                val file = item.optString("file", "").trim()
                if (file.isBlank() || !file.startsWith("http")) continue
                val (quality, label) = when {
                    file.contains("hh.mp4") -> "1080p" to "CDN > 1080p HQ"
                    file.contains("h.mp4") -> "1080p" to "CDN > 1080p"
                    file.contains("s.mp4") -> "720p" to "CDN > 720p"
                    file.contains("m.mp4") -> "480p" to "CDN > 480p"
                    else -> "Auto" to "CDN > Direct"
                }
                add(StreamOption(label = label, url = file, quality = quality))
            }
        }
    } catch (_: JSONException) {
        emptyList()
    }

    private suspend fun decryptCdnStream(cdnUrl: String): List<StreamOption> = withContext(Dispatchers.IO) {
        val parts = splitCdnUrl(cdnUrl) ?: return@withContext emptyList<StreamOption>()
        try {
            val seed = httpGet(BASE_URL + "google.php", timeout = 15).trim()
            if (seed.isBlank()) return@withContext emptyList<StreamOption>()
            val inf = buildInfToken(seed)
            val raw = httpPostJson(
                url = BASE_URL + "v-qs.php",
                form = mapOf("f" to parts.f, "e" to parts.e, "inf" to inf),
                timeout = 20
            ).trim()
            if (raw.isBlank()) return@withContext emptyList<StreamOption>()
            val envelopeBytes = java.util.Base64.getDecoder().decode(raw)
            val envelope = parseRncEnvelope(envelopeBytes)
            parseDecryptedStreams(decryptRncPayload(envelope))
        } catch (_: Exception) {
            emptyList<StreamOption>()
        }
    }

    fun multiUrlsFromSlug(slug: String): List<String> = listOf(
        "https://anslayer.com/la/public/api/f2?n=$slug",
        "https://anslayer.com/la/public/api/f?n=$slug",
        "https://a-reslayer.com/la/public/api/f?n=$slug",
        "https://a-reslayer.com/la/public/api/f2?n=$slug"
    )

    fun extractMultiSlug(url: String): String? = Regex("[?&]n=([^&]+)").find(url)?.groupValues?.get(1)

    suspend fun resolveMultiServers(multiUrl: String): List<StreamOption> = withContext(Dispatchers.IO) {
        val slug = extractMultiSlug(multiUrl) ?: return@withContext emptyList<StreamOption>()
        val allLinks = HashSet<String>()
        coroutineScope {
            multiUrlsFromSlug(slug).map { target ->
                async(Dispatchers.IO) {
                    try {
                        val body = httpGet(target, headers = mapOf("User-Agent" to UA), timeout = 8)
                        val data = JSONArray(body)
                        for (i in 0 until data.length()) {
                            val link = data.optString(i, "")
                            if (link.startsWith("http")) allLinks.add(link)
                        }
                    } catch (_: Exception) { /* source failure is tolerated; other slugs may still resolve */ }
                }
            }.awaitAll()
        }
        if (allLinks.isEmpty()) return@withContext emptyList<StreamOption>()

        coroutineScope {
            allLinks.filter { !isDeadHost(it) }.map { link ->
                async(Dispatchers.IO) {
                    extractThirdPartyDirect(link)
                }
            }.awaitAll()
        }.filterNotNull().sortedByDescending { it.rank() }
    }

    private fun isDeadHost(link: String): Boolean = listOf(
        "tune.pk", "vidlox.me", "jawcloud.co", "streamvid.net", "streamhub.ink", "highstream.tv"
    ).any { link.contains(it) }

    private suspend fun extractThirdPartyDirect(link: String): StreamOption? = when {
        link.contains("ok.ru") || link.contains("odnoklassniki") -> {
            val direct = extractOkruDirect(link)
            StreamOption("OK.ru > 720p", direct ?: link, if (direct != null) "720p" else "Auto")
        }
        link.contains("mp4upload") -> {
            val direct = extractMp4UploadDirect(link)
            StreamOption("Mp4Upload > ${direct?.let { "720p" } ?: "Direct"}", direct ?: link, "720p")
        }
        link.contains("goodstream") -> {
            val direct = extractGoodstreamDirect(link)
            StreamOption("Goodstream > Direct", direct ?: link, "Auto")
        }
        link.endsWith(".mp4") || link.contains(".mp4?") -> StreamOption("Direct MP4 > 720p", link, "720p")
        link.endsWith(".m3u8") || link.contains(".m3u8?") -> StreamOption("HLS > Direct", link, "Auto")
        else -> null
    }

    private suspend fun extractOkruDirect(link: String): String? = withContext(Dispatchers.IO) {
        try {
            val mid = Regex("/video/(\\d+)").find(link)?.groupValues?.get(1) ?: return@withContext null
            val html = httpGet("https://ok.ru/videoembed/$mid", headers = mapOf("User-Agent" to UA), timeout = 10)
            val options = Regex("data-options=[\"'](\\{.*?\\})[\"']").find(html)?.groupValues?.get(1) ?: return@withContext null
            val decoded = options
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
            val data = JSONObject(decoded)
            val videos = data.optJSONArray("videos") ?: return@withContext null
            if (videos.length() == 0) return@withContext null
            val last = videos.optJSONObject(videos.length() - 1)
            last?.optString("url", "")?.takeIf { it.startsWith("http") }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun extractMp4UploadDirect(link: String): String? = withContext(Dispatchers.IO) {
        try {
            val html = httpGet(link, headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"), timeout = 10)
            Regex("player\\.src\\(\\{\\s*type:\\s*[\"']video/mp4[\"'],\\s*src:\\s*[\"'](https?://[^\"]+)[\"']").find(html)?.groupValues?.get(1)
                ?: Regex("src:\\s*[\"'](https?://[^\"']+\\.mp4[^\"']*)[\"']").find(html)?.groupValues?.get(1)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun extractGoodstreamDirect(link: String): String? = withContext(Dispatchers.IO) {
        try {
            val html = httpGet(link, headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"), timeout = 10)
            Regex("[\"'](https?://[^\"']+\\.m3u8[^\"']*)[\"']").find(html)?.groupValues?.get(1)
        } catch (_: Exception) {
            null
        }
    }

    /** Range-probe the stream to make sure a real video body comes back before handing it to the player. */
    suspend fun verifyStream(url: String): Boolean = withContext(Dispatchers.IO) {
        if (!url.startsWith("http")) return@withContext false
        try {
            val builder = client.newBuilder().connectTimeout(6, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS)
            val request = Request.Builder().apply {
                header("User-Agent", UA)
                if (url.contains("ab-hunter")) header("Referer", "https://anslayer.com/")
            }.url(url).addHeader("Range", "bytes=0-1024").get().build()
            builder.build().newCall(request).execute().use { response ->
                if (response.code !in 200..299) return@use false
                val type = response.header("Content-Type", "").orEmpty().lowercase()
                (type.contains("video") || type.contains("octet-stream") || type.contains("mpegurl") || url.contains(".mp4") || url.contains(".m3u8"))
            }
        } catch (e: TimeoutCancellationException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    data class LatestScreenEpisode(
        val animeId: String,
        val animeTitle: String,
        val episodeNumber: String,
        val posterUrl: String? = null,
        val rating: String = ""
    )
}
