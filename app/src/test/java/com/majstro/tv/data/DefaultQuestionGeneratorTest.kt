package com.majstro.tv.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DefaultQuestionGeneratorTest {

    @Test
    fun `generated questions have 4 unique options and prioritize chosen difficulty`() = runTest {
        val works = InMemoryRepertoireSource().loadWorks()
        assertEquals(25, works.size)
        for (level in 1..5) {
            assertEquals(5, works.count { it.difficultyLevel == level })
        }

        val generator = DefaultQuestionGenerator(Random(7))

        val level5Sample = generator.generateQuestions(works, count = 3, difficultyLevel = 5)
        assertEquals(3, level5Sample.size)
        assertTrue(level5Sample.all { it.work.difficultyLevel == 5 })

        val questions = generator.generateQuestions(works, count = 10, difficultyLevel = 1)
        assertEquals(10, questions.size)
        assertTrue(questions.take(5).all { it.work.difficultyLevel == 1 })
        assertTrue(questions.drop(5).all { it.work.difficultyLevel == 2 })

        questions.forEachIndexed { index, q ->
            assertEquals(index + 1, q.roundNumber)
            assertEquals(10, q.totalRounds)
            assertEquals(4, q.options.size)
            assertEquals(4, q.options.distinct().size)
            assertTrue(q.correctIndex in 0..3)
            assertTrue(q.prompt.isNotBlank())
        }
    }
}
