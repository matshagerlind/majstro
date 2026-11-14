package com.majstro.tv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.majstro.tv.data.DefaultQuestionGenerator
import com.majstro.tv.data.InMemoryRepertoireSource
import com.majstro.tv.domain.engine.QuizEngine
import com.majstro.tv.server.KtorControllerServer
import com.majstro.tv.spotify.SharedPrefsKeyValueStore
import com.majstro.tv.spotify.SpotifyAuthStore
import com.majstro.tv.spotify.SpotifyPlayerGateway
import com.majstro.tv.ui.MajstroTvApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Entry point wiring Majstro's clean domain engine to its local Ktor server,
 * repertoire source, and Spotify Connect / Demo playback gateway.
 */
class MainActivity : ComponentActivity() {

    private lateinit var ktorServer: KtorControllerServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authStore = SpotifyAuthStore(SharedPrefsKeyValueStore(applicationContext))
        val spotifyGateway = SpotifyPlayerGateway(
            authStore = authStore,
            wakeSpotifyApp = ::tryLaunchSpotifyApp
        )
        val quizEngine = QuizEngine(
            repertoireSource = InMemoryRepertoireSource(),
            questionGenerator = DefaultQuestionGenerator(),
            playerGateway = spotifyGateway,
            scope = lifecycleScope
        )

        ktorServer = KtorControllerServer(
            hostIpProvider = ::resolveLanIpAddress,
            quizEngine = quizEngine,
            authStore = authStore,
            spotifyGateway = spotifyGateway,
            scope = lifecycleScope
        )
        ktorServer.start()

        setContent {
            MajstroTvApp(
                engine = quizEngine,
                onStartGame = { lifecycleScope.launch(Dispatchers.IO) { quizEngine.startGame() } },
                onRevealAnswer = { lifecycleScope.launch(Dispatchers.IO) { quizEngine.revealAnswer() } },
                onNextRound = { lifecycleScope.launch(Dispatchers.IO) { quizEngine.nextRound() } },
                onBackToLobby = { lifecycleScope.launch(Dispatchers.IO) { quizEngine.backToLobby() } },
                onOpenSpotifySetup = ::openLocalSpotifySetup
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::ktorServer.isInitialized) {
            ktorServer.stop()
        }
    }

    private fun openLocalSpotifySetup() {
        runCatching {
            val url = "http://127.0.0.1:${KtorControllerServer.DEFAULT_PORT}/spotify"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    private fun tryLaunchSpotifyApp(): Boolean {
        val intent = packageManager.getLaunchIntentForPackage("com.spotify.music") ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        return true
    }

    private fun resolveLanIpAddress(): String =
        runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList().asSequence() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
                ?.hostAddress
        }.getOrNull() ?: "127.0.0.1"
}
