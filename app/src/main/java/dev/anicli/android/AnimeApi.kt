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

/**
 * Talks only to the project's HTTPS backend proxy.
 * Upstream source credentials must remain on that backend, never in the APK.
 */
object AnimeApi {
    private const val BASE_URL = "https://anicli-secure-api-lzhrek.v2.appdeploy.ai"
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun search(query: String): List<Anime> = withContext(Dispatchers.IO) {
        val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
        val request = Request.Builder()
            .url("$BASE_URL/api/search?q=$encoded")
            .header("Accept", "application/json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val contentType = response.header("Content-Type").orEmpty()

            if (!response.isSuccessful) {
                val diagnostic = try {
                    JSONObject(body).optString("diagnostic").ifBlank {
                        JSONObject(body).optString("message")
                    }
                } catch (_: JSONException) {
                    body.take(120).replace(Regex("\\s+"), " ")
                }
                val detail = diagnostic.takeIf { it.isNotBlank() }
                throw IOException(
                    buildString {
                        append("تعذّر البحث (HTTP ${response.code}).")
                        if (detail != null) append(" التفاصيل: ").append(detail)
                    }
                )
            }

            if (body.isBlank()) throw IOException("استجابة الخادم فارغة.")
            if (!contentType.contains("application/json", ignoreCase = true) &&
                !body.trimStart().startsWith("{")
            ) {
                val preview = body.take(100).replace(Regex("\\s+"), " ")
                throw IOException(
                    "الخادم أعاد محتوى غير JSON (Content-Type: ${contentType.ifBlank { "غير محدد" }}). " +
                        "أول الاستجابة: $preview"
                )
            }

            val root = try {
                JSONObject(body)
            } catch (_: JSONException) {
                throw IOException("استجابة البحث ليست JSON صالحاً.")
            }
            val apiError = root.optString("message")
            if (root.has("error")) {
                throw IOException(apiError.ifBlank { "تعذّر جلب النتائج من المصدر." })
            }

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
