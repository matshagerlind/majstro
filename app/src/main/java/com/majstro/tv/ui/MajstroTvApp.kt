package com.majstro.tv.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majstro.tv.domain.engine.QuizEngine
import com.majstro.tv.domain.model.AnswerMode
import com.majstro.tv.domain.model.AwardField
import com.majstro.tv.domain.model.GameState
import com.majstro.tv.domain.model.PlaybackMode
import com.majstro.tv.domain.model.Player
import com.majstro.tv.domain.model.QuizPhase
import com.majstro.tv.domain.model.QuizQuestion
import kotlin.math.abs

private val ConcertBg = Color(0xFF0E151C)
private val ConcertCard = Color(0xFF141E28)
private val ConcertCardElevated = Color(0xFF1A2532)
private val ConcertBorder = Color(0xFF253342)
private val ConcertGold = Color(0xFFDFB49B)
private val ConcertGoldBright = Color(0xFFECC8B2)
private val ConcertGreen = Color(0xFF4E9F76)
private val ConcertSpotify = Color(0xFF25B864)
private val ConcertText = Color(0xFFF2EFEA)
private val ConcertMuted = Color(0xFF8E9BA8)
private val ConcertOnPrimary = Color(0xFF10171E)

private val MajstroTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.4.sp
    )
)

/** Landscape Tablet UI for Majstro. */
@Composable
fun MajstroTvApp(
    engine: QuizEngine,
    onStartGame: () -> Unit,
    onRevealAnswer: () -> Unit,
    onNextRound: () -> Unit,
    onBackToLobby: () -> Unit,
    onOpenSpotifySetup: () -> Unit
) {
    val state by engine.state.collectAsState()

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = ConcertGold,
            onPrimary = ConcertOnPrimary,
            surface = ConcertCard,
            onSurface = ConcertText,
            background = ConcertBg,
            onBackground = ConcertText,
            outline = ConcertBorder
        ),
        typography = MajstroTypography
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ConcertBg)
                .padding(horizontal = 28.dp, vertical = 20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopHeader(state = state)
                Spacer(modifier = Modifier.height(14.dp))
                Box(modifier = Modifier.weight(1f)) {
                    when (state.phase) {
                        QuizPhase.LOBBY -> LobbyScreen(
                            state = state,
                            onSelectRoundCount = engine::setRoundCount,
                            onSelectDifficulty = engine::setDifficultyLevel,
                            onSelectAnswerMode = engine::setAnswerMode,
                            onStartGame = onStartGame,
                            onOpenSpotifySetup = onOpenSpotifySetup
                        )
                        QuizPhase.QUESTION -> QuestionScreen(
                            state = state,
                            onSelectOption = { index ->
                                engine.submitAnswer(QuizEngine.HOST_PLAYER_ID, index)
                            },
                            onRevealAnswer = onRevealAnswer
                        )
                        QuizPhase.REVEAL -> RevealScreen(
                            state = state,
                            onToggleAward = engine::togglePlayerAward,
                            onNextRound = onNextRound
                        )
                        QuizPhase.GAME_OVER -> GameOverScreen(
                            state = state,
                            onPlayAgain = onStartGame,
                            onBackToLobby = onBackToLobby
                        )
                    }
                }
            }
        }
    }
}

/** Material 3 touch button styled for the Midnight Classical palette. */
@Composable
private fun MajstroButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = ConcertCardElevated,
    contentColor: Color = ConcertText,
    selected: Boolean = false,
    awarded: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable () -> Unit
) {
    val effectiveContainer = when {
        awarded -> ConcertGreen.copy(alpha = 0.22f)
        selected -> ConcertGold.copy(alpha = 0.16f)
        else -> containerColor
    }
    val borderColor = when {
        awarded -> ConcertGreen
        selected -> ConcertGold
        containerColor == ConcertGold -> ConcertGold
        else -> ConcertBorder
    }

    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(width = 1.dp, color = borderColor),
        contentPadding = contentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = effectiveContainer,
            contentColor = contentColor
        )
    ) {
        content()
    }
}

private fun difficultyLabel(level: Int): String = when (level) {
    1 -> "1 · Iconic Classics"
    2 -> "2 · Concert Favorites"
    3 -> "3 · Standard Repertoire"
    4 -> "4 · Musician"
    5 -> "5 · Virtuoso"
    else -> "Level $level"
}

