package com.majstro.tv.domain

import com.majstro.tv.data.DefaultQuestionGenerator
import com.majstro.tv.data.InMemoryRepertoireSource
import com.majstro.tv.domain.engine.QuizEngine
import com.majstro.tv.domain.model.AnswerMode
import com.majstro.tv.domain.model.AwardField
import com.majstro.tv.domain.model.ClassicalWork
import com.majstro.tv.domain.model.PlaybackMode
import com.majstro.tv.domain.model.PlaybackStatus
import com.majstro.tv.domain.model.QuizPhase
import com.majstro.tv.domain.port.MusicPlayerGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuizEngineTest {

    private class FakeMusicPlayerGateway : MusicPlayerGateway {
        private val _status = MutableStateFlow(PlaybackStatus(PlaybackMode.DEMO))
        override val status: StateFlow<PlaybackStatus> = _status.asStateFlow()

        val playedWorks = mutableListOf<ClassicalWork>()
        var pauseCount = 0

        override suspend fun play(work: ClassicalWork): Result<Unit> {
            playedWorks += work
            _status.value = PlaybackStatus(PlaybackMode.DEMO, isPlaying = true, message = work.title)
            return Result.success(Unit)
        }

        override suspend fun pause(): Result<Unit> {
            pauseCount++
            _status.value = PlaybackStatus(PlaybackMode.DEMO, isPlaying = false, message = "Paused")
            return Result.success(Unit)
        }

        fun emitStatus(newStatus: PlaybackStatus) {
            _status.value = newStatus
        }
    }

    @Test
    fun `write-in mode pre-selects closest year and scores manual composer and piece awards`() = runTest {
        val gateway = FakeMusicPlayerGateway()
        val engine = QuizEngine(
            repertoireSource = InMemoryRepertoireSource(),
            questionGenerator = DefaultQuestionGenerator(Random(11)),
            playerGateway = gateway,
            scope = backgroundScope
        )

        engine.setRoundCount(3)
        engine.setDifficultyLevel(4)
        engine.setAnswerMode(AnswerMode.WRITE_IN)
        engine.registerPlayer("p1", "Clara")
        engine.registerPlayer("p2", "Johannes")

        engine.startGame()
        assertEquals(QuizPhase.QUESTION, engine.state.value.phase)
        val q1 = engine.state.value.currentQuestion!!
        assertEquals(3, q1.totalRounds)
        assertEquals(4, q1.work.difficultyLevel)

        val trueYear = q1.work.yearWritten
        engine.submitWrittenAnswer("p1", composer = q1.work.composer, piece = q1.work.title, year = trueYear + 2)
        engine.submitWrittenAnswer("p2", composer = q1.work.composer, piece = "Wrong Piece", year = trueYear + 15)

        engine.revealAnswer()
        assertEquals(QuizPhase.REVEAL, engine.state.value.phase)
        assertEquals(0, gateway.pauseCount)

        // Composer and Piece are NOT auto-confirmed; only Closest Year (+1) is pre-highlighted
        val claraReveal = engine.state.value.players.first { it.id == "p1" }
        val johannesReveal = engine.state.value.players.first { it.id == "p2" }
        assertFalse(claraReveal.roundAward.composerCorrect)
        assertFalse(claraReveal.roundAward.pieceCorrect)
        assertTrue(claraReveal.roundAward.closestYear)
        assertEquals(1, claraReveal.score)
        assertFalse(johannesReveal.roundAward.closestYear)
        assertEquals(0, johannesReveal.score)

        // Manually confirm Clara got Composer (+1) and Piece (+2), and Johannes got Composer (+1)
        engine.togglePlayerAward("p1", AwardField.COMPOSER)
        engine.togglePlayerAward("p1", AwardField.PIECE)
        engine.togglePlayerAward("p2", AwardField.COMPOSER)

        val claraJudged = engine.state.value.players.first { it.id == "p1" }
        val johannesJudged = engine.state.value.players.first { it.id == "p2" }
        assertEquals(4, claraJudged.score)
        assertEquals(4, claraJudged.lastRoundDelta)
        assertEquals(1, johannesJudged.score)

        // Toggling Piece off updates live score immediately
        engine.togglePlayerAward("p1", AwardField.PIECE)
        assertEquals(2, engine.state.value.players.first { it.id == "p1" }.score)
    }

    @Test
    fun `multiple choice mode scores players and finishes match`() = runTest {
        val gateway = FakeMusicPlayerGateway()
        val engine = QuizEngine(
            repertoireSource = InMemoryRepertoireSource(),
            questionGenerator = DefaultQuestionGenerator(Random(42)),
            playerGateway = gateway,
            scope = backgroundScope
        )

        engine.setAnswerMode(AnswerMode.MULTIPLE_CHOICE)
        engine.registerPlayer("p1", "Clara")
        engine.registerPlayer("p2", "Johannes")

        engine.startGame(roundCount = 2, difficultyLevel = 2)
        val q1 = engine.state.value.currentQuestion
        assertNotNull(q1)
        assertEquals(1, q1!!.roundNumber)
        assertEquals(2, q1.totalRounds)

        val wrongIndex = (q1.correctIndex + 1) % q1.options.size
        engine.submitAnswer("p1", q1.correctIndex)
        engine.submitAnswer("p2", wrongIndex)

        engine.revealAnswer()
        assertEquals(QuizPhase.REVEAL, engine.state.value.phase)
        assertEquals(QuizEngine.MULTIPLE_CHOICE_POINTS, engine.state.value.players.first { it.id == "p1" }.score)
        assertEquals(0, engine.state.value.players.first { it.id == "p2" }.score)

        engine.nextRound()
        val q2 = engine.state.value.currentQuestion!!
        engine.submitAnswer("p2", q2.correctIndex)
        engine.revealAnswer()
        engine.nextRound()

        assertEquals(QuizPhase.GAME_OVER, engine.state.value.phase)
        assertNull(engine.state.value.currentQuestion)
        assertTrue(engine.state.value.players.all { it.score == QuizEngine.MULTIPLE_CHOICE_POINTS })

        engine.backToLobby()
        assertEquals(QuizPhase.LOBBY, engine.state.value.phase)
        assertTrue(engine.state.value.players.all { it.score == 0 })
    }

    @Test
    fun `starting with no phone players creates default Tablet player`() = runTest {
        val gateway = FakeMusicPlayerGateway()
        val engine = QuizEngine(
            repertoireSource = InMemoryRepertoireSource(),
            questionGenerator = DefaultQuestionGenerator(Random(1)),
            playerGateway = gateway,
            scope = backgroundScope
        )

        engine.startGame(roundCount = 1)
        assertEquals(1, engine.state.value.players.size)
        assertEquals(QuizEngine.HOST_PLAYER_ID, engine.state.value.players.first().id)
        assertEquals("Tablet", engine.state.value.players.first().name)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `playback status updates reactively from player gateway flow`() = runTest {
        val gateway = FakeMusicPlayerGateway()
        val engine = QuizEngine(
            repertoireSource = InMemoryRepertoireSource(),
            questionGenerator = DefaultQuestionGenerator(Random(1)),
            playerGateway = gateway,
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        )

        assertEquals(PlaybackMode.DEMO, engine.state.value.playbackStatus.mode)

        val linked = PlaybackStatus(
            mode = PlaybackMode.SPOTIFY_CONNECT,
            isPlaying = false,
            message = "Spotify linked — ready to play"
        )
        gateway.emitStatus(linked)

        assertEquals(linked, engine.state.value.playbackStatus)
    }
}
