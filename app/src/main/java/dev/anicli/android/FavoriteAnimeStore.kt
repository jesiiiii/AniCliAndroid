package dev.anicli.android

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Stores favorite anime metadata locally on this device. */
object FavoriteAnimeStore {
    private const val PREFS = "uchiha_local_library"
    private const val KEY = "favorites"

    fun getAll(context: Context): List<Anime> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val id = item.optString("id").trim()
                    val title = item.optString("title").trim()
                    if (id.isNotBlank() && title.isNotBlank()) {
                        add(Anime(
                            id = id,
                            title = title,
                            titleEn = item.optString("titleEn"),
                            posterUrl = item.optString("posterUrl").takeIf { it.startsWith("https://") },
                            animeType = item.optString("animeType", "مسلسل"),
                            rating = item.optString("rating"),
                            genres = item.optString("genres")
                        ))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun contains(context: Context, animeId: String): Boolean =
        getAll(context).any { it.id == animeId }

    fun toggle(context: Context, anime: Anime): Boolean {
        val favorites = getAll(context).toMutableList()
        val wasFavorite = favorites.any { it.id == anime.id }
        if (wasFavorite) favorites.removeAll { it.id == anime.id } else favorites.add(0, anime)
        val array = JSONArray()
        favorites.take(200).forEach { animeItem ->
            array.put(JSONObject().apply {
                put("id", animeItem.id)
                put("title", animeItem.title)
                put("titleEn", animeItem.titleEn)
                put("posterUrl", animeItem.posterUrl ?: "")
                put("animeType", animeItem.animeType)
                put("rating", animeItem.rating)
                put("genres", animeItem.genres)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, array.toString()).apply()
        return !wasFavorite
    }
}
