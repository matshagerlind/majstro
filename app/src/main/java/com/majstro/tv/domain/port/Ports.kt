package com.majstro.tv.domain.port

import com.majstro.tv.domain.model.ClassicalWork
import com.majstro.tv.domain.model.PlaybackStatus
import com.majstro.tv.domain.model.QuizQuestion
import kotlinx.coroutines.flow.StateFlow

/** Supplies classical works from local seed data, OpenOpus, or custom playlists. */
interface RepertoireSource {
    suspend fun loadWorks(): List<ClassicalWork>
}

/** Builds multiple-choice or multi-part quiz questions from a list of works. */
interface QuestionGenerator {
    fun generateQuestions(works: List<ClassicalWork>, count: Int): List<QuizQuestion>
}

/** Controls audio playback on Spotify Connect, local demo mode, or future backends. */
interface MusicPlayerGateway {
    val status: StateFlow<PlaybackStatus>
    suspend fun play(work: ClassicalWork): Result<Unit>
    suspend fun pause(): Result<Unit>
}
