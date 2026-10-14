package dev.anicli.android

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Talks only to the project's HTTPS backend proxy.
 * Upstream source credentials must remain on that backend, never in the APK.
 */
object AnimeApi {
    // Set this to the deployed HTTPS proxy base URL before expecting live results.
    private const val BASE_URL = "https://YOUR-SECURE-PROXY.example/"
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun search(query: String): List<Anime> = withContext(Dispatchers.IO) {
        if (BASE_URL.contains("YOUR-SECURE-PROXY")) {
            throw IOException("لم يُضبط عنوان خادم البحث بعد.")
        }
        val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
        val request = Request.Builder()
            .url(BASE_URL.trimEnd('/') + "/api/search?q=" + encoded)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("تعذّر البحث (HTTP ${response.code}).")
            val body = response.body?.string() ?: throw IOException("استجابة الخادم فارغة.")
            val root = JSONObject(body)
            val array = root.optJSONArray("results") ?: JSONArray()
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val title = item.optString("title").takeIf { it.isNotBlank() } ?: continue
                    add(
                        Anime(
                            id = id,
                            title = title,
                            posterUrl = item.optString("posterUrl").takeIf { it.startsWith("https://") },
                            description = item.optString("description").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }
    }
}
