package com.majstro.tv.domain.engine

import com.majstro.tv.domain.model.AnswerMode
import com.majstro.tv.domain.model.AwardField
import com.majstro.tv.domain.model.GameState
import com.majstro.tv.domain.model.Player
import com.majstro.tv.domain.model.QuizPhase
import com.majstro.tv.domain.model.QuizQuestion
import com.majstro.tv.domain.model.RoundAward
import com.majstro.tv.domain.model.WrittenAnswer
import com.majstro.tv.domain.port.MusicPlayerGateway
import com.majstro.tv.domain.port.QuestionGenerator
import com.majstro.tv.domain.port.RepertoireSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs

/**
 * Pure Kotlin state machine driving the Majstro quiz lifecycle:
 * LOBBY -> QUESTION -> REVEAL (with optional manual judging) -> (next QUESTION | GAME_OVER).
 */
class QuizEngine(
    private val repertoireSource: RepertoireSource,
    private val questionGenerator: QuestionGenerator,
    private val playerGateway: MusicPlayerGateway,
    scope: CoroutineScope
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(GameState(playbackStatus = playerGateway.status.value))
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var questions: List<QuizQuestion> = emptyList()
    private var currentIndex: Int = -1

    init {
        playerGateway.status
            .onEach { status -> _state.update { it.copy(playbackStatus = status) } }
            .launchIn(scope)
    }

    /** Records the Tablet's local LAN URLs for QR code display. */
    fun setServerUrls(joinUrl: String, spotifySetupUrl: String) {
        _state.update { it.copy(joinUrl = joinUrl, spotifySetupUrl = spotifySetupUrl) }
    }

    /** Updates the number of songs for the next match while in the lobby. */
    fun setRoundCount(count: Int) {
        val clamped = count.coerceIn(MIN_ROUND_COUNT, MAX_ROUND_COUNT)
        _state.update { current ->
            if (current.phase != QuizPhase.LOBBY) current
            else current.copy(selectedRoundCount = clamped)
        }
    }

    /** Updates the 1..5 difficulty level for the next match while in the lobby. */
    fun setDifficultyLevel(level: Int) {
        val clamped = level.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
        _state.update { current ->
            if (current.phase != QuizPhase.LOBBY) current
            else current.copy(selectedDifficulty = clamped)
        }
    }

    /** Switches between Everyone Writes and Multiple Choice while in the lobby. */
    fun setAnswerMode(mode: AnswerMode) {
        _state.update { current ->
            if (current.phase != QuizPhase.LOBBY) current
            else current.copy(selectedAnswerMode = mode)
        }
    }

    /** Adds or updates a player in the current lobby/match. */
    fun registerPlayer(id: String, name: String): Player {
        val cleanName = name.trim().ifEmpty { "Listener" }.take(20)
        var registered = Player(id = id, name = cleanName)
        _state.update { current ->
            val existing = current.players.find { it.id == id }
            val updatedPlayers = if (existing != null) {
                registered = existing.copy(name = cleanName)
                current.players.map { if (it.id == id) registered else it }
            } else {
                current.players + registered
            }
            current.copy(players = updatedPlayers)
        }
        return registered
    }

    /** Starts a fresh match from the lobby using the selected song count, difficulty, and mode. */
    suspend fun startGame(
        roundCount: Int = _state.value.selectedRoundCount,
        difficultyLevel: Int = _state.value.selectedDifficulty
    ) = mutex.withLock {
        val works = repertoireSource.loadWorks()
        require(works.isNotEmpty()) { "RepertoireSource must provide at least one classical work." }

        val targetCount = roundCount.coerceIn(MIN_ROUND_COUNT, MAX_ROUND_COUNT)
        val targetDifficulty = difficultyLevel.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)

        val generated = questionGenerator.generateQuestions(
            works = works,
            count = targetCount,
            difficultyLevel = targetDifficulty
        )
        require(generated.isNotEmpty()) { "QuestionGenerator returned no questions." }

        questions = generated
        currentIndex = 0
        val firstQuestion = questions[0]

        _state.update { current ->
            val activePlayers = current.players.ifEmpty {
                listOf(Player(id = HOST_PLAYER_ID, name = "Tablet"))
            }.map {
                it.copy(
                    score = 0,
                    lastRoundDelta = 0,
                    selectedOptionIndex = null,
                    writtenAnswer = null,
                    roundAward = RoundAward()
                )
            }

            current.copy(
                phase = QuizPhase.QUESTION,
                selectedRoundCount = generated.size,
                selectedDifficulty = targetDifficulty,
                players = activePlayers,
                currentQuestion = firstQuestion
            )
        }

        playerGateway.play(firstQuestion.work)
    }

    /** Records a player's multiple-choice pick (0..3) during an active QUESTION round. */
    fun submitAnswer(playerId: String, optionIndex: Int) {
        _state.update { current ->
            if (current.phase != QuizPhase.QUESTION) return@update current
            val question = current.currentQuestion ?: return@update current
            if (optionIndex !in question.options.indices) return@update current

            val hasPlayer = current.players.any { it.id == playerId }
            val updatedPlayers = if (hasPlayer) {
                current.players.map { player ->
                    if (player.id == playerId) player.copy(selectedOptionIndex = optionIndex) else player
                }
            } else {
                current.players + Player(
                    id = playerId,
                    name = "Tablet",
                    selectedOptionIndex = optionIndex
                )
            }
            current.copy(players = updatedPlayers)
        }
    }

    /** Records a player's free-text Composer, Piece, and Year Written guesses during QUESTION. */
    fun submitWrittenAnswer(
        playerId: String,
        composer: String,
        piece: String,
        year: Int?
    ) {
        _state.update { current ->
            if (current.phase != QuizPhase.QUESTION) return@update current
            val cleanAnswer = WrittenAnswer(
                composerGuess = composer.trim().take(60),
                pieceGuess = piece.trim().take(80),
                yearGuess = year
            )

            val hasPlayer = current.players.any { it.id == playerId }
            val updatedPlayers = if (hasPlayer) {
                current.players.map { player ->
                    if (player.id == playerId) player.copy(writtenAnswer = cleanAnswer) else player
                }
            } else {
                current.players + Player(
                    id = playerId,
                    name = "Tablet",
                    writtenAnswer = cleanAnswer
                )
            }
            current.copy(players = updatedPlayers)
        }
    }

    /** Stops playback, scores multiple-choice or pre-selects closest year for write-in, and reveals the answer. */
    suspend fun revealAnswer() = mutex.withLock {
        val current = _state.value
        if (current.phase != QuizPhase.QUESTION) return@withLock
        val question = current.currentQuestion ?: return@withLock

        playerGateway.pause()

        val updatedPlayers = when (current.selectedAnswerMode) {
            AnswerMode.MULTIPLE_CHOICE -> {
                current.players.map { player ->
                    val isCorrect = player.selectedOptionIndex == question.correctIndex
                    val delta = if (isCorrect) MULTIPLE_CHOICE_POINTS else 0
                    player.copy(
                        score = player.score + delta,
                        lastRoundDelta = delta,
                        roundAward = RoundAward()
                    )
                }.sortedByDescending { it.score }
            }
            AnswerMode.WRITE_IN -> {
                val targetYear = question.work.yearWritten
                val minYearDiff = current.players
                    .mapNotNull { it.writtenAnswer?.yearGuess }
                    .minOfOrNull { abs(it - targetYear) }

                current.players.map { player ->
                    val yearGuess = player.writtenAnswer?.yearGuess
                    val isClosest = minYearDiff != null &&
                        yearGuess != null &&
                        abs(yearGuess - targetYear) == minYearDiff
                    val award = RoundAward(
                        composerCorrect = false,
                        pieceCorrect = false,
                        closestYear = isClosest
                    )
                    val delta = award.totalPoints
                    player.copy(
                        score = player.score + delta,
                        lastRoundDelta = delta,
                        roundAward = award
                    )
                }
            }
        }

        _state.update {
            it.copy(
                phase = QuizPhase.REVEAL,
                players = updatedPlayers
            )
        }
    }

    /**
     * Toggles a scoring category (+1 Composer, +2 Piece, +1 Closest Year) for [playerId]
     * during the REVEAL phase in [AnswerMode.WRITE_IN] and updates their live score.
     */
    fun togglePlayerAward(playerId: String, field: AwardField) {
        _state.update { current ->
            if (current.phase != QuizPhase.REVEAL) return@update current
            val updatedPlayers = current.players.map { player ->
                if (player.id != playerId) {
                    player
                } else {
                    val currentAward = player.roundAward
                    val newAward = when (field) {
                        AwardField.COMPOSER ->
                            currentAward.copy(composerCorrect = !currentAward.composerCorrect)
                        AwardField.PIECE ->
                            currentAward.copy(pieceCorrect = !currentAward.pieceCorrect)
                        AwardField.CLOSEST_YEAR ->
                            currentAward.copy(closestYear = !currentAward.closestYear)
                    }
                    val newDelta = newAward.totalPoints
                    val updatedScore = (player.score - player.lastRoundDelta + newDelta).coerceAtLeast(0)
                    player.copy(
                        score = updatedScore,
                        lastRoundDelta = newDelta,
                        roundAward = newAward
                    )
                }
            }
            current.copy(players = updatedPlayers)
        }
    }

    /** Advances from REVEAL to the next QUESTION or finishes the match at GAME_OVER. */
    suspend fun nextRound() = mutex.withLock {
        val current = _state.value
        if (current.phase != QuizPhase.REVEAL) return@withLock

        val nextIdx = currentIndex + 1
        if (nextIdx >= questions.size) {
            _state.update {
                it.copy(
                    phase = QuizPhase.GAME_OVER,
                    currentQuestion = null,
                    players = it.players.sortedByDescending { p -> p.score }
                )
            }
            return@withLock
        }

        currentIndex = nextIdx
        val nextQuestion = questions[nextIdx]

        _state.update {
            it.copy(
                phase = QuizPhase.QUESTION,
                currentQuestion = nextQuestion,
                players = it.players.map { p ->
                    p.copy(
                        lastRoundDelta = 0,
                        selectedOptionIndex = null,
                        writtenAnswer = null,
                        roundAward = RoundAward()
                    )
                }
            )
        }

        playerGateway.play(nextQuestion.work)
    }

    /** Returns everyone to the lobby while keeping connected players' names and settings. */
    suspend fun backToLobby() = mutex.withLock {
        playerGateway.pause()
        currentIndex = -1
        questions = emptyList()
        _state.update { current ->
            current.copy(
                phase = QuizPhase.LOBBY,
                currentQuestion = null,
                players = current.players.map {
                    it.copy(
                        score = 0,
                        lastRoundDelta = 0,
                        selectedOptionIndex = null,
                        writtenAnswer = null,
                        roundAward = RoundAward()
                    )
                }
            )
        }
    }

    companion object {
        const val DEFAULT_ROUND_COUNT = 5
        const val MIN_ROUND_COUNT = 1
        const val MAX_ROUND_COUNT = 25
        const val MIN_DIFFICULTY = 1
        const val MAX_DIFFICULTY = 5
        const val MULTIPLE_CHOICE_POINTS = 2
        const val HOST_PLAYER_ID = "tablet-host"
    }
}
