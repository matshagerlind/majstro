package com.majstro.tv.spotify

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** Minimal persistence abstraction so [SpotifyAuthStore] can run in unit tests without Android Context. */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
    fun getLong(key: String, default: Long = 0L): Long
    fun putLong(key: String, value: Long)
}

/** [KeyValueStore] backed by Android [SharedPreferences]. */
class SharedPrefsKeyValueStore(context: Context) : KeyValueStore {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("majstro_spotify_auth", Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String?) {
        prefs.edit().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }

    override fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)

    override fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }
}

/** Stores Spotify OAuth 2.0 PKCE state, Client ID, and active access/refresh tokens. */
internal class SpotifyAuthStore(private val store: KeyValueStore) {

    var clientId: String?
        get() = store.getString(KEY_CLIENT_ID)?.takeIf { it.isNotBlank() }
        set(value) = store.putString(KEY_CLIENT_ID, value?.trim())

    var accessToken: String?
        get() = store.getString(KEY_ACCESS_TOKEN)?.takeIf { it.isNotBlank() }
        set(value) = store.putString(KEY_ACCESS_TOKEN, value)

    var refreshToken: String?
        get() = store.getString(KEY_REFRESH_TOKEN)?.takeIf { it.isNotBlank() }
        set(value) = store.putString(KEY_REFRESH_TOKEN, value)

    var expiresAtEpochSec: Long
        get() = store.getLong(KEY_EXPIRES_AT, 0L)
        set(value) = store.putLong(KEY_EXPIRES_AT, value)

    var pendingCodeVerifier: String?
        get() = store.getString(KEY_CODE_VERIFIER)
        set(value) = store.putString(KEY_CODE_VERIFIER, value)

    val isConfigured: Boolean
        get() = clientId != null && accessToken != null

    fun isTokenExpired(nowEpochSec: Long = System.currentTimeMillis() / 1000L): Boolean =
        expiresAtEpochSec > 0L && nowEpochSec >= (expiresAtEpochSec - 60L)

    /** Generates a fresh PKCE code verifier and SHA-256 S256 challenge. */
    fun createPkceChallenge(): String {
        val bytes = ByteArray(64)
        SecureRandom().nextBytes(bytes)
        val verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        pendingCodeVerifier = verifier
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    fun saveTokens(accessToken: String, refreshToken: String?, expiresInSeconds: Long) {
        this.accessToken = accessToken
        if (!refreshToken.isNullOrBlank()) {
            this.refreshToken = refreshToken
        }
        this.expiresAtEpochSec = (System.currentTimeMillis() / 1000L) + expiresInSeconds
    }

    fun clearTokens() {
        accessToken = null
        refreshToken = null
        expiresAtEpochSec = 0L
    }

    companion object {
        private const val KEY_CLIENT_ID = "spotify_client_id"
        private const val KEY_ACCESS_TOKEN = "spotify_access_token"
        private const val KEY_REFRESH_TOKEN = "spotify_refresh_token"
        private const val KEY_EXPIRES_AT = "spotify_expires_at"
        private const val KEY_CODE_VERIFIER = "spotify_code_verifier"
    }
}
