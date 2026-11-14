package com.majstro.tv.spotify

import com.majstro.tv.domain.model.ClassicalWork
import com.majstro.tv.domain.model.PlaybackMode
import com.majstro.tv.domain.model.PlaybackStatus
import com.majstro.tv.domain.port.MusicPlayerGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.ConcurrentHashMap

/**
 * [MusicPlayerGateway] backed by Spotify Web API (Connect) when authenticated,
 * with a zero-config Demo Mode fallback so the quiz always runs cleanly out of the box.
 */
class SpotifyPlayerGateway(
    private val authStore: SpotifyAuthStore,
    private val wakeSpotifyApp: () -> Boolean = { false },
    private val httpClient: OkHttpClient = OkHttpClient()
) : MusicPlayerGateway {

    private val json = Json { ignoreUnknownKeys = true }
    private val uriCache = ConcurrentHashMap<String, String>()

    private val _status = MutableStateFlow(initialStatus())
    override val status: StateFlow<PlaybackStatus> = _status.asStateFlow()

    private fun initialStatus(): PlaybackStatus =
        if (authStore.isConfigured) {
            PlaybackStatus(
                mode = PlaybackMode.SPOTIFY_CONNECT,
                isPlaying = false,
                message = "Spotify linked — ready to play"
            )
        } else {
            PlaybackStatus(
                mode = PlaybackMode.DEMO,
                isPlaying = false,
                message = "Demo Mode (scan Spotify QR to connect Premium)"
            )
        }

    /** Refreshes the displayed status after OAuth completes on the phone setup page. */
    fun refreshStatusFromAuth() {
        _status.value = initialStatus()
    }

    /** Exchanges an OAuth PKCE authorization code for Spotify access/refresh tokens. */
    suspend fun exchangeCodeForToken(code: String, redirectUri: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val clientId = authStore.clientId
                ?: return@withContext Result.failure(IllegalStateException("Spotify Client ID not set"))
            val verifier = authStore.pendingCodeVerifier
                ?: return@withContext Result.failure(IllegalStateException("Missing PKCE verifier"))

            val body = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", redirectUri)
                .add("client_id", clientId)
                .add("code_verifier", verifier)
                .build()

            val request = Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .post(body)
                .build()

            runCatching {
                httpClient.newCall(request).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        error("Spotify token exchange failed (${response.code}): $raw")
                    }
                    val token = json.decodeFromString<SpotifyTokenResponse>(raw)
                    authStore.saveTokens(token.accessToken, token.refreshToken, token.expiresIn)
                    authStore.pendingCodeVerifier = null
                    refreshStatusFromAuth()
                }
            }
        }

    override suspend fun play(work: ClassicalWork): Result<Unit> = withContext(Dispatchers.IO) {
        if (!authStore.isConfigured) {
            _status.value = PlaybackStatus(
                mode = PlaybackMode.DEMO,
                isPlaying = true,
                message = "Demo Playing: ${work.styleOrForm} (${work.era.label})"
            )
            return@withContext Result.success(Unit)
        }

        val token = ensureValidToken()
        if (token == null) {
            _status.value = PlaybackStatus(
                mode = PlaybackMode.DEMO,
                isPlaying = true,
                message = "Spotify token expired — running in Demo Mode"
            )
            return@withContext Result.success(Unit)
        }

        val trackUri = resolveTrackUri(token, work)
        if (trackUri == null) {
            _status.value = PlaybackStatus(
                mode = PlaybackMode.SPOTIFY_CONNECT,
                isPlaying = false,
                message = "Track not found on Spotify: ${work.spotifyQuery}"
            )
            return@withContext Result.failure(IllegalStateException("Track not found"))
        }

        var device = findActiveOrFirstDevice(token)
        if (device == null) {
            wakeSpotifyApp()
            device = findActiveOrFirstDevice(token)
        }

        val playUrl = "https://api.spotify.com/v1/me/player/play".toHttpUrl().newBuilder().apply {
            if (device != null) addQueryParameter("device_id", device.id)
        }.build()

        val payload = buildJsonObject {
            put("uris", buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive(trackUri)) })
            put("position_ms", work.startPositionMs)
        }.toString()

        val request = Request.Builder()
            .url(playUrl)
            .header("Authorization", "Bearer $token")
            .put(payload.toRequestBody("application/json".toMediaType()))
            .build()

        runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string().orEmpty()
                    _status.value = PlaybackStatus(
                        mode = PlaybackMode.SPOTIFY_CONNECT,
                        isPlaying = false,
                        activeDeviceName = device?.name,
                        message = "Open Spotify on tablet/phone first (${response.code})"
                    )
                    error("Spotify play failed (${response.code}): $err")
                }
                _status.value = PlaybackStatus(
                    mode = PlaybackMode.SPOTIFY_CONNECT,
                    isPlaying = true,
                    activeDeviceName = device?.name ?: "Spotify Connect",
                    message = "Playing on ${device?.name ?: "Spotify Connect"}"
                )
            }
        }
    }

    override suspend fun pause(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!authStore.isConfigured) {
            _status.value = PlaybackStatus(
                mode = PlaybackMode.DEMO,
                isPlaying = false,
                message = "Demo paused"
            )
            return@withContext Result.success(Unit)
        }

        val token = ensureValidToken() ?: return@withContext Result.success(Unit)
        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me/player/pause")
            .header("Authorization", "Bearer $token")
            .put("".toRequestBody("application/json".toMediaType()))
            .build()

        runCatching {
            httpClient.newCall(request).execute().use { response ->
                _status.value = _status.value.copy(
                    isPlaying = false,
                    message = if (response.isSuccessful) "Paused" else "Ready"
                )
            }
        }
    }

    private fun ensureValidToken(): String? {
        val current = authStore.accessToken ?: return null
        if (!authStore.isTokenExpired()) return current

        val refresh = authStore.refreshToken ?: return null
        val clientId = authStore.clientId ?: return null

        val body = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refresh)
            .add("client_id", clientId)
            .build()

        val request = Request.Builder()
            .url("https://accounts.spotify.com/api/token")
            .post(body)
            .build()

        return runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val raw = response.body?.string().orEmpty()
                val token = json.decodeFromString<SpotifyTokenResponse>(raw)
                authStore.saveTokens(token.accessToken, token.refreshToken, token.expiresIn)
                token.accessToken
            }
        }.getOrNull()
    }

    private fun resolveTrackUri(token: String, work: ClassicalWork): String? {
        uriCache[work.id]?.let { return it }

        val searchUrl = "https://api.spotify.com/v1/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", work.spotifyQuery)
            .addQueryParameter("type", "track")
            .addQueryParameter("limit", "1")
            .build()

        val request = Request.Builder()
            .url(searchUrl)
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        return runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val raw = response.body?.string().orEmpty()
                val parsed = json.decodeFromString<SpotifySearchResponse>(raw)
                parsed.tracks.items.firstOrNull()?.uri?.also { uri ->
                    uriCache[work.id] = uri
                }
            }
        }.getOrNull()
    }

    private fun findActiveOrFirstDevice(token: String): SpotifyDevice? {
        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me/player/devices")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        return runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val raw = response.body?.string().orEmpty()
                val devices = json.decodeFromString<SpotifyDevicesResponse>(raw).devices
                devices.firstOrNull { it.isActive } ?: devices.firstOrNull()
            }
        }.getOrNull()
    }

    @Serializable
    private data class SpotifyTokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String? = null,
        @SerialName("expires_in") val expiresIn: Long
    )

    @Serializable
    private data class SpotifySearchResponse(val tracks: SpotifyTrackList)

    @Serializable
    private data class SpotifyTrackList(val items: List<SpotifyTrackItem> = emptyList())

    @Serializable
    private data class SpotifyTrackItem(val uri: String)

    @Serializable
    private data class SpotifyDevicesResponse(val devices: List<SpotifyDevice> = emptyList())

    @Serializable
    private data class SpotifyDevice(
        val id: String,
        val name: String,
        @SerialName("is_active") val isActive: Boolean = false
    )
}
