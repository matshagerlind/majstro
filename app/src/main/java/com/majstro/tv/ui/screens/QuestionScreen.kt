package com.majstro.tv.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majstro.tv.domain.engine.QuizEngine
import com.majstro.tv.domain.model.AnswerMode
import com.majstro.tv.domain.model.GameState
import com.majstro.tv.domain.model.QuizQuestion
import com.majstro.tv.ui.components.ChampagnePillButton
import com.majstro.tv.ui.components.EditorialClueQuote
import com.majstro.tv.ui.components.EditorialSectionLabel
import com.majstro.tv.ui.components.PlayerRosterRow
import com.majstro.tv.ui.theme.InterFamily
import com.majstro.tv.ui.theme.MajstroColors

/** Calm "Listening Room" screen with rotating vinyl centerpiece, optional clue toggle, and 2x2 MC grid. */
@Composable
fun QuestionScreen(
    state: GameState,
    onSelectOption: (Int) -> Unit,
    onRevealAnswer: () -> Unit
) {
    val question: QuizQuestion = state.currentQuestion ?: return
    val labels = listOf("A", "B", "C", "D")
    val hostChoice = state.players.find { it.id == QuizEngine.HOST_PLAYER_ID }?.selectedOptionIndex
    val isWriteIn = state.selectedAnswerMode == AnswerMode.WRITE_IN
    var clueExpanded by remember(question.roundNumber, question.work.id) { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(36.dp)
    ) {
        // Left Listening Room canvas
        Column(
            modifier = Modifier
                .weight(1.65f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            EditorialSectionLabel(
                "SONG ${question.roundNumber} OF ${question.totalRounds}  ·  LEVEL ${question.work.difficultyLevel}/5"
            )
            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                ListeningVinylDisc(modifier = Modifier.size(148.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isWriteIn) {
                            "Listen closely and record your guesses."
                        } else {
                            question.prompt
                        },
                        style = MaterialTheme.typography.displaySmall,
                        color = MajstroColors.WarmIvory
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isWriteIn) {
                            "Composer +1 pt   ·   Piece +2 pts   ·   Closest Year +1 pt"
                        } else {
                            "Select one option on your phone or below on the tablet."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MajstroColors.CoolMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Quiet Stylistic Clue toggle pill — hidden by default so players listen first
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { clueExpanded = !clueExpanded },
                    color = if (clueExpanded) MajstroColors.Champagne.copy(alpha = 0.14f) else MajstroColors.SlateElevated,
                    shape = CircleShape,
                    border = BorderStroke(
                        1.dp,
                        if (clueExpanded) MajstroColors.Champagne.copy(alpha = 0.5f) else MajstroColors.Hairline
                    )
                ) {
                    Text(
                        text = if (clueExpanded) "Hide Stylistic Clue" else "Show Stylistic Clue",
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.6.sp,
                        color = if (clueExpanded) MajstroColors.ChampagneBright else MajstroColors.CoolMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = clueExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    EditorialClueQuote(clue = question.work.stylisticClue)
                }
            }

            // 2x2 Multiple Choice Grid on landscape tablet
            if (!isWriteIn) {
                Spacer(modifier = Modifier.height(22.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    question.options.chunked(2).forEachIndexed { rowIdx, pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEachIndexed { colIdx, option ->
                                val optionIndex = rowIdx * 2 + colIdx
                                val isSelected = hostChoice == optionIndex
                                MultipleChoicePill(
                                    label = labels[optionIndex],
                                    optionText = option,
                                    selected = isSelected,
                                    onClick = { onSelectOption(optionIndex) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Right status rail: Borderless tonal container + Reveal pill CTA
        Column(
            modifier = Modifier
                .weight(0.92f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MajstroColors.SlateSurface)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                EditorialSectionLabel("SALON STATUS")
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
                    PlayerRosterRow(
                        player = player,
                        showScore = false,
                        statusText = statusText,
                        statusHighlight = hasSubmitted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ChampagnePillButton(
                text = "Reveal Answer",
                onClick = onRevealAnswer,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ListeningVinylDisc(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinylSpin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinylRotation"
    )

    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Outer vinyl body
        drawCircle(
            color = MajstroColors.SlateElevated,
            radius = radius,
            center = center
        )
        drawCircle(
            color = MajstroColors.Hairline,
            radius = radius - 1.dp.toPx(),
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Subtle rotating grooves + champagne glint arc
        rotate(degrees = angle, pivot = center) {
            listOf(0.84f, 0.72f, 0.60f, 0.48f).forEach { factor ->
                drawCircle(
                    color = MajstroColors.WarmIvory.copy(alpha = 0.08f),
                    radius = radius * factor,
                    center = center,
                    style = Stroke(width = 0.9.dp.toPx())
                )
            }
            drawArc(
                color = MajstroColors.Champagne.copy(alpha = 0.45f),
                startAngle = -35f,
                sweepAngle = 55f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.78f, center.y - radius * 0.78f),
                size = androidx.compose.ui.geometry.Size(radius * 1.56f, radius * 1.56f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Center champagne label + spindle
        drawCircle(
            color = MajstroColors.Champagne.copy(alpha = 0.22f),
            radius = radius * 0.30f,
            center = center
        )
        drawCircle(
            color = MajstroColors.Champagne,
            radius = radius * 0.30f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = MajstroColors.MidnightBg,
            radius = 5.dp.toPx(),
            center = center
        )
    }
}

@Composable
private fun MultipleChoicePill(
    label: String,
    optionText: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MajstroColors.Champagne.copy(alpha = 0.15f) else MajstroColors.SlateElevated,
        label = "mcBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MajstroColors.Champagne else Color.Transparent,
        label = "mcBorder"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = bgColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontFamily = InterFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) MajstroColors.ChampagneBright else MajstroColors.Champagne
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = optionText,
                fontFamily = InterFamily,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = MajstroColors.WarmIvory
            )
        }
    }
}
