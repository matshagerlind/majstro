package com.majstro.tv.data

import com.majstro.tv.domain.model.ClassicalWork
import com.majstro.tv.domain.model.QuizCategory
import com.majstro.tv.domain.port.RepertoireSource
import kotlinx.serialization.json.Json

/**
 * Curated seed repertoire loaded from `repertoire.json` (Classical) and `movie_repertoire.json`
 * (Movie Music) across 5 difficulty levels, each with exact year for Closest-Year scoring.
 */
class InMemoryRepertoireSource(
    private val resourceName: String = DEFAULT_RESOURCE_NAME,
    private val movieResourceName: String? = DEFAULT_MOVIE_RESOURCE_NAME
) : RepertoireSource {

    private val json = Json { ignoreUnknownKeys = true }

    private val cachedWorks: List<ClassicalWork> by lazy {
        val classicalWorks = loadResource(resourceName, required = true)
            .map { it.copy(category = QuizCategory.CLASSICAL) }
        val movieWorks = movieResourceName?.let { name ->
            loadResource(name, required = false).map { it.copy(category = QuizCategory.MOVIE_MUSIC) }
        }.orEmpty()
        classicalWorks + movieWorks
    }

    private fun loadResource(name: String, required: Boolean): List<ClassicalWork> {
        val stream = javaClass.classLoader?.getResourceAsStream(name)
        if (stream == null) {
            check(!required) { "Missing repertoire resource: $name" }
            return emptyList()
        }
        return stream.bufferedReader().use { reader ->
            json.decodeFromString<List<ClassicalWork>>(reader.readText())
        }
    }

    override suspend fun loadWorks(): List<ClassicalWork> = cachedWorks

    companion object {
        const val DEFAULT_RESOURCE_NAME = "repertoire.json"
        const val DEFAULT_MOVIE_RESOURCE_NAME = "movie_repertoire.json"
    }
}
