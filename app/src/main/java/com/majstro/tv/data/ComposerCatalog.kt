package com.majstro.tv.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ComposerMetadata(
    val name: String,
    val lifespan: String,
    val wikipediaTitle: String
)

/**
 * Provides composer birth–death dates and Wikipedia article titles loaded from `composers.json`.
 */
object ComposerCatalog {
    private const val RESOURCE_NAME = "composers.json"
    private val json = Json { ignoreUnknownKeys = true }

    private val byName: Map<String, ComposerMetadata> by lazy {
        val stream = checkNotNull(ComposerCatalog::class.java.classLoader?.getResourceAsStream(RESOURCE_NAME)) {
            "Missing composer catalog resource: $RESOURCE_NAME"
        }
        val entries = stream.bufferedReader().use { reader ->
            json.decodeFromString<List<ComposerMetadata>>(reader.readText())
        }
        entries.associateBy { it.name }
    }

    fun all(): Collection<ComposerMetadata> = byName.values

    fun lifespanFor(composer: String): String? = byName[composer]?.lifespan

    fun wikipediaTitleFor(composer: String): String =
        byName[composer]?.wikipediaTitle ?: composer.trim().replace(' ', '_')
}
