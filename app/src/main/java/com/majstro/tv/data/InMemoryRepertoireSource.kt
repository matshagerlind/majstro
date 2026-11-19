package com.majstro.tv.data

import com.majstro.tv.domain.model.ClassicalWork
import com.majstro.tv.domain.port.RepertoireSource
import kotlinx.serialization.json.Json

/**
 * Curated 300-work seed repertoire loaded from `repertoire.json` across 5 difficulty levels
 * (60 works per level, 12 per musical era), each with exact composition year for Closest-Year scoring.
 */
class InMemoryRepertoireSource(
    private val resourceName: String = DEFAULT_RESOURCE_NAME
) : RepertoireSource {

    private val json = Json { ignoreUnknownKeys = true }

    private val cachedWorks: List<ClassicalWork> by lazy {
        val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream(resourceName)) {
            "Missing repertoire resource: $resourceName"
        }
        stream.bufferedReader().use { reader ->
            json.decodeFromString<List<ClassicalWork>>(reader.readText())
        }
    }

    override suspend fun loadWorks(): List<ClassicalWork> = cachedWorks

    companion object {
        const val DEFAULT_RESOURCE_NAME = "repertoire.json"
    }
}
