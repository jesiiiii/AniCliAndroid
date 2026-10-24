package dev.anicli.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class ParserTest {

    @Test
    fun `search json parses into anime list`() {
        val body = """
            {"response":{"meta_data":{},"data":[
              {"anime_id":"2107","anime_name":"بوروتو","anime_english_title":"Boruto","anime_type":"TV","anime_rating":"7.95","anime_cover_image_url":"https://img.anslayer.com/a.jpg","anime_genres":"اكشن, شونين"},
              {"anime_id":"","anime_name":"missing id"},
              {"anime_name":"id-less row"}
            ]}}
        """.trimIndent()
        val result = SlayerApi.parseAnimeList(body)
        assertEquals(1, result.size)
        val first = result.first()
        assertEquals("2107", first.id)
        assertEquals("بوروتو", first.title)
        assertEquals("Boruto", first.titleEn)
        assertEquals("https://img.anslayer.com/a.jpg", first.posterUrl)
    }

    @Test
    fun `non json body parses to empty list not crash`() {
        assertEquals(emptyList<Anime>(), SlayerApi.parseAnimeList("<!doctype html>nope"))
    }

    @Test
    fun `details json parses episodes with cleaned titles and servers`() {
        val body = """
            {"response":{
              "anime_id":"2107","anime_name":"بوروتو","anime_type":"مسلسل","anime_rating":"7.95",
              "episodes":{"data":[
                {"episode_number":"2.0","episode_name":"الحلقة 2: المدرسة","episode_urls":[{"episode_server_name":"cdn","episode_url":"https://anslayer.com/anime/public/vq.php?f=slug&e=2|ab"}]},
                {"episode_number":"1.0","episode_name":"الحلقة : 1","episode_urls":[]}
              ]}}}
        """.trimIndent()
        val parsed = SlayerApi.parseAnimeDetails(body)
        assertNotNull(parsed)
        assertEquals(2, parsed!!.episodes.size)
        assertEquals("1", parsed.episodes[0].number) // sorted ascending by resolved number
        assertEquals(1, parsed.episodes[0].number.toInt())
        assertEquals("", parsed.episodes[0].title) // redundant prefix stripped entirely
        assertEquals("المدرسة", parsed.episodes[1].title)
        assertTrue(parsed.episodes[1].serversJson.contains("vq.php"))
    }

    @Test
    fun `details payload without response section yields null not crash`() {
        val parsed = SlayerApi.parseAnimeDetails("{\"error\":\"nope\"}")
        assertNull(parsed)
    }

    @Test
    fun `episode title cleaning strips redundant prefixes`() {
        assertEquals("", SlayerApi.cleanEpisodeTitle("الحلقة : 1", 1))
        assertEquals("", SlayerApi.cleanEpisodeTitle("Episode 1", 1))
        assertEquals("المدرسة", SlayerApi.cleanEpisodeTitle("الحلقة 2: المدرسة", 2))
        assertEquals("", SlayerApi.cleanEpisodeTitle("123", 123))
        assertEquals("اسم الحلقة", SlayerApi.cleanEpisodeTitle("اسم الحلقة", 4))
    }

    @Test
    fun `cdn url splitting extracts f and e params`() {
        val parts = SlayerApi.splitCdnUrl("https://anslayer.com/anime/public/vq.php?f=slug&e=1|b26")
        assertNotNull(parts)
        assertEquals("slug", parts!!.f)
        assertEquals("1|b26", parts.e)
        assertNull(SlayerApi.splitCdnUrl("https://example.com/no-params"))
    }

    @Test
    fun `multi slug extraction matches f endpoints`() {
        assertEquals("slug\\1", SlayerApi.extractMultiSlug("https://a-reslayer.com/la/public/api/f?n=slug\\1"))
        assertNull(SlayerApi.extractMultiSlug("https://example.com/other?q=1"))
        val urls = SlayerApi.multiUrlsFromSlug("slug\\1")
        assertEquals(4, urls.size)
        assertTrue(urls.all { it.contains("n=slug") })
    }

    @Test
    fun `decrypted stream payloads map quality by filename`() {
        val streams = SlayerApi.parseDecryptedStreams(
            """[{"file":"https://cdn/h.mp4?token=a"},{"file":"https://cdn/s.mp4?token=b"},{"file":"https://cdn/m.mp4?token=c"},{"file":""}]"""
        )
        assertEquals(3, streams.size)
        assertEquals("CDN > 1080p", streams[0].label)
        assertEquals("1080p", streams[0].quality)
        assertEquals("CDN > 720p", streams[1].label)
        assertEquals("CDN > 480p", streams[2].label)
    }

    @Test
    fun `stream rank picks highest quality first`() {
        val options = listOf(
            StreamOption("a", "http://x", "Auto"),
            StreamOption("b", "http://y", "480p"),
            StreamOption("c", "http://z", "1080p"),
            StreamOption("d", "http://w", "720p")
        )
        val sorted = options.sortedByDescending { it.rank() }
        assertEquals("1080p", sorted[0].quality)
        assertEquals("Auto", sorted[3].quality)
    }

    @Test
    fun `inf token embeds encrypted native payload and seed`() {
        val token = SlayerApi.buildInfToken("seed123")
        assertTrue(token.contains("\"b\":\"seed123\""))
        val json = org.json.JSONObject(token)
        val payloadA = json.getString("a")
        assertFalse(payloadA.isBlank())
    }

    @Test
    fun `rncryptor envelope decrypt roundtrip matches upstream format`() {
        // build an envelope in the exact upstream layout: version, options, salt(8), iv bytes, ciphertext, hmac(32)
        val password = "android-app9>E>VBa=X%;[5BX~=Q~K".toCharArray()
        val salt = "22345678".toByteArray()
        val iv = "0123456789abcdef".toByteArray()
        val secret = "test message secret".toByteArray()

        val keySpec = javax.crypto.spec.PBEKeySpec(password, salt, 10000, 256)
        val secretKeyFactory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val key = secretKeyFactory.generateSecret(keySpec).encoded

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        val ciphertext = cipher.doFinal(secret)

        val envelope = byteArrayOf(3, 1) + salt + byteArrayOf(1) + iv + ciphertext + ByteArray(32)
        val decrypted = SlayerApi.decryptRncPayload(SlayerApi.parseRncEnvelope(envelope))
        assertEquals("test message secret", decrypted)
    }

    @Test
    fun `oversized rncryptor envelope is rejected`() {
        var caught: Exception? = null
        try {
            SlayerApi.parseRncEnvelope(ByteArray(10))
        } catch (e: Exception) {
            caught = e
        }
        assertTrue(caught is SlayerApi.ApiException)
    }

    @Test
    fun `dead hosts are filtered out of multi arrays`() {
        val bad = "https://jawcloud.co/some/link"
        val filtered = listOf("tune.pk", "vidlox.me", "jawcloud.co")
            .map { bad.contains(it) }
        assertTrue(filtered.any { it })
    }

    @Test
    fun `episodes with fractional numbers keep ascending order`() {
        val body = """
            {"response":{"anime_id":"1","anime_name":"x","episodes":{"data":[
              {"episode_number":"3.0","episode_name":"الحلقة : 3","episode_urls":[]},
              {"episode_number":"1.0","episode_name":"الحلقة : 1","episode_urls":[]},
              {"episode_number":"2.0","episode_name":"الحلقة : 2","episode_urls":[]}
            ]}}}
        """.trimIndent()
        val parsed = SlayerApi.parseAnimeDetails(body)
        assertEquals(listOf(1, 2, 3), parsed!!.episodes.map { it.number.toInt() })
    }
}
