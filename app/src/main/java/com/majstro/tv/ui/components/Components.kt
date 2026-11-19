package com.majstro.tv.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.majstro.tv.R
import com.majstro.tv.domain.model.GameState
import com.majstro.tv.domain.model.MusicalEra
import com.majstro.tv.domain.model.PlaybackMode
import com.majstro.tv.domain.model.Player
import com.majstro.tv.domain.model.QuizPhase
import com.majstro.tv.ui.generateQrCodeBitmap
import com.majstro.tv.ui.theme.CormorantGaramondFamily
import com.majstro.tv.ui.theme.InterFamily
import com.majstro.tv.ui.theme.MajstroColors

/** Maps each [MusicalEra] to its bundled classical oil-painting drawable. */
@DrawableRes
fun eraArtworkRes(era: MusicalEra): Int = when (era) {
    MusicalEra.BAROQUE -> R.drawable.era_baroque
    MusicalEra.CLASSICAL -> R.drawable.era_classical
    MusicalEra.ROMANTIC -> R.drawable.era_romantic
    MusicalEra.IMPRESSIONIST -> R.drawable.era_impressionist
    MusicalEra.MODERN -> R.drawable.era_modern
}

/** Minimalist concentric-ring vinyl emblem echoing the classical salon header. */
@Composable
fun VinylEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    accentColor: Color = MajstroColors.Champagne
) {
    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(
            color = MajstroColors.WarmIvory.copy(alpha = 0.72f),
            radius = radius - 1.dp.toPx(),
            center = center,
            style = Stroke(width = 1.2.dp.toPx())
        )
        drawCircle(
            color = MajstroColors.WarmIvory.copy(alpha = 0.25f),
            radius = radius * 0.68f,
            center = center,
            style = Stroke(width = 0.8.dp.toPx())
        )
        drawCircle(
            color = accentColor,
            radius = radius * 0.36f,
            center = center,
            style = Stroke(width = 1.1.dp.toPx())
        )
        drawCircle(
            color = MajstroColors.WarmIvory,
            radius = 2.dp.toPx(),
            center = center
        )
    }
}

/** Quiet editorial top bar with vinyl brand mark, match context, and Spotify modal / overflow menu. */
@Composable
fun TopHeader(
    state: GameState,
    onOpenSpotifyModal: () -> Unit,
    onRequestExitToLobby: () -> Unit = {}
) {
    val isSpotify = state.playbackStatus.mode == PlaybackMode.SPOTIFY_CONNECT
    val statusDotColor = if (isSpotify) MajstroColors.SpotifyGreen else MajstroColors.Champagne
    val statusLabel = if (isSpotify) {
        state.playbackStatus.activeDeviceName?.let { "Spotify · $it" } ?: "Spotify Connect"
    } else {
        "Demo Audio · Setup Spotify"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VinylEmblem(size = 34.dp)
            Text(
                text = "MAJSTRO",
                fontFamily = CormorantGaramondFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 3.sp,
                color = MajstroColors.WarmIvory
            )
        }

        // Quiet center context during an active match
        val centerContext = when (state.phase) {
            QuizPhase.LOBBY -> null
            QuizPhase.QUESTION, QuizPhase.REVEAL -> state.currentQuestion?.let {
                "SONG ${it.roundNumber} OF ${it.totalRounds}  ·  ${state.selectedAnswerMode.label.uppercase()}"
            }
            QuizPhase.GAME_OVER -> "CONCERT FINALE"
        }
        if (centerContext != null) {
            Text(
                text = centerContext,
                style = MaterialTheme.typography.labelSmall,
                color = MajstroColors.CoolMuted
            )
        }

        if (state.phase == QuizPhase.LOBBY) {
            Surface(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onOpenSpotifyModal),
                color = MajstroColors.SlateElevated,
                shape = CircleShape,
                border = BorderStroke(
                    1.dp,
                    if (isSpotify) MajstroColors.SpotifyGreen.copy(alpha = 0.45f) else MajstroColors.Hairline
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Text(
                        text = statusLabel,
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MajstroColors.WarmIvory,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            var menuExpanded by remember { mutableStateOf(false) }
            Box {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable { menuExpanded = true },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.5.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(3.5.dp)
                                    .clip(CircleShape)
                                    .background(MajstroColors.CoolMuted)
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = MajstroColors.SlateSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MajstroColors.Hairline)
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(statusDotColor)
                                )
                                Text(
                                    text = statusLabel,
                                    fontFamily = InterFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MajstroColors.WarmIvory
                                )
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenSpotifyModal()
                        }
                    )
                    HorizontalDivider(color = MajstroColors.Hairline, thickness = 1.dp)
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Exit to Lobby",
                                fontFamily = InterFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MajstroColors.WarmIvory
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onRequestExitToLobby()
                        }
                    )
                }
            }
        }
    }
}