@Composable
private fun TopHeader(state: GameState) {
    val isSpotify = state.playbackStatus.mode == PlaybackMode.SPOTIFY_CONNECT
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Minimalist vinyl/record emblem matching the reference header
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = ConcertCardElevated,
                border = BorderStroke(1.dp, ConcertText.copy(alpha = 0.65f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.size(16.dp),
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, ConcertGold)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(ConcertText)
                            )
                        }
                    }
                }
            }

            Column {
                Text(
                    text = "MAJSTRO",
                    fontFamily = FontFamily.Serif,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = ConcertText,
                    letterSpacing = 2.5.sp
                )
                Text(
                    text = "Classical Music Quiz · ${state.selectedAnswerMode.label} · Diff ${state.selectedDifficulty}/5",
                    fontSize = 12.sp,
                    color = ConcertMuted
                )
            }
        }

        Surface(
            color = if (isSpotify) ConcertSpotify.copy(alpha = 0.16f) else ConcertCard,
            border = BorderStroke(
                width = 1.dp,
                color = if (isSpotify) ConcertSpotify.copy(alpha = 0.7f) else ConcertBorder
            ),
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(
                text = "${state.playbackStatus.mode.label} · ${state.playbackStatus.message}",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                fontSize = 12.sp,
                color = ConcertText
            )
        }
    }
}

