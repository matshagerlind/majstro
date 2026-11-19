package com.majstro.tv.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majstro.tv.data.ComposerCatalog
import com.majstro.tv.domain.model.AnswerMode
import com.majstro.tv.domain.model.AwardField
import com.majstro.tv.domain.model.GameState
import com.majstro.tv.domain.model.Player
import com.majstro.tv.domain.model.QuizCategory
import com.majstro.tv.ui.rememberComposerPortrait
import com.majstro.tv.ui.components.ChampagnePillButton
import com.majstro.tv.ui.components.EditorialClueQuote
import com.majstro.tv.ui.components.EditorialSectionLabel
import com.majstro.tv.ui.components.PlayerRosterRow
import com.majstro.tv.ui.components.eraArtworkRes
import com.majstro.tv.ui.theme.CormorantGaramondFamily
import com.majstro.tv.ui.theme.InterFamily
import com.majstro.tv.ui.theme.MajstroColors
import kotlin.math.abs

/** Program-notes reveal paired with hairline-separated judging pills or scoreboard. */
@Composable
fun RevealScreen(
    state: GameState,
    onToggleAward: (String, AwardField) -> Unit,
    onNextRound: () -> Unit
) {
    val question = state.currentQuestion ?: return
    val work = question.work
    val isWriteIn = state.selectedAnswerMode == AnswerMode.WRITE_IN

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(38.dp)
    ) {
        // Left column: Era oil painting & editorial program notes (borderless)
        Column(
            modifier = Modifier
                .weight(1.05f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            val composerPortrait = rememberComposerPortrait(work.composer)
            val composerDisplay = ComposerCatalog.lifespanFor(work.composer)
                ?.let { "${work.composer} ($it)" }
                ?: work.composer

            val portraitShape = RoundedCornerShape(18.dp)
            val portraitModifier = Modifier
                .width(140.dp)
                .height(186.dp)
                .clip(portraitShape)
                .border(1.dp, MajstroColors.Champagne.copy(alpha = 0.28f), portraitShape)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (composerPortrait != null) {
                    Image(
                        bitmap = composerPortrait,
                        contentDescription = "${work.composer} portrait",
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = portraitModifier
                    )
                } else {
                    Image(
                        painter = painterResource(id = eraArtworkRes(work.era)),
                        contentDescription = "${work.era.label} era painting",
                        contentScale = ContentScale.Crop,
                        modifier = portraitModifier
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    EditorialSectionLabel("ANSWER REVEALED  ·  ${work.era.label}")
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = work.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MajstroColors.WarmIvory
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = composerDisplay,
                        fontFamily = CormorantGaramondFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = MajstroColors.Champagne
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MajstroColors.Champagne.copy(alpha = 0.14f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, MajstroColors.Champagne.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "Written ${work.yearWritten}",
                                fontFamily = InterFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MajstroColors.ChampagneBright,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                        val metaText = listOf(work.catalogNumber, work.styleOrForm)
                            .filter { it.isNotBlank() }
                            .joinToString("  ·  ")
                        Text(
                            text = metaText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MajstroColors.CoolMuted
                        )
                    }
                }
            }

            val revealNote = work.funFact.ifBlank { work.stylisticClue }
            if (revealNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(18.dp))
                EditorialSectionLabel("FUN FACT")
                Spacer(modifier = Modifier.height(8.dp))
                EditorialClueQuote(clue = revealNote)
            }

            if (!isWriteIn) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Correct option: ${question.options[question.correctIndex]}",
                    fontFamily = InterFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MajstroColors.SageGreen
                )
            }
        }

        // Right column: Hairline-separated Write-In judging rows or Scoreboard + Next CTA
        Column(
            modifier = Modifier
                .weight(1.18f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                EditorialSectionLabel(
                    if (isWriteIn) "CONFIRM SCORING" else "SCOREBOARD"
                )
                val workTitleLabel = if (work.category == QuizCategory.MOVIE_MUSIC) "Movie" else "Piece"
                if (isWriteIn) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap pills to award Composer (+1), $workTitleLabel (+2), and Closest Year (+1).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MajstroColors.CoolMuted
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                state.players.forEach { player ->
                    if (isWriteIn) {
                        PlayerJudgeRow(
                            player = player,
                            trueYear = work.yearWritten,
                            workTitleLabel = workTitleLabel,
                            onToggleAward = { field -> onToggleAward(player.id, field) }
                        )
                    } else {
                        PlayerRosterRow(player = player, showDelta = true)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ChampagnePillButton(
                text = if (question.roundNumber >= question.totalRounds) "Final Standings" else "Next Song",
                onClick = onNextRound,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PlayerJudgeRow(
    player: Player,
    trueYear: Int,
    workTitleLabel: String,
    onToggleAward: (AwardField) -> Unit
) {
    val written = player.writtenAnswer
    val yearDiffText = written?.yearGuess?.let { guess ->
        val diff = abs(guess - trueYear)
        if (diff == 0) "$guess (exact)" else "$guess (${diff}y off)"
    } ?: "—"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = player.name,
                fontFamily = InterFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MajstroColors.WarmIvory
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (player.lastRoundDelta > 0) {
                    Text(
                        text = "+${player.lastRoundDelta}",
                        fontFamily = InterFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MajstroColors.SageGreen
                    )
                }
                Text(
                    text = "${player.score} pts",
                    fontFamily = InterFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MajstroColors.Champagne
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        if (written != null) {
            Text(
                text = "Composer: ${written.composerGuess.ifBlank { "—" }}   ·   $workTitleLabel: ${written.pieceGuess.ifBlank { "—" }}   ·   Year: $yearDiffText",
                style = MaterialTheme.typography.bodyMedium,
                color = MajstroColors.CoolMuted
            )
        } else {
            Text(
                text = "No written entry submitted (judge oral or paper guess)",
                style = MaterialTheme.typography.bodyMedium,
                color = MajstroColors.CoolMuted
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AwardTogglePill(
                label = "Composer +1",
                awarded = player.roundAward.composerCorrect,
                onClick = { onToggleAward(AwardField.COMPOSER) },
                modifier = Modifier.weight(1f)
            )
            AwardTogglePill(
                label = "$workTitleLabel +2",
                awarded = player.roundAward.pieceCorrect,
                onClick = { onToggleAward(AwardField.PIECE) },
                modifier = Modifier.weight(1f)
            )
            AwardTogglePill(
                label = "Closest Yr +1",
                awarded = player.roundAward.closestYear,
                onClick = { onToggleAward(AwardField.CLOSEST_YEAR) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MajstroColors.Hairline, thickness = 1.dp)
    }
}

@Composable
private fun AwardTogglePill(
    label: String,
    awarded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (awarded) MajstroColors.SageGreen.copy(alpha = 0.22f) else MajstroColors.SlateElevated,
        label = "awardBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (awarded) MajstroColors.SageGreen else MajstroColors.Hairline,
        label = "awardBorder"
    )

    Surface(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        color = bgColor,
        shape = CircleShape,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = if (awarded) "✓ $label" else label,
            fontFamily = InterFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (awarded) MajstroColors.WarmIvory else MajstroColors.CoolMuted,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