/** Uppercase tracked section header in warm champagne. */
@Composable
fun EditorialSectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MajstroColors.Champagne
    )
}

/** Borderless tonal segmented option with a subtle champagne bottom accent when active. */
@Composable
fun TonalSegmentedChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MajstroColors.Champagne.copy(alpha = 0.14f) else MajstroColors.SlateElevated,
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) MajstroColors.ChampagneBright else MajstroColors.WarmIvory.copy(alpha = 0.85f),
        label = "chipText"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(top = 11.dp, bottom = 9.dp, start = 10.dp, end = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontFamily = InterFamily,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .width(22.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(if (selected) MajstroColors.Champagne else Color.Transparent)
            )
        }
    }
}

/** Primary pill CTA filled with warm champagne and deep midnight ink. */
@Composable
fun ChampagnePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 26.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MajstroColors.Champagne,
            contentColor = MajstroColors.DeepInk
        )
    ) {
        Text(
            text = text,
            fontFamily = InterFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp
        )
    }
}

/** Secondary pill button outlined in warm champagne, echoing the reference PLAY NOW style. */
@Composable
fun OutlinedChampagneButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = CircleShape,
        border = BorderStroke(1.dp, MajstroColors.Champagne.copy(alpha = 0.65f)),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MajstroColors.WarmIvory
        )
    ) {
        Text(
            text = text,
            fontFamily = InterFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp
        )
    }
}

/** Unboxed editorial italic quote with a thin champagne vertical rule. */
@Composable
fun EditorialClueQuote(
    clue: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .width(2.dp)
                .height(38.dp)
                .background(MajstroColors.Champagne.copy(alpha = 0.75f))
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = clue,
            fontFamily = CormorantGaramondFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 19.sp,
            lineHeight = 25.sp,
            color = MajstroColors.WarmIvory.copy(alpha = 0.9f)
        )
    }
}

/** Clean hairline-separated player row without card borders. */
@Composable
fun PlayerRosterRow(
    player: Player,
    showScore: Boolean = true,
    showDelta: Boolean = false,
    statusText: String? = null,
    statusHighlight: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = player.name,
                fontFamily = InterFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MajstroColors.WarmIvory
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (statusText != null) {
                    Text(
                        text = statusText,
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        fontWeight = if (statusHighlight) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (statusHighlight) MajstroColors.SageGreen else MajstroColors.CoolMuted
                    )
                }
                if (showDelta && player.lastRoundDelta > 0) {
                    Text(
                        text = "+${player.lastRoundDelta}",
                        fontFamily = InterFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MajstroColors.SageGreen
                    )
                }
                if (showScore) {
                    Text(
                        text = "${player.score} pts",
                        fontFamily = InterFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MajstroColors.Champagne
                    )
                }
            }
        }
        HorizontalDivider(color = MajstroColors.Hairline, thickness = 1.dp)
    }
}

/** Modal dialog for Spotify Connect setup triggered from the quiet top header. */
@Composable
fun SpotifySetupDialog(
    state: GameState,
    onOpenSpotifySetup: () -> Unit,
    onDismiss: () -> Unit
) {
    val spotifyQr = remember(state.spotifySetupUrl) {
        generateQrCodeBitmap(state.spotifySetupUrl, 220)
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false)
    ) {
        Surface(
            color = MajstroColors.SlateSurface,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, MajstroColors.Hairline)
        ) {
            Column(
                modifier = Modifier
                    .width(380.dp)
                    .padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Spotify Connect",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MajstroColors.WarmIvory
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = state.playbackStatus.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MajstroColors.CoolMuted
                )
                Spacer(modifier = Modifier.height(18.dp))
                if (spotifyQr != null) {
                    Image(
                        bitmap = spotifyQr,
                        contentDescription = "Spotify Setup QR Code",
                        modifier = Modifier
                            .size(164.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MajstroColors.Parchment)
                            .padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = state.spotifySetupUrl,
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    color = MajstroColors.CoolMuted
                )
                Spacer(modifier = Modifier.height(20.dp))
                ChampagnePillButton(
                    text = "Open Setup on Tablet",
                    onClick = {
                        onDismiss()
                        onOpenSpotifySetup()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedChampagneButton(
                    text = "Close",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** Confirmation dialog before quitting an in-progress match back to the lobby. */
@Composable
fun ExitToLobbyConfirmDialog(
    onConfirmExit: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false)
    ) {
        Surface(
            color = MajstroColors.SlateSurface,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, MajstroColors.Hairline)
        ) {
            Column(
                modifier = Modifier
                    .width(360.dp)
                    .padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Exit to Lobby?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MajstroColors.WarmIvory
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your current game progress and scores will be lost.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MajstroColors.CoolMuted
                )
                Spacer(modifier = Modifier.height(22.dp))
                ChampagnePillButton(
                    text = "Exit to Lobby",
                    onClick = onConfirmExit,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedChampagneButton(
                    text = "Keep Playing",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
