package com.majstro.tv.domain.model

import kotlinx.serialization.Serializable

/** Top-level quiz repertoire category selectable in the lobby. */
@Serializable
enum class QuizCategory(val label: String) {
    CLASSICAL("Classical"),
    MOVIE_MUSIC("Movie Music")
}

/** Historical classical music periods used for categorization and quiz prompts. */
@Serializable
enum class MusicalEra(val label: String, val years: String) {
    BAROQUE("Baroque", "1600–1750"),
    CLASSICAL("Classical", "1750–1820"),
    ROMANTIC("Romantic", "1820–1900"),
    IMPRESSIONIST("Impressionist", "1890–1920"),
    MODERN("20th Century", "1900–2000")
}

/** High-level quiz question focus; easily extensible with new nerdy dimensions. */
@Serializable
enum class QuestionCategory(val label: String) {
    COMPOSER_AND_STYLE("Composer & Style"),
    ERA_AND_FORM("Era & Form"),
    WORK_IDENTIFICATION("Work Identification")
}

/** How players answer each song during a match. */
@Serializable
enum class AnswerMode(val label: String) {
    WRITE_IN("Everyone Writes"),
    MULTIPLE_CHOICE("Multiple Choice")
}

/** Scoring categories toggled manually on the Reveal screen in Write-In mode. */
@Serializable
enum class AwardField {
    COMPOSER,
    PIECE,
    CLOSEST_YEAR
}

/** Canonical classical music work entry returned by any [com.majstro.tv.domain.port.RepertoireSource]. */
@Serializable
data class ClassicalWork(
    val id: String,
    val title: String,
    val composer: String,
    val era: MusicalEra,
    val styleOrForm: String,
    val catalogNumber: String = "",
    val yearWritten: Int,
    val difficultyLevel: Int = 2,
    val stylisticClue: String,
    val funFact: String = "",
    val spotifyQuery: String,
    val startPositionMs: Long = 30_000L,
    val category: QuizCategory = QuizCategory.CLASSICAL
)

/** A single round question derived from a [ClassicalWork]. */
@Serializable
data class QuizQuestion(
    val roundNumber: Int,
    val totalRounds: Int,
    val work: ClassicalWork,
    val category: QuestionCategory,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int
)

/** Free-text guess submitted by a player in [AnswerMode.WRITE_IN]. */
@Serializable
data class WrittenAnswer(
    val composerGuess: String = "",
    val pieceGuess: String = "",
    val yearGuess: Int? = null
)

/** Awarded components for a player on the current round's Reveal screen. */
@Serializable
data class RoundAward(
    val composerCorrect: Boolean = false,
    val pieceCorrect: Boolean = false,
    val closestYear: Boolean = false
) {
    val totalPoints: Int
        get() = (if (composerCorrect) COMPOSER_POINTS else 0) +
                (if (pieceCorrect) PIECE_POINTS else 0) +
                (if (closestYear) CLOSEST_YEAR_POINTS else 0)

    companion object {
        const val COMPOSER_POINTS = 1
        const val PIECE_POINTS = 2
        const val CLOSEST_YEAR_POINTS = 1
    }
}

/** Connected participant (phone browser or local Tablet player). */
@Serializable
data class Player(
    val id: String,
    val name: String,
    val score: Int = 0,
    val lastRoundDelta: Int = 0,
    val selectedOptionIndex: Int? = null,
    val writtenAnswer: WrittenAnswer? = null,
    val roundAward: RoundAward = RoundAward()
)

/** Underlying playback adapter mode. */
@Serializable
enum class PlaybackMode(val label: String) {
    DEMO("Demo Mode"),
    SPOTIFY_CONNECT("Spotify Connect")
}

/** Live status reported by [com.majstro.tv.domain.port.MusicPlayerGateway]. */
@Serializable
data class PlaybackStatus(
    val mode: PlaybackMode = PlaybackMode.DEMO,
    val isPlaying: Boolean = false,
    val activeDeviceName: String? = null,
    val message: String = "Demo playback ready (connect Spotify via phone QR)"
)

/** Lifecycle phases of a Majstro quiz match. */
@Serializable
enum class QuizPhase {
    LOBBY,
    QUESTION,
    REVEAL,
    GAME_OVER
}

/** Immutable snapshot observed by both the Tablet UI and connected phone clients. */
@Serializable
data class GameState(
    val phase: QuizPhase = QuizPhase.LOBBY,
    val selectedCategory: QuizCategory = QuizCategory.CLASSICAL,
    val selectedRoundCount: Int = 5,
    val selectedDifficulty: Int = 2,
    val selectedAnswerMode: AnswerMode = AnswerMode.WRITE_IN,
    val players: List<Player> = emptyList(),
    val currentQuestion: QuizQuestion? = null,
    val playbackStatus: PlaybackStatus = PlaybackStatus(),
    val joinUrl: String = "",
    val spotifySetupUrl: String = ""
)
