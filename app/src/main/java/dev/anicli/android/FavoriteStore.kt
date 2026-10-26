package dev.anicli.android

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/**
 * Small local favorites store. Favorites are kept on-device and survive app restarts.
 * The stored records include enough metadata to render the favorites screen offline.
 */
internal object FavoriteStore {
    private const val PREFS = "uchiha_anime_favorites"
    private const val KEY_ITEMS = "items"
    private var items by mutableStateOf<List<Anime>>(emptyList())
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val raw = context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_ITEMS, "[]")
                .orEmpty()
            val parsed = mutableListOf<Anime>()
            try {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val o = array.optJSONObject(i) ?: continue
                    val id = o.optString("id")
                    if (id.isBlank()) continue
                    parsed += Anime(
                        id = id,
                        title = o.optString("title"),
                        titleEn = o.optString("titleEn"),
                        posterUrl = o.optString("posterUrl").takeIf { it.isNotBlank() },
                        description = o.optString("description").takeIf { it.isNotBlank() },
                        animeType = o.optString("animeType", "مسلسل"),
                        rating = o.optString("rating"),
                        genres = o.optString("genres"),
                        latestEpisode = o.optString("latestEpisode").takeIf { it.isNotBlank() }
                    )
                }
            } catch (_: Exception) {
                // A malformed old preference should never prevent the app from opening.
            }
            items = parsed
            loaded = true
        }
    }

    fun all(context: Context): List<Anime> {
        load(context)
        return items
    }

    fun isFavorite(context: Context, animeId: String): Boolean {
        load(context)
        return items.any { it.id == animeId }
    }

    fun toggle(context: Context, anime: Anime): Boolean {
        load(context)
        val exists = items.any { it.id == anime.id }
        items = if (exists) items.filterNot { it.id == anime.id } else listOf(anime) + items
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("titleEn", item.titleEn)
                    .put("posterUrl", item.posterUrl.orEmpty())
                    .put("description", item.description.orEmpty())
                    .put("animeType", item.animeType)
                    .put("rating", item.rating)
                    .put("genres", item.genres)
                    .put("latestEpisode", item.latestEpisode.orEmpty())
            )
        }
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ITEMS, array.toString()).apply()
        return !exists
    }
}
