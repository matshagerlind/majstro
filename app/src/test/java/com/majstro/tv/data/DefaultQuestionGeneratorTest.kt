package com.majstro.tv.data

import com.majstro.tv.domain.model.MusicalEra
import com.majstro.tv.domain.model.QuizCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DefaultQuestionGeneratorTest {

    @Test
    fun `repertoire contains 300 unique works evenly distributed by difficulty and era`() = runTest {
        val works = InMemoryRepertoireSource().loadWorks()
            .filter { it.category == QuizCategory.CLASSICAL }
        assertEquals(300, works.size)
        assertEquals(300, works.map { it.id }.distinct().size)

        for (level in 1..5) {
            val levelWorks = works.filter { it.difficultyLevel == level }
            assertEquals("Expected 60 works at level $level", 60, levelWorks.size)
            for (era in MusicalEra.entries) {
                assertEquals(
                    "Expected 12 $era works at level $level",
                    12,
                    levelWorks.count { it.era == era }
                )
            }
        }

        works.forEach { work ->
            assertTrue("Blank title for ${work.id}", work.title.isNotBlank())
            assertTrue("Blank composer for ${work.id}", work.composer.isNotBlank())
            assertTrue("Blank funFact for ${work.id}", work.funFact.isNotBlank())
            assertTrue("Blank spotifyQuery for ${work.id}", work.spotifyQuery.isNotBlank())
            assertTrue("Invalid year for ${work.id}", work.yearWritten in 1600..2025)
            assertTrue(
                "Missing composer lifespan for ${work.composer}",
                !ComposerCatalog.lifespanFor(work.composer).isNullOrBlank()
            )
            assertTrue(
                "Blank Wikipedia title for ${work.composer}",
                ComposerCatalog.wikipediaTitleFor(work.composer).isNotBlank()
            )
        }
    }

    @Test
    fun `generated questions have 4 unique options and prioritize chosen difficulty`() = runTest {
        val works = InMemoryRepertoireSource().loadWorks()
            .filter { it.category == QuizCategory.CLASSICAL }
        val generator = DefaultQuestionGenerator(Random(7))

        val level5Sample = generator.generateQuestions(works, count = 10, difficultyLevel = 5)
        assertEquals(10, level5Sample.size)
        assertTrue(level5Sample.all { it.work.difficultyLevel == 5 })

        val questions = generator.generateQuestions(works, count = 65, difficultyLevel = 1)
        assertEquals(65, questions.size)
        assertTrue(questions.take(60).all { it.work.difficultyLevel == 1 })
        assertTrue(questions.drop(60).all { it.work.difficultyLevel == 2 })

        questions.forEachIndexed { index, q ->
            assertEquals(index + 1, q.roundNumber)
            assertEquals(65, q.totalRounds)
            assertEquals(4, q.options.size)
            assertEquals(4, q.options.distinct().size)
            assertTrue(q.correctIndex in 0..3)
            assertTrue(q.prompt.isNotBlank())
        }
    }

    @Test
    fun `movie repertoire contains 100 unique works evenly distributed by difficulty`() = runTest {
        val movieWorks = InMemoryRepertoireSource().loadWorks()
            .filter { it.category == QuizCategory.MOVIE_MUSIC }
        assertEquals(100, movieWorks.size)
        assertEquals(100, movieWorks.map { it.id }.distinct().size)

        for (level in 1..5) {
            val levelWorks = movieWorks.filter { it.difficultyLevel == level }
            assertEquals("Expected 20 movie works at level $level", 20, levelWorks.size)
        }

        movieWorks.forEach { work ->
            assertTrue("Blank title for ${work.id}", work.title.isNotBlank())
            assertTrue("Blank composer for ${work.id}", work.composer.isNotBlank())
            assertTrue("Blank funFact for ${work.id}", work.funFact.isNotBlank())
            assertTrue("Blank spotifyQuery for ${work.id}", work.spotifyQuery.isNotBlank())
            assertTrue("Invalid year for ${work.id}", work.yearWritten in 1920..2026)
        }

        val generator = DefaultQuestionGenerator(Random(11))
        val questions = generator.generateQuestions(movieWorks, count = 25, difficultyLevel = 1)
        assertEquals(25, questions.size)
        assertTrue(questions.take(20).all { it.work.difficultyLevel == 1 })
        assertTrue(questions.drop(20).all { it.work.difficultyLevel == 2 })
        questions.forEach { q ->
            assertEquals(4, q.options.size)
            assertEquals(4, q.options.distinct().size)
            assertTrue(q.correctIndex in 0..3)
            assertTrue(q.prompt.isNotBlank())
        }
    }
}
