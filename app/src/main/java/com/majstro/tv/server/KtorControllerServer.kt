package com.majstro.tv.server

import com.majstro.tv.domain.engine.QuizEngine
import com.majstro.tv.domain.model.GameState
import com.majstro.tv.domain.model.PlaybackStatus
import com.majstro.tv.domain.model.Player
import com.majstro.tv.domain.model.QuizPhase
import com.majstro.tv.spotify.SpotifyAuthStore
import com.majstro.tv.spotify.SpotifyPlayerGateway
import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Embedded Ktor HTTP + WebSocket server running on the TV/Tablet host.
 * - `GET /`: Jackbox-style mobile web controller for phones on the same Wi-Fi.
 * - `GET /spotify`: Phone-friendly Spotify Client ID + OAuth PKCE setup flow.
 * - `WS /ws`: Real-time game state broadcast & player answer channel.
 */
class KtorControllerServer(
    private val port: Int = DEFAULT_PORT,
    private val hostIpProvider: () -> String,
    private val quizEngine: QuizEngine,
    private val authStore: SpotifyAuthStore,
    private val spotifyGateway: SpotifyPlayerGateway,
    private val scope: CoroutineScope
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val sessions = CopyOnWriteArraySet<WebSocketSession>()
    private var server: ApplicationEngine? = null
    private var stateObserverJob: Job? = null

    fun start() {
        if (server != null) return

        val hostIp = hostIpProvider()
        val baseUrl = "http://$hostIp:$port"
        quizEngine.setServerUrls(
            joinUrl = baseUrl,
            spotifySetupUrl = "$baseUrl/spotify"
        )

        server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            install(WebSockets)

            routing {
                get("/") {
                    call.respondText(MOBILE_CONTROLLER_HTML, ContentType.Text.Html)
                }

                get("/spotify") {
                    val currentRedirect = "http://${resolveHostAuthority(call)}/spotify/callback"
                    call.respondText(
                        buildSpotifySetupHtml(
                            clientId = authStore.clientId.orEmpty(),
                            redirectUri = currentRedirect,
                            isConnected = authStore.isConfigured,
                            message = call.request.queryParameters["msg"]
                        ),
                        ContentType.Text.Html
                    )
                }

                post("/spotify/connect") {
                    val params = call.receiveParameters()
                    val clientId = params["client_id"]?.trim().orEmpty()
                    if (clientId.isEmpty()) {
                        call.respondRedirect("/spotify?msg=Please+enter+a+Client+ID")
                        return@post
                    }
                    authStore.clientId = clientId
                    val challenge = authStore.createPkceChallenge()
                    val redirectUri = "http://${resolveHostAuthority(call)}/spotify/callback"
                    val scopes = "user-modify-playback-state user-read-playback-state user-read-currently-playing"
                    val authUrl = buildString {
                        append("https://accounts.spotify.com/authorize")
                        append("?response_type=code")
                        append("&client_id=").append(urlEncode(clientId))
                        append("&scope=").append(urlEncode(scopes))
                        append("&redirect_uri=").append(urlEncode(redirectUri))
                        append("&code_challenge_method=S256")
                        append("&code_challenge=").append(urlEncode(challenge))
                    }
                    call.respondRedirect(authUrl)
                }

                post("/spotify/disconnect") {
                    authStore.clearTokens()
                    spotifyGateway.refreshStatusFromAuth()
                    quizEngine.updatePlaybackStatus(spotifyGateway.status.value)
                    call.respondRedirect("/spotify?msg=Disconnected+Spotify")
                }

                get("/spotify/callback") {
                    val code = call.request.queryParameters["code"]
                    val error = call.request.queryParameters["error"]
                    if (!error.isNullOrBlank() || code.isNullOrBlank()) {
                        call.respondRedirect("/spotify?msg=${urlEncode("Spotify auth error: ${error ?: "missing code"}")}")
                        return@get
                    }
                    val redirectUri = "http://${resolveHostAuthority(call)}/spotify/callback"
                    val result = spotifyGateway.exchangeCodeForToken(code, redirectUri)
                    if (result.isSuccess) {
                        quizEngine.updatePlaybackStatus(spotifyGateway.status.value)
                        call.respondRedirect("/spotify?msg=Spotify+Connected!")
                    } else {
                        val errMsg = result.exceptionOrNull()?.message ?: "Token exchange failed"
                        call.respondRedirect("/spotify?msg=${urlEncode(errMsg)}")
                    }
                }

                webSocket("/ws") {
                    sessions.add(this)
                    try {
                        send(Frame.Text(serializeStateForPhone(quizEngine.state.value)))
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                handleClientMessage(frame.readText())
                            }
                        }
                    } finally {
                        sessions.remove(this)
                    }
                }
            }
        }.start(wait = false)

        stateObserverJob = quizEngine.state
            .onEach { state -> broadcastState(state) }
            .launchIn(scope)
    }

    fun stop() {
        stateObserverJob?.cancel()
        stateObserverJob = null
        server?.stop(500, 1000)
        server = null
        sessions.clear()
    }

    private fun handleClientMessage(raw: String) {
        val msg = runCatching { json.decodeFromString<ClientMessage>(raw) }.getOrNull() ?: return
        when (msg.type) {
            "join" -> {
                val playerId = msg.playerId?.takeIf { it.isNotBlank() } ?: return
                quizEngine.registerPlayer(playerId, msg.name.orEmpty())
            }
            "answer" -> {
                val playerId = msg.playerId?.takeIf { it.isNotBlank() } ?: return
                val optionIndex = msg.optionIndex ?: return
                quizEngine.submitAnswer(playerId, optionIndex)
            }
            "start" -> scope.launch(Dispatchers.IO) { quizEngine.startGame() }
            "reveal" -> scope.launch(Dispatchers.IO) { quizEngine.revealAnswer() }
            "next" -> scope.launch(Dispatchers.IO) { quizEngine.nextRound() }
            "lobby" -> scope.launch(Dispatchers.IO) { quizEngine.backToLobby() }
        }
    }

    private suspend fun broadcastState(state: GameState) {
        val payload = serializeStateForPhone(state)
        for (session in sessions) {
            runCatching { session.send(Frame.Text(payload)) }
                .onFailure { sessions.remove(session) }
        }
    }

    private fun serializeStateForPhone(state: GameState): String {
        val question = state.currentQuestion
        val isReveal = state.phase == QuizPhase.REVEAL
        val view = PhoneGameState(
            phase = state.phase,
            players = state.players,
            playbackStatus = state.playbackStatus,
            roundNumber = question?.roundNumber ?: 0,
            totalRounds = question?.totalRounds ?: 0,
            prompt = question?.prompt.orEmpty(),
            stylisticClue = question?.work?.stylisticClue.orEmpty(),
            options = question?.options.orEmpty(),
            correctIndex = if (isReveal) question?.correctIndex else null,
            revealedTitle = if (isReveal) question?.work?.title else null,
            revealedComposer = if (isReveal) question?.work?.composer else null,
            revealedEra = if (isReveal) question?.work?.era?.label else null,
            revealedForm = if (isReveal) question?.work?.styleOrForm else null,
            revealedCatalog = if (isReveal) question?.work?.catalogNumber else null
        )
        return json.encodeToString(view)
    }

    private fun resolveHostAuthority(call: ApplicationCall): String =
        call.request.headers["Host"]?.takeIf { it.isNotBlank() } ?: "${hostIpProvider()}:$port"

    private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")

    @Serializable
    private data class ClientMessage(
        val type: String,
        val playerId: String? = null,
        val name: String? = null,
        val optionIndex: Int? = null
    )

    @Serializable
    private data class PhoneGameState(
        val phase: QuizPhase,
        val players: List<Player>,
        val playbackStatus: PlaybackStatus,
        val roundNumber: Int,
        val totalRounds: Int,
        val prompt: String,
        val stylisticClue: String,
        val options: List<String>,
        val correctIndex: Int? = null,
        val revealedTitle: String? = null,
        val revealedComposer: String? = null,
        val revealedEra: String? = null,
        val revealedForm: String? = null,
        val revealedCatalog: String? = null
    )

    companion object {
        const val DEFAULT_PORT = 8080

        private fun buildSpotifySetupHtml(
            clientId: String,
            redirectUri: String,
            isConnected: Boolean,
            message: String?
        ): String = """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1" />
              <title>Majstro · Spotify Setup</title>
              <style>
                body { font-family: system-ui, -apple-system, sans-serif; background: #0d1117; color: #f0f4f8; margin: 0; padding: 20px; }
                .card { max-width: 480px; margin: 0 auto; background: #161b22; border: 1px solid #30363d; border-radius: 14px; padding: 20px; }
                h1 { color: #d4af37; margin-top: 0; font-size: 1.4rem; }
                label { display: block; margin-top: 14px; font-size: 0.9rem; color: #9da7b3; }
                input { width: 100%; box-sizing: border-box; padding: 12px; margin-top: 6px; border-radius: 8px; border: 1px solid #30363d; background: #0d1117; color: #fff; font-size: 1rem; }
                button { width: 100%; margin-top: 16px; padding: 13px; border: none; border-radius: 10px; font-size: 1rem; font-weight: 600; cursor: pointer; background: #1db954; color: #041208; }
                .btn-secondary { background: #30363d; color: #f0f4f8; }
                .code { background: #0d1117; padding: 10px; border-radius: 8px; font-family: monospace; font-size: 0.82rem; word-break: break-all; border: 1px solid #30363d; }
                .badge { display: inline-block; padding: 4px 10px; border-radius: 999px; font-size: 0.8rem; font-weight: 600; background: ${if (isConnected) "#1db95433" else "#d4af3733"}; color: ${if (isConnected) "#4ae381" else "#f1d37a"}; }
                .msg { background: #1f2937; border-left: 4px solid #d4af37; padding: 10px 12px; border-radius: 6px; margin-top: 12px; font-size: 0.9rem; }
                a { color: #d4af37; }
              </style>
            </head>
            <body>
              <div class="card">
                <h1>Majstro · Spotify Connect</h1>
                <span class="badge">${if (isConnected) "Connected" else "Demo Mode Active"}</span>
                ${if (!message.isNullOrBlank()) "<div class='msg'>${escapeHtml(message)}</div>" else ""}
                <p style="font-size:0.9rem;color:#9da7b3;line-height:1.45;">
                  Add this Redirect URI in your <a href="https://developer.spotify.com/dashboard" target="_blank">Spotify Developer Dashboard</a>:
                </p>
                <div class="code">${escapeHtml(redirectUri)}</div>
                <form method="post" action="/spotify/connect">
                  <label for="client_id">Spotify Client ID</label>
                  <input id="client_id" name="client_id" value="${escapeHtml(clientId)}" placeholder="Paste 32-char Client ID" required />
                  <button type="submit">${if (isConnected) "Reconnect Spotify" else "Authorize with Spotify"}</button>
                </form>
                ${if (isConnected) "<form method='post' action='/spotify/disconnect'><button class='btn-secondary' type='submit'>Switch Back to Demo Mode</button></form>" else ""}
                <p style="margin-top:18px;text-align:center;"><a href="/">← Back to Quiz Controller</a></p>
              </div>
            </body>
            </html>
        """.trimIndent()

        private fun escapeHtml(text: String): String =
            text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")

        private val MOBILE_CONTROLLER_HTML = """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1" />
              <title>Majstro · Controller</title>
              <style>
                :root { --bg: #0d1117; --card: #161b22; --gold: #d4af37; --text: #f0f4f8; --muted: #9da7b3; --border: #30363d; }
                body { font-family: system-ui, -apple-system, sans-serif; background: var(--bg); color: var(--text); margin: 0; padding: 16px; }
                .wrap { max-width: 520px; margin: 0 auto; }
                .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
                .brand { font-size: 1.35rem; font-weight: 700; color: var(--gold); letter-spacing: 0.04em; }
                .pill { font-size: 0.78rem; padding: 4px 10px; border-radius: 999px; background: #21262d; color: var(--muted); }
                .card { background: var(--card); border: 1px solid var(--border); border-radius: 14px; padding: 18px; margin-bottom: 14px; }
                input { width: 100%; box-sizing: border-box; padding: 12px; border-radius: 10px; border: 1px solid var(--border); background: var(--bg); color: #fff; font-size: 1rem; margin-bottom: 12px; }
                button { width: 100%; padding: 14px; border-radius: 12px; border: 1px solid var(--border); background: #21262d; color: var(--text); font-size: 1rem; font-weight: 600; cursor: pointer; margin-bottom: 10px; text-align: left; }
                button.primary { background: var(--gold); color: #111; border-color: var(--gold); text-align: center; }
                button.selected { border-color: var(--gold); background: #d4af3726; color: #ffe58f; }
                button.correct { border-color: #2ea043; background: #2ea04333; color: #7ee787; }
                .clue { font-size: 0.9rem; color: #cbd5e1; border-left: 3px solid var(--gold); padding-left: 10px; margin: 10px 0 14px; }
                .row { display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid #21262d; }
                .row:last-child { border-bottom: none; }
                a { color: var(--gold); font-size: 0.85rem; }
              </style>
            </head>
            <body>
              <div class="wrap">
                <div class="header">
                  <div class="brand">MAJSTRO</div>
                  <div id="conn" class="pill">Connecting…</div>
                </div>
                <div id="app"></div>
                <div style="text-align:center;margin-top:10px;"><a href="/spotify">Spotify Settings</a></div>
              </div>
              <script>
                const playerIdKey = "majstro_player_id";
                const playerNameKey = "majstro_player_name";
                let playerId = localStorage.getItem(playerIdKey);
                if (!playerId) {
                  playerId = "p-" + Math.random().toString(36).slice(2, 10);
                  localStorage.setItem(playerIdKey, playerId);
                }
                let playerName = localStorage.getItem(playerNameKey) || "";
                let joined = false;
                let state = null;
                let ws = null;

                function connect() {
                  const proto = location.protocol === "https:" ? "wss://" : "ws://";
                  ws = new WebSocket(proto + location.host + "/ws");
                  ws.onopen = () => {
                    document.getElementById("conn").textContent = "Connected";
                    if (playerName) send({ type: "join", playerId, name: playerName });
                  };
                  ws.onclose = () => {
                    document.getElementById("conn").textContent = "Reconnecting…";
                    setTimeout(connect, 1500);
                  };
                  ws.onmessage = (ev) => {
                    state = JSON.parse(ev.data);
                    if (state.players && state.players.some(p => p.id === playerId)) joined = true;
                    render();
                  };
                }

                function send(obj) {
                  if (ws && ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(obj));
                }

                function joinGame() {
                  const input = document.getElementById("nameInput");
                  const val = (input ? input.value : "").trim();
                  if (!val) return;
                  playerName = val;
                  localStorage.setItem(playerNameKey, playerName);
                  joined = true;
                  send({ type: "join", playerId, name: playerName });
                  render();
                }

                function render() {
                  const root = document.getElementById("app");
                  if (!state) { root.innerHTML = "<div class='card'>Waiting for TV…</div>"; return; }
                  if (!joined) {
                    root.innerHTML =
                      '<div class="card">' +
                        '<h3 style="margin-top:0">Join the Concert Quiz</h3>' +
                        '<input id="nameInput" placeholder="Your name" value="' + escapeHtml(playerName) + '" maxlength="20" />' +
                        '<button class="primary" onclick="joinGame()">Join Lobby</button>' +
                      '</div>';
                    return;
                  }

                  const me = state.players.find(function(p) { return p.id === playerId; });
                  if (state.phase === "LOBBY") {
                    const rows = state.players.map(function(p) {
                      return '<div class="row"><span>' + escapeHtml(p.name) + '</span><span>' + p.score + ' pts</span></div>';
                    }).join("");
                    root.innerHTML =
                      '<div class="card">' +
                        '<h3 style="margin-top:0">Lobby · ' + escapeHtml(state.playbackStatus.mode) + '</h3>' +
                        '<p style="color:var(--muted);font-size:0.9rem;">' + escapeHtml(state.playbackStatus.message) + '</p>' +
                        rows +
                      '</div>' +
                      '<button class="primary" onclick="send({type:\'start\'})">Start 5-Round Quiz</button>';
                  } else if (state.phase === "QUESTION") {
                    const labels = ["A", "B", "C", "D"];
                    const opts = state.options.map(function(opt, i) {
                      const cls = (me && me.selectedOptionIndex === i) ? 'selected' : '';
                      return '<button class="' + cls + '" onclick="send({type:\'answer\', playerId: playerId, optionIndex:' + i + '})">' +
                        '<strong>' + labels[i] + '.</strong> ' + escapeHtml(opt) + '</button>';
                    }).join("");
                    root.innerHTML =
                      '<div class="card">' +
                        '<div style="color:var(--gold);font-size:0.85rem;font-weight:600;">ROUND ' + state.roundNumber + ' / ' + state.totalRounds + '</div>' +
                        '<h3 style="margin:6px 0 8px;">' + escapeHtml(state.prompt) + '</h3>' +
                        '<div class="clue">' + escapeHtml(state.stylisticClue) + '</div>' +
                        opts +
                      '</div>' +
                      '<button onclick="send({type:\'reveal\'})" style="text-align:center;">Reveal Answer on TV</button>';
                  } else if (state.phase === "REVEAL") {
                    const cat = state.revealedCatalog ? ' (' + escapeHtml(state.revealedCatalog) + ')' : '';
                    const rows = state.players.map(function(p) {
                      const delta = p.lastRoundDelta > 0 ? ' (+' + p.lastRoundDelta + ')' : '';
                      return '<div class="row"><span>' + escapeHtml(p.name) + '</span><span>' + p.score + ' pts' + delta + '</span></div>';
                    }).join("");
                    root.innerHTML =
                      '<div class="card">' +
                        '<div style="color:var(--gold);font-size:0.85rem;font-weight:600;">ANSWER REVEALED</div>' +
                        '<h3 style="margin:6px 0 4px;">' + escapeHtml(state.revealedTitle || "") + '</h3>' +
                        '<div style="color:var(--muted);margin-bottom:10px;">' +
                          escapeHtml(state.revealedComposer || "") + ' · ' + escapeHtml(state.revealedEra || "") + ' · ' + escapeHtml(state.revealedForm || "") + cat +
                        '</div>' +
                        rows +
                      '</div>' +
                      '<button class="primary" onclick="send({type:\'next\'})">Next Round</button>';
                  } else {
                    const rows = state.players.map(function(p, idx) {
                      return '<div class="row"><span>#' + (idx + 1) + ' ' + escapeHtml(p.name) + '</span><span>' + p.score + ' pts</span></div>';
                    }).join("");
                    root.innerHTML =
                      '<div class="card">' +
                        '<h3 style="margin-top:0;color:var(--gold);">Final Standings</h3>' +
                        rows +
                      '</div>' +
                      '<button class="primary" onclick="send({type:\'lobby\'})">Back to Lobby</button>';
                  }
                }

                function escapeHtml(s) {
                  return String(s).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;");
                }

                connect();
              </script>
            </body>
            </html>
        """.trimIndent()
    }
}
