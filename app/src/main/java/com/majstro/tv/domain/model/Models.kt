package com.majstro.tv.domain.model

import kotlinx.serialization.Serializable

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

/** Canonical classical music work entry returned by any [com.majstro.tv.domain.port.RepertoireSource]. */
@Serializable
data class ClassicalWork(
    val id: String,
    val title: String,
    val composer: String,
    val era: MusicalEra,
    val styleOrForm: String,
    val catalogNumber: String = "",
    val stylisticClue: String,
    val spotifyQuery: String,
    val startPositionMs: Long = 30_000L
)

/** A single multiple-choice round question derived from a [ClassicalWork]. */
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

/** Connected participant (phone browser or local TV/tablet player). */
@Serializable
data class Player(
    val id: String,
    val name: String,
    val score: Int = 0,
    val lastRoundDelta: Int = 0,
    val selectedOptionIndex: Int? = null
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

/** Immutable snapshot observed by both the TV/Tablet UI and connected phone clients. */
@Serializable
data class GameState(
    val phase: QuizPhase = QuizPhase.LOBBY,
    val players: List<Player> = emptyList(),
    val currentQuestion: QuizQuestion? = null,
    val playbackStatus: PlaybackStatus = PlaybackStatus(),
    val joinUrl: String = "",
    val spotifySetupUrl: String = ""
)
