package dev.anicli.android

data class Anime(
    val id: String,
    val title: String,
    val posterUrl: String? = null,
    val description: String? = null,
    val animeType: String = "SERIES"
)

data class Episode(
    val number: String,
    val type: String = "Episode",
    val displayNumber: Double = 0.0
)

data class StreamInfo(
    val url: String,
    val quality: String
)
