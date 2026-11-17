package com.majstro.tv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majstro.tv.R
import com.majstro.tv.domain.model.AnswerMode
import com.majstro.tv.domain.model.GameState
import com.majstro.tv.ui.components.ChampagnePillButton
import com.majstro.tv.ui.components.EditorialSectionLabel
import com.majstro.tv.ui.components.PlayerRosterRow
import com.majstro.tv.ui.components.TonalSegmentedChip
import com.majstro.tv.ui.generateQrCodeBitmap
import com.majstro.tv.ui.theme.CormorantGaramondFamily
import com.majstro.tv.ui.theme.InterFamily
import com.majstro.tv.ui.theme.MajstroColors

private fun difficultyLabel(level: Int): String = when (level) {
    1 -> "Iconic Classics"
    2 -> "Concert Favorites"
    3 -> "Standard Repertoire"
    4 -> "Musician"
    5 -> "Virtuoso"
    else -> "Level $level"
}

/** 2-zone editorial lobby with classical hero, warm parchment QR, and borderless setup canvas. */
@Composable
fun LobbyScreen(
    state: GameState,
    onSelectRoundCount: (Int) -> Unit,
    onSelectDifficulty: (Int) -> Unit,
    onSelectAnswerMode: (AnswerMode) -> Unit,
    onStartGame: () -> Unit
) {
    val joinQr = remember(state.joinUrl) { generateQrCodeBitmap(state.joinUrl, 280) }
    val songOptions = listOf(3, 5, 8, 10, 15)
    val difficultyLevels = listOf(1, 2, 3, 4, 5)

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        // Left editorial rail: Hero artwork, salon title, and warm-parchment Join QR
        Column(
            modifier = Modifier
                .weight(0.82f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Image(
                    painter = painterResource(id = R.drawable.hero_concert_hall),
                    contentDescription = "Vintage violin on classical sheet music",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(18.dp))
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "An Evening of\nClassical Masterworks",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MajstroColors.WarmIvory
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "25 curated works  ·  5 difficulty tiers",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MajstroColors.CoolMuted
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Tonal borderless join bar inspired by the reference active track pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MajstroColors.SlateSurface)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (joinQr != null) {
                    Image(
                        bitmap = joinQr,
                        contentDescription = "Join QR Code",
                        modifier = Modifier
                            .size(104.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MajstroColors.Parchment)
                            .padding(6.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    EditorialSectionLabel("JOIN WITH YOUR PHONE")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.joinUrl.ifBlank { "Starting local server…" },
                        fontFamily = InterFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MajstroColors.WarmIvory
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scan over Wi-Fi to submit guesses from phones.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MajstroColors.CoolMuted
                    )
                }
            }
        }

        // Right open canvas: Program setup, live player roster, and champagne CTA
        Column(
            modifier = Modifier
                .weight(1.32f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                EditorialSectionLabel("PROGRAM LENGTH")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    songOptions.forEach { count ->
                        TonalSegmentedChip(
                            label = "$count",
                            selected = state.selectedRoundCount == count,
                            onClick = { onSelectRoundCount(count) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                EditorialSectionLabel(
                    "DIFFICULTY  ·  ${state.selectedDifficulty} — ${difficultyLabel(state.selectedDifficulty)}"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    difficultyLevels.forEach { level ->
                        TonalSegmentedChip(
                            label = "$level",
                            selected = state.selectedDifficulty == level,
                            onClick = { onSelectDifficulty(level) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                EditorialSectionLabel("ANSWER FORMAT")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnswerMode.entries.forEach { mode ->
                        TonalSegmentedChip(
                            label = mode.label,
                            selected = state.selectedAnswerMode == mode,
                            onClick = { onSelectAnswerMode(mode) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                EditorialSectionLabel("SALON PLAYERS (${state.players.size})")
                Spacer(modifier = Modifier.height(6.dp))
                if (state.players.isEmpty()) {
                    Text(
                        text = "No phones joined yet — scan the code or start right away to play on this tablet.",
                        fontFamily = CormorantGaramondFamily,
                        fontStyle = FontStyle.Italic,
                        fontSize = 18.sp,
                        color = MajstroColors.CoolMuted,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    state.players.forEach { player ->
                        PlayerRosterRow(player = player, showScore = false, showDelta = false)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.selectedRoundCount} works  ·  ${state.selectedAnswerMode.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MajstroColors.CoolMuted,
                    modifier = Modifier.padding(end = 18.dp)
                )
                ChampagnePillButton(
                    text = "Start Quiz",
                    onClick = onStartGame
                )
            }
        }
    }
}
