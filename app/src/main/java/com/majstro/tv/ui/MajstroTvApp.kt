package com.majstro.tv.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.majstro.tv.domain.engine.QuizEngine
import com.majstro.tv.domain.model.QuizPhase
import com.majstro.tv.ui.components.ExitToLobbyConfirmDialog
import com.majstro.tv.ui.components.SpotifySetupDialog
import com.majstro.tv.ui.components.TopHeader
import com.majstro.tv.ui.screens.GameOverScreen
import com.majstro.tv.ui.screens.LobbyScreen
import com.majstro.tv.ui.screens.QuestionScreen
import com.majstro.tv.ui.screens.RevealScreen
import com.majstro.tv.ui.theme.MajstroColors
import com.majstro.tv.ui.theme.MajstroTheme

/** Landscape Tablet UI orchestrator for Majstro. */
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
    var showSpotifyModal by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    MajstroTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MajstroColors.MidnightBg)
                .displayCutoutPadding()
                .padding(horizontal = 36.dp, vertical = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopHeader(
                    state = state,
                    onOpenSpotifyModal = { showSpotifyModal = true },
                    onRequestExitToLobby = {
                        if (state.phase == QuizPhase.GAME_OVER) {
                            onBackToLobby()
                        } else {
                            showExitConfirmDialog = true
                        }
                    }
                )
                Spacer(modifier = Modifier.height(22.dp))
                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = state.phase,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(260)) +
                                slideInVertically(animationSpec = tween(260)) { it / 24 })
                                .togetherWith(fadeOut(animationSpec = tween(180)))
                        },
                        label = "phaseTransition"
                    ) { phase ->
                        // Freeze the exiting screen's snapshot so fadeOut doesn't flash the next round's state.
                        val phaseState = remember(phase) { mutableStateOf(state) }
                        if (phase == state.phase) {
                            phaseState.value = state
                        }
                        val effectiveState = phaseState.value
                        when (phase) {
                            QuizPhase.LOBBY -> LobbyScreen(
                                state = effectiveState,
                                onSelectCategory = engine::setCategory,
                                onSelectRoundCount = engine::setRoundCount,
                                onSelectDifficulty = engine::setDifficultyLevel,
                                onSelectAnswerMode = engine::setAnswerMode,
                                onStartGame = onStartGame
                            )
                            QuizPhase.QUESTION -> QuestionScreen(
                                state = effectiveState,
                                onSelectOption = { index ->
                                    engine.submitAnswer(QuizEngine.HOST_PLAYER_ID, index)
                                },
                                onRevealAnswer = onRevealAnswer
                            )
                            QuizPhase.REVEAL -> RevealScreen(
                                state = effectiveState,
                                onToggleAward = engine::togglePlayerAward,
                                onNextRound = onNextRound
                            )
                            QuizPhase.GAME_OVER -> GameOverScreen(
                                state = effectiveState,
                                onPlayAgain = onStartGame,
                                onBackToLobby = onBackToLobby
                            )
                        }
                    }
                }
            }

            if (showSpotifyModal) {
                SpotifySetupDialog(
                    state = state,
                    onOpenSpotifySetup = onOpenSpotifySetup,
                    onDismiss = { showSpotifyModal = false }
                )
            }

            if (showExitConfirmDialog) {
                ExitToLobbyConfirmDialog(
                    onConfirmExit = {
                        showExitConfirmDialog = false
                        onBackToLobby()
                    },
                    onDismiss = { showExitConfirmDialog = false }
                )
            }
        }
    }
}
