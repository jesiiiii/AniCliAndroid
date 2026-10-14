package dev.anicli.android

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Anime(
    val id: String,
    val title: String,
    val posterUrl: String? = null,
    val description: String? = null
)

@Serializable
data class SearchResponse(
    val results: List<Anime> = emptyList()
)
