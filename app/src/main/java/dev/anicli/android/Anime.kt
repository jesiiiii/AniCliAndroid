package dev.anicli.android

data class Anime(
    val id: String,
    val title: String,
    val titleEn: String = "",
    val posterUrl: String? = null,
    val description: String? = null,
    val animeType: String = "مسلسل",
    val rating: String = "",
    val genres: String = "",
    val latestEpisode: String? = null
)

data class AnimeWithEpisodes(
    val anime: Anime,
    val episodes: List<Episode>
)

data class Episode(
    val number: String,
    val title: String = "",
    val serversJson: String = ""
)

data class StreamOption(
    val label: String,
    val url: String,
    val quality: String
) {
    fun isHls(): Boolean = url.contains(".m3u8")
    fun rank(): Int = when {
        quality == "1080p" -> 5
        quality == "720p" -> 4
        quality == "480p" -> 3
        quality == "360p" -> 2
        else -> 1
    }
}
