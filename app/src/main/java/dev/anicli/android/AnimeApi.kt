package dev.anicli.android

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** All source credentials stay on the HTTPS backend, never in the APK. */
object AnimeApi {
    private const val BASE_URL = "https://anicli-secure-api-lb55v9.v2.appdeploy.ai"
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS)
        .build()

    private fun encode(value: String) = java.net.URLEncoder.encode(value.trim(), "UTF-8")

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
                    root.optString("diagnostic").ifBlank { "فشل الطلب (HTTP ${response.code})." }
                }
                throw IOException(message)
            }
            root
        }
    }

    suspend fun search(query: String): List<Anime> {
        val root = getJson("/api/search?q=${encode(query)}")
        val array = root.optJSONArray("results") ?: JSONArray()
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
        val root = getJson("/api/episodes?animeId=${encode(animeId)}")
        val array = root.optJSONArray("episodes") ?: JSONArray()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val number = item.optString("number").takeIf { it.isNotBlank() } ?: continue
                add(
                    Episode(
                        number = number,
                        type = item.optString("type", "Episode"),
                        displayNumber = item.optDouble("displayNumber", i + 1.0)
                    )
                )
            }
        }
    }

    suspend fun resolveStream(
        animeId: String,
        episode: String,
        animeType: String = "SERIES",
        quality: String = "auto"
    ): StreamInfo {
        val path = "/api/stream?animeId=${encode(animeId)}" +
            "&episode=${encode(episode)}&type=${encode(animeType)}&quality=${encode(quality)}"
        val root = getJson(path)
        val url = root.optString("streamUrl").takeIf { it.startsWith("https://") }
            ?: throw IOException("لم يُرجع الخادم رابط فيديو صالحاً.")
        return StreamInfo(url = url, quality = root.optString("quality", "auto"))
    }
}
