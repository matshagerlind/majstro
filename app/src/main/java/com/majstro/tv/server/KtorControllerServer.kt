package com.majstro.tv.server

import com.majstro.tv.domain.engine.QuizEngine
import com.majstro.tv.domain.model.AnswerMode
import com.majstro.tv.domain.model.AwardField
import com.majstro.tv.domain.model.GameState
import com.majstro.tv.domain.model.PlaybackStatus
import com.majstro.tv.domain.model.Player
import com.majstro.tv.domain.model.QuizCategory
import com.majstro.tv.domain.model.QuizPhase
import com.majstro.tv.spotify.SpotifyAuthSession
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
 * Embedded Ktor HTTP + WebSocket server running on the Tablet host.
 * - `GET /`: Jackbox-style mobile web controller for phones on the same Wi-Fi.
 * - `GET /spotify`: Phone-friendly Spotify Client ID + OAuth PKCE setup flow.
 * - `WS /ws`: Real-time game state broadcast & player answer/judging channel.
 */
class KtorControllerServer(
    private val port: Int = DEFAULT_PORT,
    private val hostIpProvider: () -> String,
    private val quizEngine: QuizEngine,
    private val spotifyAuth: SpotifyAuthSession,
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
                            clientId = spotifyAuth.clientId.orEmpty(),
                            redirectUri = currentRedirect,
                            isConnected = spotifyAuth.isConnected,
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
                    val redirectUri = "http://${resolveHostAuthority(call)}/spotify/callback"
                    val authUrl = spotifyAuth.beginAuth(clientId, redirectUri)
                    call.respondRedirect(authUrl)
                }

                post("/spotify/disconnect") {
                    spotifyAuth.disconnect()
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
                    val result = spotifyAuth.completeAuth(code, redirectUri)
                    if (result.isSuccess) {
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
            "setConfig" -> {
                msg.category?.let { quizEngine.setCategory(it) }
                msg.roundCount?.let { quizEngine.setRoundCount(it) }
                msg.difficultyLevel?.let { quizEngine.setDifficultyLevel(it) }
                msg.answerMode?.let { quizEngine.setAnswerMode(it) }
            }
            "answer" -> {
                val playerId = msg.playerId?.takeIf { it.isNotBlank() } ?: return
                val optionIndex = msg.optionIndex ?: return
                quizEngine.submitAnswer(playerId, optionIndex)
            }
            "writeAnswer" -> {
                val playerId = msg.playerId?.takeIf { it.isNotBlank() } ?: return
                quizEngine.submitWrittenAnswer(
                    playerId = playerId,
                    composer = msg.composerGuess.orEmpty(),
                    piece = msg.pieceGuess.orEmpty(),
                    year = msg.yearGuess
                )
            }
            "toggleAward" -> {
                val targetPlayerId = msg.targetPlayerId?.takeIf { it.isNotBlank() } ?: return
                val field = msg.awardField ?: return
                quizEngine.togglePlayerAward(targetPlayerId, field)
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
            selectedCategory = state.selectedCategory,
            selectedRoundCount = state.selectedRoundCount,
            selectedDifficulty = state.selectedDifficulty,
            selectedAnswerMode = state.selectedAnswerMode,
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
            revealedYearWritten = if (isReveal) question?.work?.yearWritten else null,
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
        val optionIndex: Int? = null,
        val roundCount: Int? = null,
        val difficultyLevel: Int? = null,
        val answerMode: AnswerMode? = null,
        val category: QuizCategory? = null,
        val composerGuess: String? = null,
        val pieceGuess: String? = null,
        val yearGuess: Int? = null,
        val targetPlayerId: String? = null,
        val awardField: AwardField? = null
    )

    @Serializable
    private data class PhoneGameState(
        val phase: QuizPhase,
        val selectedCategory: QuizCategory,
        val selectedRoundCount: Int,
        val selectedDifficulty: Int,
        val selectedAnswerMode: AnswerMode,
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
        val revealedYearWritten: Int? = null,
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
                body { font-family: system-ui, -apple-system, sans-serif; background: #0e151c; color: #f2efea; margin: 0; padding: 20px; }
                .card { max-width: 480px; margin: 0 auto; background: #141e28; border: 1px solid #253342; border-radius: 18px; padding: 22px; }
                h1 { font-family: Georgia, "Times New Roman", serif; font-weight: normal; color: #f2efea; margin-top: 0; font-size: 1.45rem; }
                label { display: block; margin-top: 14px; font-size: 0.9rem; color: #8e9ba8; }
                input { width: 100%; box-sizing: border-box; padding: 12px; margin-top: 6px; border-radius: 10px; border: 1px solid #253342; background: #0e151c; color: #f2efea; font-size: 1rem; }
                button { width: 100%; margin-top: 16px; padding: 13px; border: none; border-radius: 12px; font-size: 1rem; font-weight: 600; cursor: pointer; background: #25b864; color: #08150d; }
                .btn-secondary { background: #1a2532; color: #f2efea; border: 1px solid #253342; }
                .code { background: #0e151c; padding: 10px; border-radius: 10px; font-family: monospace; font-size: 0.82rem; word-break: break-all; border: 1px solid #253342; }
                .badge { display: inline-block; padding: 4px 10px; border-radius: 999px; font-size: 0.8rem; font-weight: 600; background: ${if (isConnected) "#25b86426" else "#dfb49b26"}; color: ${if (isConnected) "#63d492" else "#ecc8b2"}; }
                .msg { background: #1a2532; border-left: 4px solid #dfb49b; padding: 10px 12px; border-radius: 6px; margin-top: 12px; font-size: 0.9rem; }
                a { color: #dfb49b; }
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
                :root { --bg: #0e151c; --card: #141e28; --elevated: #1a2532; --gold: #dfb49b; --green: #4e9f76; --text: #f2efea; --muted: #8e9ba8; --border: #253342; }
                body { font-family: system-ui, -apple-system, sans-serif; background: var(--bg); color: var(--text); margin: 0; padding: 16px; }
                .wrap { max-width: 520px; margin: 0 auto; }
                .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
                .brand { font-family: Georgia, "Times New Roman", serif; font-size: 1.4rem; font-weight: normal; color: var(--text); letter-spacing: 0.08em; }
                .pill { font-size: 0.78rem; padding: 4px 10px; border-radius: 999px; background: var(--elevated); border: 1px solid var(--border); color: var(--muted); }
                .card { background: var(--card); border: 1px solid var(--border); border-radius: 18px; padding: 16px; margin-bottom: 12px; }
                label { display: block; font-size: 0.75rem; color: var(--gold); margin: 8px 0 4px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.08em; }
                input { width: 100%; box-sizing: border-box; padding: 11px; border-radius: 10px; border: 1px solid var(--border); background: var(--bg); color: var(--text); font-size: 1rem; margin-bottom: 8px; }
                button { width: 100%; padding: 13px; border-radius: 12px; border: 1px solid var(--border); background: var(--elevated); color: var(--text); font-size: 0.98rem; font-weight: 600; cursor: pointer; margin-bottom: 8px; text-align: left; }
                button.primary { background: var(--gold); color: #10171e; border-color: var(--gold); text-align: center; }
                button.selected { border-color: var(--gold); background: #dfb49b26; color: #ecc8b2; }
                button.awarded { border-color: var(--green); background: #4e9f7633; color: #8ad6ad; text-align: center; }
                .chip-row { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 8px; }
                .chip { width: auto; flex: 1; min-width: 44px; padding: 9px 10px; margin: 0; text-align: center; font-size: 0.88rem; border-radius: 9px; }
                .clue { font-size: 0.9rem; color: var(--text); border-left: 3px solid var(--gold); padding-left: 10px; margin: 10px 0 12px; opacity: 0.9; }
                .row { display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid var(--border); }
                .row:last-child { border-bottom: none; }
                .guess-box { background: var(--elevated); border: 1px solid var(--border); border-radius: 12px; padding: 10px; margin-bottom: 10px; }
                .judge-row { display: flex; gap: 6px; margin-top: 8px; }
                .judge-btn { flex: 1; padding: 9px 6px; margin: 0; text-align: center; font-size: 0.8rem; border-radius: 8px; }
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
                let renderedWriteRound = null;
                let draftComposer = "";
                let draftPiece = "";
                let draftYear = "";

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

                function submitWritten() {
                  const yTrim = (draftYear || "").trim();
                  const parsedYear = yTrim && /^\d+$/.test(yTrim) ? parseInt(yTrim, 10) : null;
                  send({
                    type: "writeAnswer",
                    playerId: playerId,
                    composerGuess: draftComposer,
                    pieceGuess: draftPiece,
                    yearGuess: parsedYear
                  });
                }

                function render() {
                  const root = document.getElementById("app");
                  if (!state) { root.innerHTML = "<div class='card'>Waiting for Tablet…</div>"; return; }
                  if (!joined) {
                    renderedWriteRound = null;
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
                    renderedWriteRound = null;
                    const counts = [3, 5, 8, 10, 15].map(function(n) {
                      const cls = state.selectedRoundCount === n ? 'chip selected' : 'chip';
                      return '<button class="' + cls + '" onclick="send({type:\'setConfig\', roundCount:' + n + '})">' + n + '</button>';
                    }).join("");
                    const diffs = [1, 2, 3, 4, 5].map(function(d) {
                      const cls = state.selectedDifficulty === d ? 'chip selected' : 'chip';
                      return '<button class="' + cls + '" onclick="send({type:\'setConfig\', difficultyLevel:' + d + '})">' + d + '</button>';
                    }).join("");
                    const wCls = state.selectedAnswerMode === "WRITE_IN" ? 'chip selected' : 'chip';
                    const mCls = state.selectedAnswerMode === "MULTIPLE_CHOICE" ? 'chip selected' : 'chip';
                    const rows = state.players.map(function(p) {
                      return '<div class="row"><span>' + escapeHtml(p.name) + '</span><span>' + p.score + ' pts</span></div>';
                    }).join("");
                    root.innerHTML =
                      '<div class="card">' +
                        '<h3 style="margin-top:0">Lobby · ' + escapeHtml(state.playbackStatus.mode) + '</h3>' +
                        '<label>Songs</label><div class="chip-row">' + counts + '</div>' +
                        '<label>Difficulty (1–5)</label><div class="chip-row">' + diffs + '</div>' +
                        '<label>Mode</label><div class="chip-row">' +
                          '<button class="' + wCls + '" onclick="send({type:\'setConfig\', answerMode:\'WRITE_IN\'})">Everyone Writes</button>' +
                          '<button class="' + mCls + '" onclick="send({type:\'setConfig\', answerMode:\'MULTIPLE_CHOICE\'})">Multiple Choice</button>' +
                        '</div>' +
                        '<label>Players (' + state.players.length + ')</label>' + rows +
                      '</div>' +
                      '<button class="primary" onclick="send({type:\'start\'})">Start ' + state.selectedRoundCount + '-Song Quiz</button>';
                  } else if (state.phase === "QUESTION") {
                    if (state.selectedAnswerMode === "WRITE_IN") {
                      const submitted = me && me.writtenAnswer;
                      const statusLine = submitted
                        ? 'Submitted: ' + escapeHtml((submitted.composerGuess || "—") + ' · ' + (submitted.pieceGuess || "—") + ' · ' + (submitted.yearGuess || "—"))
                        : 'Not submitted yet';
                      if (renderedWriteRound === state.roundNumber && document.getElementById("writeStatus")) {
                        document.getElementById("writeStatus").innerHTML = statusLine;
                        return;
                      }
                      renderedWriteRound = state.roundNumber;
                      draftComposer = submitted ? (submitted.composerGuess || "") : "";
                      draftPiece = submitted ? (submitted.pieceGuess || "") : "";
                      draftYear = (submitted && submitted.yearGuess != null) ? String(submitted.yearGuess) : "";
                      const isMovie = state.selectedCategory === "MOVIE_MUSIC";
                      const titleLabel = isMovie ? "Movie" : "Piece";
                      const compPlaceholder = isMovie ? "e.g. John Williams" : "e.g. Mozart";
                      const titlePlaceholder = isMovie ? "e.g. Jurassic Park" : "e.g. Symphony No. 40";
                      const yearPlaceholder = isMovie ? "e.g. 1993" : "e.g. 1788";
                      const yearLabel = isMovie ? "Release Year (+1 pt closest)" : "Year Written (+1 pt closest)";
                      root.innerHTML =
                        '<div class="card">' +
                          '<div style="color:var(--gold);font-size:0.85rem;font-weight:600;">SONG ' + state.roundNumber + ' / ' + state.totalRounds + ' · WRITE-IN</div>' +
                          '<div class="clue">' + escapeHtml(state.stylisticClue) + '</div>' +
                          '<label>Composer (+1 pt)</label>' +
                          '<input id="inComposer" placeholder="' + compPlaceholder + '" value="' + escapeHtml(draftComposer) + '" oninput="draftComposer=this.value" />' +
                          '<label>' + titleLabel + ' (+2 pts)</label>' +
                          '<input id="inPiece" placeholder="' + titlePlaceholder + '" value="' + escapeHtml(draftPiece) + '" oninput="draftPiece=this.value" />' +
                          '<label>' + yearLabel + '</label>' +
                          '<input id="inYear" type="number" inputmode="numeric" placeholder="' + yearPlaceholder + '" value="' + escapeHtml(draftYear) + '" oninput="draftYear=this.value" />' +
                          '<button class="primary" onclick="submitWritten()">Submit Guess</button>' +
                          '<div id="writeStatus" style="font-size:0.85rem;color:var(--muted);margin-top:6px;">' + statusLine + '</div>' +
                        '</div>' +
                        '<button onclick="send({type:\'reveal\'})" style="text-align:center;">Reveal Answer on Tablet</button>';
                    } else {
                      renderedWriteRound = null;
                      const labels = ["A", "B", "C", "D"];
                      const opts = state.options.map(function(opt, i) {
                        const cls = (me && me.selectedOptionIndex === i) ? 'selected' : '';
                        return '<button class="' + cls + '" onclick="send({type:\'answer\', playerId: playerId, optionIndex:' + i + '})">' +
                          '<strong>' + labels[i] + '.</strong> ' + escapeHtml(opt) + '</button>';
                      }).join("");
                      root.innerHTML =
                        '<div class="card">' +
                          '<div style="color:var(--gold);font-size:0.85rem;font-weight:600;">SONG ' + state.roundNumber + ' / ' + state.totalRounds + '</div>' +
                          '<h3 style="margin:6px 0 8px;">' + escapeHtml(state.prompt) + '</h3>' +
                          '<div class="clue">' + escapeHtml(state.stylisticClue) + '</div>' +
                          opts +
                        '</div>' +
                        '<button onclick="send({type:\'reveal\'})" style="text-align:center;">Reveal Answer on Tablet</button>';
                    }
                  } else if (state.phase === "REVEAL") {
                    renderedWriteRound = null;
                    const cat = state.revealedCatalog ? ' (' + escapeHtml(state.revealedCatalog) + ')' : '';
                    const yearTxt = state.revealedYearWritten ? ' · Written ' + state.revealedYearWritten : '';
                    const isWriteIn = state.selectedAnswerMode === "WRITE_IN";
                    const playersHtml = state.players.map(function(p) {
                      const delta = p.lastRoundDelta > 0 ? ' (+' + p.lastRoundDelta + ')' : '';
                      if (!isWriteIn) {
                        return '<div class="row"><span>' + escapeHtml(p.name) + '</span><span>' + p.score + ' pts' + delta + '</span></div>';
                      }
                      const wa = p.writtenAnswer;
                      const aw = p.roundAward || {};
                      const guessLine = wa
                        ? escapeHtml((wa.composerGuess || "—") + ' · ' + (wa.pieceGuess || "—") + ' · ' + (wa.yearGuess != null ? wa.yearGuess : "—"))
                        : 'No written entry (oral/paper)';
                      const cCls = aw.composerCorrect ? 'judge-btn awarded' : 'judge-btn';
                      const pCls = aw.pieceCorrect ? 'judge-btn awarded' : 'judge-btn';
                      const yCls = aw.closestYear ? 'judge-btn awarded' : 'judge-btn';
                      return '<div class="guess-box">' +
                        '<div class="row" style="border:none;padding:0 0 4px;"><strong>' + escapeHtml(p.name) + '</strong><span>' + p.score + ' pts' + delta + '</span></div>' +
                        '<div style="font-size:0.85rem;color:var(--muted);">' + guessLine + '</div>' +
                        '<div class="judge-row">' +
                          '<button class="' + cCls + '" onclick="send({type:\'toggleAward\', targetPlayerId:\'' + escapeHtml(p.id) + '\', awardField:\'COMPOSER\'})">Composer +1</button>' +
                          '<button class="' + pCls + '" onclick="send({type:\'toggleAward\', targetPlayerId:\'' + escapeHtml(p.id) + '\', awardField:\'PIECE\'})">' + (state.selectedCategory === "MOVIE_MUSIC" ? "Movie +2" : "Piece +2") + '</button>' +
                          '<button class="' + yCls + '" onclick="send({type:\'toggleAward\', targetPlayerId:\'' + escapeHtml(p.id) + '\', awardField:\'CLOSEST_YEAR\'})">Year +1</button>' +
                        '</div>' +
                      '</div>';
                    }).join("");
                    root.innerHTML =
                      '<div class="card">' +
                        '<div style="color:var(--gold);font-size:0.85rem;font-weight:600;">ANSWER REVEALED</div>' +
                        '<h3 style="margin:6px 0 4px;">' + escapeHtml(state.revealedTitle || "") + '</h3>' +
                        '<div style="color:var(--gold);font-weight:600;margin-bottom:4px;">' + escapeHtml(state.revealedComposer || "") + yearTxt + '</div>' +
                        '<div style="color:var(--muted);margin-bottom:12px;font-size:0.88rem;">' +
                          escapeHtml(state.revealedEra || "") + ' · ' + escapeHtml(state.revealedForm || "") + cat +
                        '</div>' +
                        playersHtml +
                      '</div>' +
                      '<button class="primary" onclick="send({type:\'next\'})">' + (state.roundNumber >= state.totalRounds ? 'Final Standings' : 'Next Song') + '</button>';
                  } else {
                    renderedWriteRound = null;
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