@Composable
private fun LobbyScreen(
    state: GameState,
    onSelectRoundCount: (Int) -> Unit,
    onSelectDifficulty: (Int) -> Unit,
    onSelectAnswerMode: (AnswerMode) -> Unit,
    onStartGame: () -> Unit,
    onOpenSpotifySetup: () -> Unit
) {
    val joinQr = remember(state.joinUrl) { generateQrCodeBitmap(state.joinUrl, 280) }
    val spotifyQr = remember(state.spotifySetupUrl) { generateQrCodeBitmap(state.spotifySetupUrl, 180) }
    val songOptions = listOf(3, 5, 8, 10, 15)
    val difficultyLevels = listOf(1, 2, 3, 4, 5)

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Left column: Phone Join QR
        Surface(
            modifier = Modifier
                .weight(0.85f)
                .fillMaxSize(),
            color = ConcertCard,
            border = BorderStroke(1.dp, ConcertBorder),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Join With Your Phone",
                    fontFamily = FontFamily.Serif,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Normal,
                    color = ConcertText
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (joinQr != null) {
                    Image(
                        bitmap = joinQr,
                        contentDescription = "Join QR Code",
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = state.joinUrl.ifBlank { "Starting local server…" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = ConcertText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Scan on phones over Wi-Fi to join & write guesses.",
                    fontSize = 12.sp,
                    color = ConcertMuted
                )
            }
        }

        // Middle column: Game Setup (Songs, Difficulty 1-5, Mode), Players, Start Button
        Column(
            modifier = Modifier
                .weight(1.45f)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = ConcertCard,
                border = BorderStroke(1.dp, ConcertBorder),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp)
                ) {
                    Text(
                        text = "SONGS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.4.sp,
                        color = ConcertGold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        songOptions.forEach { count ->
                            MajstroButton(
                                onClick = { onSelectRoundCount(count) },
                                modifier = Modifier.weight(1f),
                                selected = state.selectedRoundCount == count,
                                contentPadding = PaddingValues(vertical = 9.dp)
                            ) {
                                Text(text = "$count", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "DIFFICULTY · ${difficultyLabel(state.selectedDifficulty).uppercase()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.4.sp,
                        color = ConcertGold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        difficultyLevels.forEach { level ->
                            MajstroButton(
                                onClick = { onSelectDifficulty(level) },
                                modifier = Modifier.weight(1f),
                                selected = state.selectedDifficulty == level,
                                contentPadding = PaddingValues(vertical = 9.dp)
                            ) {
                                Text(text = "$level", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "MODE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.4.sp,
                        color = ConcertGold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnswerMode.entries.forEach { mode ->
                            MajstroButton(
                                onClick = { onSelectAnswerMode(mode) },
                                modifier = Modifier.weight(1f),
                                selected = state.selectedAnswerMode == mode,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                Text(text = mode.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Players (${state.players.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ConcertText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (state.players.isEmpty()) {
                        Text(
                            text = "No phones joined yet — scan QR or press Start to play on screen.",
                            fontSize = 13.sp,
                            color = ConcertMuted
                        )
                    } else {
                        state.players.forEach { player ->
                            PlayerRow(player = player, showDelta = false)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            MajstroButton(
                onClick = onStartGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                containerColor = ConcertGold,
                contentColor = ConcertOnPrimary
            ) {
                Text(
                    text = "Start ${state.selectedRoundCount}-Song Quiz (${state.selectedAnswerMode.label})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Right column: Spotify Setup
        Surface(
            modifier = Modifier
                .weight(0.75f)
                .fillMaxSize(),
            color = ConcertCard,
            border = BorderStroke(1.dp, ConcertBorder),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Spotify Connect",
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = ConcertSpotify
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (spotifyQr != null) {
                    Image(
                        bitmap = spotifyQr,
                        contentDescription = "Spotify Setup QR Code",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .padding(6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.spotifySetupUrl,
                    fontSize = 11.sp,
                    color = ConcertText
                )
                Spacer(modifier = Modifier.height(10.dp))
                MajstroButton(
                    onClick = onOpenSpotifySetup,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = ConcertSpotify.copy(alpha = 0.2f),
                    contentColor = Color(0xFF7EE787),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = "Open Setup on Device",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Works in Demo Mode without setup.",
                    fontSize = 11.sp,
                    color = ConcertMuted
                )
            }
        }
    }
}

@Composable
private fun QuestionScreen(
    state: GameState,
    onSelectOption: (Int) -> Unit,
    onRevealAnswer: () -> Unit
) {
    val question: QuizQuestion = state.currentQuestion ?: return
    val labels = listOf("A", "B", "C", "D")
    val hostChoice = state.players.find { it.id == QuizEngine.HOST_PLAYER_ID }?.selectedOptionIndex
    val isWriteIn = state.selectedAnswerMode == AnswerMode.WRITE_IN

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1.7f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "SONG ${question.roundNumber} OF ${question.totalRounds} · LEVEL ${question.work.difficultyLevel}/5 · ${state.selectedAnswerMode.label.uppercase()}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = ConcertGold,
                letterSpacing = 1.6.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isWriteIn) {
                    "Listen carefully and write your guesses:"
                } else {
                    question.prompt
                },
                fontFamily = FontFamily.Serif,
                fontSize = 25.sp,
                fontWeight = FontWeight.Normal,
                color = ConcertText
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (isWriteIn) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ConcertCard,
                    border = BorderStroke(1.dp, ConcertGold.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Write on your phone (or paper) before revealing:",
                            fontSize = 14.sp,
                            color = ConcertMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ScoringBadge("Composer", "+1 pt", Modifier.weight(1f))
                            ScoringBadge("Piece", "+2 pts", Modifier.weight(1f))
                            ScoringBadge("Year Written", "+1 pt closest", Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ConcertCard,
                border = BorderStroke(1.dp, ConcertGold.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "STYLISTIC CLUE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConcertGold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = question.work.stylisticClue,
                        fontSize = 15.sp,
                        color = ConcertText,
                        lineHeight = 21.sp
                    )
                }
            }

            if (!isWriteIn) {
                Spacer(modifier = Modifier.height(14.dp))
                question.options.forEachIndexed { index, option ->
                    val isSelected = hostChoice == index
                    MajstroButton(
                        onClick = { onSelectOption(index) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        selected = isSelected,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "${labels[index]}.  $option",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Right sidebar: Player lock-in status & Reveal button
        Column(
            modifier = Modifier
                .weight(0.95f)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = ConcertCard,
                border = BorderStroke(1.dp, ConcertBorder),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Players",
                        fontFamily = FontFamily.Serif,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = ConcertText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    state.players.forEach { player ->
                        val hasSubmitted = if (isWriteIn) {
                            player.writtenAnswer != null
                        } else {
                            player.selectedOptionIndex != null
                        }
                        val statusText = when {
                            isWriteIn && hasSubmitted -> "Guess submitted ✓"
                            isWriteIn -> "Writing…"
                            hasSubmitted -> "Locked in"
                            else -> "Listening…"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = player.name, fontSize = 15.sp, color = ConcertText)
                            Text(
                                text = statusText,
                                fontSize = 13.sp,
                                color = if (hasSubmitted) ConcertGreen else ConcertMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            MajstroButton(
                onClick = onRevealAnswer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                containerColor = ConcertGold,
                contentColor = ConcertOnPrimary
            ) {
                Text(
                    text = "Reveal Answer",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ScoringBadge(title: String, pts: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = ConcertCardElevated,
        border = BorderStroke(1.dp, ConcertBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ConcertText)
            Text(text = pts, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ConcertGold)
        }
    }
}

@Composable
private fun RevealScreen(
    state: GameState,
    onToggleAward: (String, AwardField) -> Unit,
    onNextRound: () -> Unit
) {
    val question = state.currentQuestion ?: return
    val work = question.work
    val isWriteIn = state.selectedAnswerMode == AnswerMode.WRITE_IN

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Left card: Authoritative Revealed Answer (Piece, Composer, Year Written)
        Surface(
            modifier = Modifier
                .weight(1.05f)
                .fillMaxSize(),
            color = ConcertCard,
            border = BorderStroke(1.dp, ConcertGreen),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ANSWER REVEALED",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConcertGreen,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = work.title,
                        fontFamily = FontFamily.Serif,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Normal,
                        color = ConcertText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = work.composer,
                        fontFamily = FontFamily.Serif,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Normal,
                        color = ConcertGold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = ConcertGold.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, ConcertGold),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Year Written: ${work.yearWritten}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConcertGoldBright
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${work.catalogNumber} · ${work.era.label} (${work.era.years}) · ${work.styleOrForm}",
                        fontSize = 14.sp,
                        color = ConcertMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = work.stylisticClue,
                        fontSize = 15.sp,
                        color = ConcertText,
                        lineHeight = 22.sp
                    )
                }

                if (!isWriteIn) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Correct option: ${question.options[question.correctIndex]}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ConcertGreen
                    )
                }
            }
        }

        // Right column: Judging (in Write-In) or Scoreboard + Next Song
        Column(
            modifier = Modifier
                .weight(1.25f)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = ConcertCard,
                border = BorderStroke(1.dp, ConcertBorder),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp)
                ) {
                    Text(
                        text = if (isWriteIn) "Confirm Who Was Right" else "Scoreboard",
                        fontFamily = FontFamily.Serif,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = ConcertText
                    )
                    if (isWriteIn) {
                        Text(
                            text = "Tap to award Composer (+1), Piece (+2), and Closest Year (+1):",
                            fontSize = 12.sp,
                            color = ConcertMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    state.players.forEach { player ->
                        if (isWriteIn) {
                            PlayerJudgeCard(
                                player = player,
                                trueYear = work.yearWritten,
                                onToggleAward = { field -> onToggleAward(player.id, field) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        } else {
                            PlayerRow(player = player, showDelta = true)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            MajstroButton(
                onClick = onNextRound,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                containerColor = ConcertGold,
                contentColor = ConcertOnPrimary
            ) {
                Text(
                    text = if (question.roundNumber >= question.totalRounds) "Final Standings" else "Next Song",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PlayerJudgeCard(
    player: Player,
    trueYear: Int,
    onToggleAward: (AwardField) -> Unit
) {
    val written = player.writtenAnswer
    val yearDiffText = written?.yearGuess?.let { guess ->
        val diff = abs(guess - trueYear)
        if (diff == 0) "$guess (exact!)" else "$guess (${diff}y off)"
    } ?: "—"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ConcertCardElevated,
        border = BorderStroke(1.dp, ConcertBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = player.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConcertText
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (player.lastRoundDelta > 0) {
                        Text(
                            text = "+${player.lastRoundDelta}  ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConcertGreen
                        )
                    }
                    Text(
                        text = "${player.score} pts",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ConcertGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            if (written != null) {
                Text(
                    text = "Composer: ${written.composerGuess.ifBlank { "—" }}   ·   Piece: ${written.pieceGuess.ifBlank { "—" }}   ·   Year: $yearDiffText",
                    fontSize = 13.sp,
                    color = ConcertMuted
                )
            } else {
                Text(
                    text = "No written entry submitted (judge oral/paper guess)",
                    fontSize = 12.sp,
                    color = ConcertMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MajstroButton(
                    onClick = { onToggleAward(AwardField.COMPOSER) },
                    modifier = Modifier.weight(1f),
                    awarded = player.roundAward.composerCorrect,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(text = "Composer +1", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                MajstroButton(
                    onClick = { onToggleAward(AwardField.PIECE) },
                    modifier = Modifier.weight(1f),
                    awarded = player.roundAward.pieceCorrect,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(text = "Piece +2", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                MajstroButton(
                    onClick = { onToggleAward(AwardField.CLOSEST_YEAR) },
                    modifier = Modifier.weight(1f),
                    awarded = player.roundAward.closestYear,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(text = "Closest Yr +1", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun GameOverScreen(
    state: GameState,
    onPlayAgain: () -> Unit,
    onBackToLobby: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "CONCERT FINALE · FINAL STANDINGS",
            fontFamily = FontFamily.Serif,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            color = ConcertGold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.width(540.dp),
            color = ConcertCard,
            border = BorderStroke(1.dp, ConcertBorder),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                state.players.forEachIndexed { index, player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "#${index + 1}  ${player.name}",
                            fontSize = 20.sp,
                            fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (index == 0) ConcertGold else ConcertText
                        )
                        Text(
                            text = "${player.score} pts",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConcertText
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MajstroButton(
                onClick = onPlayAgain,
                containerColor = ConcertGold,
                contentColor = ConcertOnPrimary
            ) {
                Text("Play Again", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }

            MajstroButton(
                onClick = onBackToLobby,
                containerColor = ConcertCardElevated,
                contentColor = ConcertText
            ) {
                Text("Back to Lobby", fontSize = 17.sp)
            }
        }
    }
}

@Composable
private fun PlayerRow(player: Player, showDelta: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = player.name,
            fontSize = 16.sp,
            color = ConcertText
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showDelta && player.lastRoundDelta > 0) {
                Text(
                    text = "+${player.lastRoundDelta}  ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConcertGreen
                )
            }
            Text(
                text = "${player.score} pts",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = ConcertGold
            )
        }
    }
}
