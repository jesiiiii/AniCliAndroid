package dev.anicli.android

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** The upstream credentials stay server-side; the APK contains only the public API URL. */
object AnimeApi {
    // This deployment has the configured server-side secret. The older lb55v9 deployment does not.
    private const val BASE_URL = "https://anicli-secure-api-lzhrek.v2.appdeploy.ai"
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS)
        .build()

    private fun encode(value: String) = URLEncoder.encode(value.trim(), "UTF-8")

    private suspend fun getJson(path: String): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL$path")
            .header("Accept", "application/json")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val root = try {
                JSONObject(body)
            } catch (_: JSONException) {
                throw IOException("استجابة الخادم ليست JSON صالحاً (HTTP ${response.code}).")
            }
            if (!response.isSuccessful || root.has("error")) {
                val message = root.optString("message").ifBlank {
                    "فشل الطلب (HTTP ${response.code})."
                }
                throw IOException(message)
            }
            root
        }
    }

    suspend fun search(query: String): List<Anime> {
        if (query.isBlank()) throw IOException("اكتب اسم الأنمي أولاً.")
        val root = getJson("/api/search?q=${encode(query)}")
        val array = root.optJSONArray("results")
            ?: throw IOException("صيغة نتائج البحث غير متوافقة مع التطبيق.")
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                val title = item.optString("title").takeIf { it.isNotBlank() } ?: continue
                add(
                    Anime(
                        id = id,
                        title = title,
                        posterUrl = item.optString("posterUrl").takeIf { it.startsWith("https://") },
                        description = item.optString("description").takeIf { it.isNotBlank() },
                        animeType = item.optString("animeType", "SERIES").uppercase()
                    )
                )
            }
        }
    }

    suspend fun getEpisodes(animeId: String): List<Episode> {
        if (animeId.isBlank()) throw IOException("معرّف الأنمي غير صالح.")
        val root = getJson("/api/episodes?animeId=${encode(animeId)}")
        val array = root.optJSONArray("episodes")
            ?: throw IOException("صيغة قائمة الحلقات غير متوافقة مع التطبيق.")
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val number = item.optString("number").takeIf { it.isNotBlank() } ?: continue
                add(Episode(number, item.optString("type", "Episode"), item.optDouble("displayNumber", i + 1.0)))
            }
        }
    }

    suspend fun resolveStream(
        animeId: String,
        episode: String,
        animeType: String = "SERIES",
        quality: String = "auto"
    ): StreamInfo {
        if (animeId.isBlank() || episode.isBlank()) throw IOException("بيانات الحلقة غير مكتملة.")
        val path = "/api/stream?animeId=${encode(animeId)}&episode=${encode(episode)}&type=${encode(animeType)}&quality=${encode(quality)}"
        val root = getJson(path)
        val url = root.optString("streamUrl").takeIf { it.startsWith("https://") }
            ?: throw IOException("لم يُرجع الخادم رابط فيديو HTTPS صالحاً.")
        return StreamInfo(url = url, quality = root.optString("quality", "auto"))
    }
}
