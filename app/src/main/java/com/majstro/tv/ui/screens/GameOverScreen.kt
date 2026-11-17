package com.majstro.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majstro.tv.domain.model.GameState
import com.majstro.tv.ui.components.ChampagnePillButton
import com.majstro.tv.ui.components.EditorialSectionLabel
import com.majstro.tv.ui.components.OutlinedChampagneButton
import com.majstro.tv.ui.theme.CormorantGaramondFamily
import com.majstro.tv.ui.theme.InterFamily
import com.majstro.tv.ui.theme.MajstroColors

/** Concert Finale standings screen with spotlighted winner and hairline-divided podium. */
@Composable
fun GameOverScreen(
    state: GameState,
    onPlayAgain: () -> Unit,
    onBackToLobby: () -> Unit
) {
    val winner = state.players.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        EditorialSectionLabel("CONCERT FINALE  ·  FINAL STANDINGS")
        Spacer(modifier = Modifier.height(14.dp))

        if (winner != null) {
            Text(
                text = winner.name,
                fontFamily = CormorantGaramondFamily,
                fontSize = 44.sp,
                fontWeight = FontWeight.Normal,
                color = MajstroColors.WarmIvory
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "First Chair  ·  ${winner.score} pts",
                fontFamily = InterFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MajstroColors.Champagne,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(26.dp))
        }

        Column(modifier = Modifier.width(520.dp)) {
            HorizontalDivider(color = MajstroColors.Hairline, thickness = 1.dp)
            state.players.forEachIndexed { index, player ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontFamily = CormorantGaramondFamily,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (index == 0) MajstroColors.Champagne else MajstroColors.CoolMuted
                        )
                        Text(
                            text = player.name,
                            fontFamily = InterFamily,
                            fontSize = 17.sp,
                            fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = MajstroColors.WarmIvory
                        )
                    }
                    Text(
                        text = "${player.score} pts",
                        fontFamily = InterFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (index == 0) MajstroColors.ChampagneBright else MajstroColors.CoolMuted
                    )
                }
                HorizontalDivider(color = MajstroColors.Hairline, thickness = 1.dp)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ChampagnePillButton(
                text = "Play Again",
                onClick = onPlayAgain
            )
            OutlinedChampagneButton(
                text = "Back to Lobby",
                onClick = onBackToLobby
            )
        }
    }
}
