package com.majstro.tv.data

import com.majstro.tv.domain.model.ClassicalWork
import com.majstro.tv.domain.model.QuestionCategory
import com.majstro.tv.domain.model.QuizCategory
import com.majstro.tv.domain.model.QuizQuestion
import com.majstro.tv.domain.port.QuestionGenerator
import kotlin.math.abs
import kotlin.random.Random

/**
 * Selects works prioritizing the chosen difficulty level (1..5), pulling nearest levels
 * when [count] exceeds the number of works at that exact level, and builds 4-option prompts.
 */
class DefaultQuestionGenerator(
    private val random: Random = Random.Default
) : QuestionGenerator {

    override fun generateQuestions(
        works: List<ClassicalWork>,
        count: Int,
        difficultyLevel: Int
    ): List<QuizQuestion> {
        require(works.isNotEmpty()) { "Cannot generate questions from an empty repertoire." }

        val targetCount = count.coerceIn(1, works.size)
        val clampedDifficulty = difficultyLevel.coerceIn(1, 5)

        // Group works by distance to selected difficulty and shuffle within each bucket.
        val selectedWorks = works
            .groupBy { abs(it.difficultyLevel - clampedDifficulty) }
            .toSortedMap()
            .values
            .flatMap { bucket -> bucket.shuffled(random) }
            .take(targetCount)

        val categories = QuestionCategory.entries

        return selectedWorks.mapIndexed { index, work ->
            val category = categories[index % categories.size]
            val prompt = formatPrompt(work, category)

            val correctLabel = formatOption(work, category)
            val distractors = works
                .asSequence()
                .filter { it.id != work.id }
                .map { formatOption(it, category) }
                .distinct()
                .filter { it != correctLabel }
                .toList()
                .shuffled(random)
                .take(OPTION_COUNT - 1)

            val allOptions = (distractors + correctLabel).shuffled(random)
            val correctIndex = allOptions.indexOf(correctLabel)

            QuizQuestion(
                roundNumber = index + 1,
                totalRounds = targetCount,
                work = work,
                category = category,
                prompt = prompt,
                options = allOptions,
                correctIndex = correctIndex
            )
        }
    }

    private fun formatPrompt(work: ClassicalWork, category: QuestionCategory): String =
        if (work.category == QuizCategory.MOVIE_MUSIC) {
            when (category) {
                QuestionCategory.COMPOSER_AND_STYLE ->
                    "Identify the composer & genre of this ${work.styleOrForm}:"
                QuestionCategory.ERA_AND_FORM ->
                    "Listen to the score — which movie & composer is playing?"
                QuestionCategory.WORK_IDENTIFICATION ->
                    "Which film score is playing?"
            }
        } else {
            when (category) {
                QuestionCategory.COMPOSER_AND_STYLE ->
                    "Identify the composer & era of this ${work.styleOrForm}:"
                QuestionCategory.ERA_AND_FORM ->
                    "Listen to the orchestration & style — which piece and era is playing?"
                QuestionCategory.WORK_IDENTIFICATION ->
                    "Which masterwork matches this musical fingerprint?"
            }
        }

    private fun formatOption(work: ClassicalWork, category: QuestionCategory): String =
        if (work.category == QuizCategory.MOVIE_MUSIC) {
            when (category) {
                QuestionCategory.COMPOSER_AND_STYLE ->
                    "${work.composer} · ${work.styleOrForm}"
                QuestionCategory.ERA_AND_FORM ->
                    "${work.title} (${work.yearWritten}) · ${work.composer.substringAfterLast(' ')}"
                QuestionCategory.WORK_IDENTIFICATION ->
                    "${work.composer.substringAfterLast(' ')} — ${work.title}"
            }
        } else {
            when (category) {
                QuestionCategory.COMPOSER_AND_STYLE ->
                    "${work.composer} · ${work.era.label}"
                QuestionCategory.ERA_AND_FORM ->
                    "${work.era.label} · ${work.styleOrForm} (${work.composer.substringAfterLast(' ')})"
                QuestionCategory.WORK_IDENTIFICATION ->
                    "${work.composer.substringAfterLast(' ')} — ${work.title}"
            }
        }

    companion object {
        private const val OPTION_COUNT = 4
    }
}
