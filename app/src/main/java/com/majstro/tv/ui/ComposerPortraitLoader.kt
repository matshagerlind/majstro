package com.majstro.tv.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.majstro.tv.data.ComposerCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Fetches and caches public-domain composer portraits from Wikipedia's REST summary API.
 * Returns `null` while loading or when offline so callers can fall back to era artwork.
 */
@Composable
fun rememberComposerPortrait(composer: String): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    var bitmap by remember(composer) {
        mutableStateOf(ComposerPortraitCache.getMemory(composer))
    }

    LaunchedEffect(composer) {
        if (bitmap == null) {
            bitmap = ComposerPortraitCache.loadOrFetch(context, composer)
        }
    }

    return bitmap
}

private object ComposerPortraitCache {
    private val memory = ConcurrentHashMap<String, ImageBitmap>()
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private const val USER_AGENT = "MajstroTV/0.1 (Android; classical quiz)"

    fun getMemory(composer: String): ImageBitmap? = memory[composer]

    suspend fun loadOrFetch(context: Context, composer: String): ImageBitmap? =
        withContext(Dispatchers.IO) {
            memory[composer]?.let { return@withContext it }

            val cacheDir = File(context.cacheDir, "composer_portraits").apply { mkdirs() }
            val cacheFile = File(cacheDir, "${sha256(composer)}.img")

            if (cacheFile.exists()) {
                decodeFile(cacheFile)?.let {
                    memory[composer] = it
                    return@withContext it
                }
            }

            val imageUrl = fetchWikipediaThumbnailUrl(composer) ?: return@withContext null
            val bytes = downloadBytes(imageUrl) ?: return@withContext null
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                ?: return@withContext null

            runCatching { cacheFile.writeBytes(bytes) }
            memory[composer] = decoded
            decoded
        }

    private fun fetchWikipediaThumbnailUrl(composer: String): String? = runCatching {
        val title = ComposerCatalog.wikipediaTitleFor(composer)
        val encoded = URLEncoder.encode(title, "UTF-8").replace("+", "%20")
        val request = Request.Builder()
            .url("https://en.wikipedia.org/api/rest_v1/page/summary/$encoded")
            .header("User-Agent", USER_AGENT)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@runCatching null
            val body = response.body?.string() ?: return@runCatching null
            val root = json.parseToJsonElement(body).jsonObject
            root["thumbnail"]?.jsonObject?.get("source")?.jsonPrimitive?.content
                ?: root["originalimage"]?.jsonObject?.get("source")?.jsonPrimitive?.content
        }
    }.getOrNull()

    private fun downloadBytes(url: String): ByteArray? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@runCatching null
            response.body?.bytes()
        }
    }.getOrNull()

    private fun decodeFile(file: File): ImageBitmap? = runCatching {
        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
    }.getOrNull()

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
