package com.majstro.tv.data

import com.majstro.tv.domain.model.ClassicalWork
import com.majstro.tv.domain.model.QuestionCategory
import com.majstro.tv.domain.model.QuizQuestion
import com.majstro.tv.domain.port.QuestionGenerator
import kotlin.random.Random

/**
 * Generates 4-option multiple-choice questions rotating across Composer/Style, Era/Form,
 * and Work Identification so rounds test different facets of classical knowledge.
 */
class DefaultQuestionGenerator(
    private val random: Random = Random.Default
) : QuestionGenerator {

    override fun generateQuestions(works: List<ClassicalWork>, count: Int): List<QuizQuestion> {
        require(works.isNotEmpty()) { "Cannot generate questions from an empty repertoire." }

        val targetCount = count.coerceAtMost(works.size)
        val selectedWorks = works.shuffled(random).take(targetCount)
        val categories = QuestionCategory.entries

        return selectedWorks.mapIndexed { index, work ->
            val category = categories[index % categories.size]
            val prompt = when (category) {
                QuestionCategory.COMPOSER_AND_STYLE ->
                    "Identify the composer & era of this ${work.styleOrForm}:"
                QuestionCategory.ERA_AND_FORM ->
                    "Listen to the orchestration & style — which piece and era is playing?"
                QuestionCategory.WORK_IDENTIFICATION ->
                    "Which masterwork matches this musical fingerprint?"
            }

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

    private fun formatOption(work: ClassicalWork, category: QuestionCategory): String = when (category) {
        QuestionCategory.COMPOSER_AND_STYLE ->
            "${work.composer} · ${work.era.label}"
        QuestionCategory.ERA_AND_FORM ->
            "${work.era.label} · ${work.styleOrForm} (${work.composer.substringAfterLast(' ')})"
        QuestionCategory.WORK_IDENTIFICATION ->
            "${work.composer.substringAfterLast(' ')} — ${work.title}"
    }

    companion object {
        private const val OPTION_COUNT = 4
    }
}
