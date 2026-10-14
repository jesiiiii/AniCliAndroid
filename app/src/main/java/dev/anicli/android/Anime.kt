package dev.anicli.android

data class Anime(
    val id: String,
    val title: String,
    val posterUrl: String? = null,
    val description: String? = null
)
