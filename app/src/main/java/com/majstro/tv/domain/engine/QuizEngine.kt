package com.majstro.tv.domain.engine

import com.majstro.tv.domain.model.GameState
import com.majstro.tv.domain.model.PlaybackStatus
import com.majstro.tv.domain.model.Player
import com.majstro.tv.domain.model.QuizPhase
import com.majstro.tv.domain.model.QuizQuestion
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

/**
 * Pure Kotlin state machine driving the Majstro quiz lifecycle:
 * LOBBY -> QUESTION -> REVEAL -> (next QUESTION | GAME_OVER).
 */
class QuizEngine(
    private val repertoireSource: RepertoireSource,
    private val questionGenerator: QuestionGenerator,
    private val playerGateway: MusicPlayerGateway,
    scope: CoroutineScope? = null
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(GameState(playbackStatus = playerGateway.status.value))
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var questions: List<QuizQuestion> = emptyList()
    private var currentIndex: Int = -1

    init {
        if (scope != null) {
            playerGateway.status
                .onEach { updatePlaybackStatus(it) }
                .launchIn(scope)
        }
    }

    /** Updates the playback status displayed on the TV/Tablet and phone controllers. */
    fun updatePlaybackStatus(playbackStatus: PlaybackStatus) {
        _state.update { it.copy(playbackStatus = playbackStatus) }
    }

    /** Records the TV/Tablet's local LAN URLs for QR code display. */
    fun setServerUrls(joinUrl: String, spotifySetupUrl: String) {
        _state.update { it.copy(joinUrl = joinUrl, spotifySetupUrl = spotifySetupUrl) }
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

    /** Starts a fresh match from the lobby, creating a default TV/Tablet player if none joined via phone. */
    suspend fun startGame(roundCount: Int = DEFAULT_ROUND_COUNT) = mutex.withLock {
        val works = repertoireSource.loadWorks()
        require(works.isNotEmpty()) { "RepertoireSource must provide at least one classical work." }

        val generated = questionGenerator.generateQuestions(works, roundCount)
        require(generated.isNotEmpty()) { "QuestionGenerator returned no questions." }

        questions = generated
        currentIndex = 0
        val firstQuestion = questions[0]

        _state.update { current ->
            val activePlayers = current.players.ifEmpty {
                listOf(Player(id = TV_PLAYER_ID, name = "TV Couch"))
            }.map { it.copy(score = 0, lastRoundDelta = 0, selectedOptionIndex = null) }

            current.copy(
                phase = QuizPhase.QUESTION,
                players = activePlayers,
                currentQuestion = firstQuestion,
                playbackStatus = playerGateway.status.value
            )
        }

        playerGateway.play(firstQuestion.work)
        _state.update { it.copy(playbackStatus = playerGateway.status.value) }
    }

    /** Records a player's choice (0..3) during an active QUESTION round. */
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
                    name = "TV Couch",
                    selectedOptionIndex = optionIndex
                )
            }
            current.copy(players = updatedPlayers)
        }
    }

    /** Stops playback, scores all player answers, and transitions to the REVEAL screen. */
    suspend fun revealAnswer() = mutex.withLock {
        val current = _state.value
        if (current.phase != QuizPhase.QUESTION) return@withLock
        val question = current.currentQuestion ?: return@withLock

        playerGateway.pause()

        val scoredPlayers = current.players.map { player ->
            val isCorrect = player.selectedOptionIndex == question.correctIndex
            val delta = if (isCorrect) POINTS_PER_CORRECT_ANSWER else 0
            player.copy(
                score = player.score + delta,
                lastRoundDelta = delta
            )
        }.sortedByDescending { it.score }

        _state.update {
            it.copy(
                phase = QuizPhase.REVEAL,
                players = scoredPlayers,
                playbackStatus = playerGateway.status.value
            )
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
                players = it.players.map { p -> p.copy(lastRoundDelta = 0, selectedOptionIndex = null) }
            )
        }

        playerGateway.play(nextQuestion.work)
        _state.update { it.copy(playbackStatus = playerGateway.status.value) }
    }

    /** Returns everyone to the lobby while keeping connected players' names. */
    suspend fun backToLobby() = mutex.withLock {
        playerGateway.pause()
        currentIndex = -1
        questions = emptyList()
        _state.update { current ->
            current.copy(
                phase = QuizPhase.LOBBY,
                currentQuestion = null,
                players = current.players.map { it.copy(score = 0, lastRoundDelta = 0, selectedOptionIndex = null) },
                playbackStatus = playerGateway.status.value
            )
        }
    }

    companion object {
        const val DEFAULT_ROUND_COUNT = 5
        const val POINTS_PER_CORRECT_ANSWER = 100
        const val TV_PLAYER_ID = "tv-remote"
    }
}
