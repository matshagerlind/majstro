package com.majstro.tv.spotify

import com.majstro.tv.domain.model.ClassicalWork
import com.majstro.tv.domain.model.MusicalEra
import com.majstro.tv.domain.model.PlaybackMode
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifySessionTest {

    private class InMemoryKeyValueStore : KeyValueStore {
        private val strings = mutableMapOf<String, String>()
        private val longs = mutableMapOf<String, Long>()

        override fun getString(key: String): String? = strings[key]

        override fun putString(key: String, value: String?) {
            if (value == null) strings.remove(key) else strings[key] = value
        }

        override fun getLong(key: String, default: Long): Long = longs[key] ?: default

        override fun putLong(key: String, value: Long) {
            longs[key] = value
        }
    }

    private fun fakeHttpClient(handler: (Request) -> Pair<Int, String>): OkHttpClient {
        val interceptor = Interceptor { chain ->
            val request = chain.request()
            val (code, body) = handler(request)
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(if (code in 200..299) "OK" else "Error")
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
        }
        return OkHttpClient.Builder().addInterceptor(interceptor).build()
    }

    private val sampleWork = ClassicalWork(
        id = "bach-brandenburg-3",
        title = "Brandenburg Concerto No. 3",
        composer = "J.S. Bach",
        era = MusicalEra.BAROQUE,
        styleOrForm = "Concerto Grosso",
        yearWritten = 1721,
        stylisticClue = "Driving string counterpoint",
        spotifyQuery = "Bach Brandenburg Concerto No 3"
    )

    @Test
    fun `unconfigured session starts in Demo Mode and plays in Demo Mode`() = runTest {
        val session = SpotifySession(store = InMemoryKeyValueStore())

        assertFalse(session.isConnected)
        assertEquals(PlaybackMode.DEMO, session.status.value.mode)
        assertFalse(session.status.value.isPlaying)

        val result = session.play(sampleWork)
        assertTrue(result.isSuccess)
        assertEquals(PlaybackMode.DEMO, session.status.value.mode)
        assertTrue(session.status.value.isPlaying)

        session.pause()
        assertFalse(session.status.value.isPlaying)
    }

    @Test
    fun `beginAuth, completeAuth, and disconnect drive OAuth and playback status`() = runTest {
        val store = InMemoryKeyValueStore()
        val client = fakeHttpClient { request ->
            if (request.url.encodedPath == "/api/token") {
                200 to """{"access_token":"acc-1","refresh_token":"ref-1","expires_in":3600}"""
            } else {
                404 to "{}"
            }
        }
        val session = SpotifySession(store = store, httpClient = client)

        val authUrl = session.beginAuth("  my-client-id  ", "http://192.168.1.10:8080/spotify/callback")
        assertEquals("my-client-id", session.clientId)
        assertTrue(authUrl.startsWith("https://accounts.spotify.com/authorize?"))
        assertTrue(authUrl.contains("client_id=my-client-id"))
        assertTrue(authUrl.contains("code_challenge_method=S256"))

        val authResult = session.completeAuth("auth-code-123", "http://192.168.1.10:8080/spotify/callback")
        assertTrue(authResult.isSuccess)
        assertTrue(session.isConnected)
        assertEquals(PlaybackMode.SPOTIFY_CONNECT, session.status.value.mode)
        assertFalse(session.status.value.isPlaying)

        session.disconnect()
        assertFalse(session.isConnected)
        assertEquals(PlaybackMode.DEMO, session.status.value.mode)
    }

    @Test
    fun `play refreshes expired token and starts Spotify Connect playback`() = runTest {
        val store = InMemoryKeyValueStore()
        var refreshed = false
        val client = fakeHttpClient { request ->
            when (request.url.encodedPath) {
                "/api/token" -> {
                    refreshed = true
                    200 to """{"access_token":"fresh-token","expires_in":3600}"""
                }
                "/v1/search" -> 200 to """{"tracks":{"items":[{"uri":"spotify:track:123"}]}}"""
                "/v1/me/player/devices" -> 200 to """{"devices":[{"id":"dev-1","name":"Living Room","is_active":true}]}"""
                "/v1/me/player/play" -> {
                    assertEquals("Bearer fresh-token", request.header("Authorization"))
                    204 to ""
                }
                else -> 404 to "{}"
            }
        }

        // Seed an expired token directly via KeyValueStore
        store.putString("spotify_client_id", "my-client-id")
        store.putString("spotify_access_token", "expired-token")
        store.putString("spotify_refresh_token", "ref-1")
        store.putLong("spotify_expires_at", 1L)

        val session = SpotifySession(store = store, httpClient = client)
        val result = session.play(sampleWork)

        assertTrue(result.isSuccess)
        assertTrue(refreshed)
        assertEquals(PlaybackMode.SPOTIFY_CONNECT, session.status.value.mode)
        assertTrue(session.status.value.isPlaying)
        assertEquals("Living Room", session.status.value.activeDeviceName)
    }
}
