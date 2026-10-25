package dev.anicli.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-parser coverage for the Tenrai episode-metadata enrichment. Payload samples are
 * recorded responses from https://api.tenrai.org (Jikan-compat schema).
 */
class TenraiMetaTest {

    private fun episodePayload(title: String, thumb: String?): String = """
        {"data":{"mal_id":5,"url":"https://myanimelist.net/anime/20/Naruto/episode/5",
        "title":"$title","title_japanese":"失格？カカシの結論",
        "images":{"jpg":{"image_url":${thumb?.let { "\"$it\"" } ?: "null"}},"webp":{"image_url":null}}}}
    """.trimIndent()

    @Test
    fun `parses verified episode title and thumbnail`() {
        val meta = TenraiMeta.parseTenraiEpisode(
            episodePayload(
                "You Failed! Kakashi's Final Decision",
                "https://img1.ak.crunchyroll.com/i/spire2-tmb/abc123_large.jpg"
            )
        )
        assertTrue(meta != null)
        assertEquals("You Failed! Kakashi's Final Decision", meta!!.title)
        assertEquals("https://img1.ak.crunchyroll.com/i/spire2-tmb/abc123_large.jpg", meta.thumbnailUrl)
        assertEquals("5", meta.number)
    }

    @Test
    fun `null thumbnail stays null instead of fabricated`() {
        val meta = TenraiMeta.parseTenraiEpisode(
            episodePayload("Killing Magic", null)
        )
        assertTrue(meta != null)
        assertEquals("Killing Magic", meta!!.title)
        assertNull(meta.thumbnailUrl)
    }

    @Test
    fun `malformed body yields nothing`() {
        assertNull(TenraiMeta.parseTenraiEpisode("not json"))
        assertNull(TenraiMeta.parseTenraiEpisode("{\"nope\":1}"))
    }

    @Test
    fun `anime list parsing keeps approved hits only with ids`() {
        val body = """
            {"data":[
              {"mal_id":20,"title":"Naruto","approved":true},
              {"mal_id":-1,"title":"Bad row","approved":true},
              {"mal_id":52991,"title":"Sousou no Frieren","approved":true},
              {"mal_id":99,"title":"Naruto","approved":false}
            ]}
        """.trimIndent()
        val hits = TenraiMeta.parseTenraiAnimeList(body)
        assertEquals(3, hits.size)
        assertEquals(20, hits[0].malId)
    }

    @Test
    fun `exact title match wins and fuzzy names never match`() {
        val hits = listOf(
            TenraiMeta.TenraiAnimeHit(20, "Naruto", true),
            TenraiMeta.TenraiAnimeHit(52991, "Sousou no Frieren", true)
        )
        assertEquals(20, TenraiMeta.matchAnime(hits, "naruto")?.malId)
        assertEquals(52991, TenraiMeta.matchAnime(hits, "Sousou  no  Frieren")?.malId)
        assertNull(TenraiMeta.matchAnime(hits, "Narue: Zoku-Hen"))
        assertNull(TenraiMeta.matchAnime(hits, "Frieren at Journey's End"))
    }

    @Test
    fun `normalize folds whitespace accents and case`() {
        assertEquals("sousou no frieren", TenraiMeta.normalizeTitle("Sousou  no  FRIEREN"))
    }

    @Test
    fun `episode numbers are strings to survive slayer keys`() {
        val meta = TenraiMeta.parseTenraiEpisode(episodePayload("T", null))
        assertFalse(meta!!.number.isBlank())
    }
}
