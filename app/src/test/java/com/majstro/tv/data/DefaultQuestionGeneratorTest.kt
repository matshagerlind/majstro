package com.majstro.tv.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DefaultQuestionGeneratorTest {

    @Test
    fun `generated questions have 4 unique options and valid correctIndex`() = runTest {
        val works = InMemoryRepertoireSource().loadWorks()
        val generator = DefaultQuestionGenerator(Random(7))

        val questions = generator.generateQuestions(works, count = 5)
        assertEquals(5, questions.size)

        questions.forEachIndexed { index, q ->
            assertEquals(index + 1, q.roundNumber)
            assertEquals(5, q.totalRounds)
            assertEquals(4, q.options.size)
            assertEquals(4, q.options.distinct().size)
            assertTrue(q.correctIndex in 0..3)
            assertTrue(q.prompt.isNotBlank())
        }
    }
}
