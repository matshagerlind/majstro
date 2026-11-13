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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majstro.tv.domain.engine.QuizEngine
import com.majstro.tv.domain.model.GameState
import com.majstro.tv.domain.model.PlaybackMode
import com.majstro.tv.domain.model.Player
import com.majstro.tv.domain.model.QuizPhase
import com.majstro.tv.domain.model.QuizQuestion

private val ConcertBg = Color(0xFF0D1117)
private val ConcertCard = Color(0xFF161B22)
private val ConcertGold = Color(0xFFD4AF37)
private val ConcertGoldBright = Color(0xFFF3D56B)
private val ConcertGreen = Color(0xFF2EA043)
private val ConcertSpotify = Color(0xFF1DB954)
private val ConcertText = Color(0xFFF0F4F8)
private val ConcertMuted = Color(0xFF9DA7B3)

/** Unified Landscape UI supporting both Android TV (D-pad) and Tablets (touch). */
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
            onPrimary = Color(0xFF111111),
            surface = ConcertCard,
            onSurface = ConcertText,
            background = ConcertBg,
            onBackground = ConcertText
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ConcertBg)
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopHeader(state = state)
                Spacer(modifier = Modifier.height(18.dp))
                Box(modifier = Modifier.weight(1f)) {
                    when (state.phase) {
                        QuizPhase.LOBBY -> LobbyScreen(
                            state = state,
                            onStartGame = onStartGame,
                            onOpenSpotifySetup = onOpenSpotifySetup
                        )
                        QuizPhase.QUESTION -> QuestionScreen(
                            state = state,
                            onSelectOption = { index ->
                                engine.submitAnswer(QuizEngine.TV_PLAYER_ID, index)
                            },
                            onRevealAnswer = onRevealAnswer
                        )
                        QuizPhase.REVEAL -> RevealScreen(
                            state = state,
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

/** Material 3 button with a visible gold focus ring for TV D-pad navigation and instant touch response on tablets. */
@Composable
private fun HybridButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = ConcertCard,
    contentColor: Color = ConcertText,
    selected: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    content: @Composable () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val effectiveContainer = when {
        isFocused && containerColor == ConcertGold -> ConcertGoldBright
        isFocused -> ConcertGold.copy(alpha = 0.25f)
        selected -> ConcertGold.copy(alpha = 0.22f)
        else -> containerColor
    }
    val borderColor = when {
        isFocused -> ConcertGoldBright
        selected -> ConcertGold
        containerColor == ConcertGold -> ConcertGold
        else -> Color(0xFF30363D)
    }

    Button(
        onClick = onClick,
        modifier = modifier.onFocusChanged { isFocused = it.isFocused },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(width = if (isFocused) 2.5.dp else 1.dp, color = borderColor),
        contentPadding = contentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = effectiveContainer,
            contentColor = contentColor
        )
    ) {
        content()
    }
}

@Composable
private fun TopHeader(state: GameState) {
    val isSpotify = state.playbackStatus.mode == PlaybackMode.SPOTIFY_CONNECT
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "MAJSTRO",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ConcertGold,
                letterSpacing = 2.sp
            )
            Text(
                text = "Classical Music Quiz · TV & Tablet",
                fontSize = 13.sp,
                color = ConcertMuted
            )
        }

        Surface(
            color = if (isSpotify) ConcertSpotify.copy(alpha = 0.2f) else ConcertCard,
            border = BorderStroke(
                width = 1.dp,
                color = if (isSpotify) ConcertSpotify else ConcertGold.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(
                text = "${state.playbackStatus.mode.label} · ${state.playbackStatus.message}",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                fontSize = 13.sp,
                color = ConcertText
            )
        }
    }
}

@Composable
private fun LobbyScreen(
    state: GameState,
    onStartGame: () -> Unit,
    onOpenSpotifySetup: () -> Unit
) {
    val joinQr = remember(state.joinUrl) { generateQrCodeBitmap(state.joinUrl, 280) }
    val spotifyQr = remember(state.spotifySetupUrl) { generateQrCodeBitmap(state.spotifySetupUrl, 180) }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Left column: Phone Join QR
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            color = ConcertCard,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Join With Your Phone",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ConcertGold
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (joinQr != null) {
                    Image(
                        bitmap = joinQr,
                        contentDescription = "Join QR Code",
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = state.joinUrl.ifBlank { "Starting local server…" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = ConcertText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Scan on phones over Wi-Fi, or play directly on this screen",
                    fontSize = 12.sp,
                    color = ConcertMuted
                )
            }
        }

        // Middle column: Connected Players & Start Button
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = ConcertCard,
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Concert Hall Lobby (${state.players.size} players)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ConcertText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (state.players.isEmpty()) {
                        Text(
                            text = "No phones connected yet.\nScan the QR code to join, or press Start to play directly on this TV or tablet!",
                            fontSize = 15.sp,
                            color = ConcertMuted,
                            lineHeight = 22.sp
                        )
                    } else {
                        state.players.forEach { player ->
                            PlayerRow(player = player, showDelta = false)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            HybridButton(
                onClick = onStartGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                containerColor = ConcertGold,
                contentColor = Color(0xFF111111)
            ) {
                Text(
                    text = "Start 5-Round Classical Quiz",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Right column: Spotify Setup (QR for TV + Tap button for Tablet)
        Surface(
            modifier = Modifier
                .weight(0.85f)
                .fillMaxSize(),
            color = ConcertCard,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Spotify Connect",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ConcertSpotify
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (spotifyQr != null) {
                    Image(
                        bitmap = spotifyQr,
                        contentDescription = "Spotify Setup QR Code",
                        modifier = Modifier
                            .size(130.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .padding(6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.spotifySetupUrl,
                    fontSize = 12.sp,
                    color = ConcertText
                )
                Spacer(modifier = Modifier.height(10.dp))
                HybridButton(
                    onClick = onOpenSpotifySetup,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = ConcertSpotify.copy(alpha = 0.2f),
                    contentColor = Color(0xFF7EE787),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Open Setup on Device",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Works in Demo Mode without setup.",
                    fontSize = 12.sp,
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
    val tvChoice = state.players.find { it.id == QuizEngine.TV_PLAYER_ID }?.selectedOptionIndex

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
                text = "ROUND ${question.roundNumber} OF ${question.totalRounds} · ${question.category.label.uppercase()}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ConcertGold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = question.prompt,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ConcertText
            )
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ConcertCard,
                border = BorderStroke(1.dp, ConcertGold.copy(alpha = 0.4f)),
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

            Spacer(modifier = Modifier.height(14.dp))

            question.options.forEachIndexed { index, option ->
                val isSelected = tvChoice == index
                HybridButton(
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

        // Right sidebar: Player lock-in status & Reveal button
        Column(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = ConcertCard,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Players",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ConcertGold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    state.players.forEach { player ->
                        val statusText = if (player.selectedOptionIndex != null) "Locked in" else "Listening…"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = player.name, fontSize = 15.sp, color = ConcertText)
                            Text(
                                text = statusText,
                                fontSize = 13.sp,
                                color = if (player.selectedOptionIndex != null) ConcertGreen else ConcertMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            HybridButton(
                onClick = onRevealAnswer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                containerColor = ConcertGold,
                contentColor = Color(0xFF111111)
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
private fun RevealScreen(state: GameState, onNextRound: () -> Unit) {
    val question = state.currentQuestion ?: return
    val work = question.work

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1.4f)
                .fillMaxSize(),
            color = ConcertCard,
            border = BorderStroke(1.dp, ConcertGreen),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ANSWER REVEALED",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConcertGreen,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = work.title,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConcertText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${work.composer} (${work.catalogNumber})",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ConcertGold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Era: ${work.era.label} (${work.era.years})  ·  Form: ${work.styleOrForm}",
                        fontSize = 15.sp,
                        color = ConcertMuted
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = work.stylisticClue,
                        fontSize = 16.sp,
                        color = ConcertText,
                        lineHeight = 24.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Correct option: ${question.options[question.correctIndex]}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ConcertGreen
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = ConcertCard,
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Scoreboard",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ConcertGold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    state.players.forEach { player ->
                        PlayerRow(player = player, showDelta = true)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            HybridButton(
                onClick = onNextRound,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                containerColor = ConcertGold,
                contentColor = Color(0xFF111111)
            ) {
                Text(
                    text = if (question.roundNumber >= question.totalRounds) "Final Standings" else "Next Round",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
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
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = ConcertGold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.width(540.dp),
            color = ConcertCard,
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
            HybridButton(
                onClick = onPlayAgain,
                containerColor = ConcertGold,
                contentColor = Color(0xFF111111)
            ) {
                Text("Play Again", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }

            HybridButton(
                onClick = onBackToLobby,
                containerColor = ConcertCard,
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
            fontSize = 17.sp,
            color = ConcertText
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showDelta && player.lastRoundDelta > 0) {
                Text(
                    text = "+${player.lastRoundDelta}  ",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConcertGreen
                )
            }
            Text(
                text = "${player.score} pts",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = ConcertGold
            )
        }
    }
}
