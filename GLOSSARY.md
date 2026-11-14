# Majstro Domain Glossary

Canonical terms for the Majstro codebase. Keep module names, interfaces, and documentation aligned with this vocabulary.

- **ClassicalWork**: A classical piece in the repertoire with composer, title, era, year written, difficulty level, stylistic clue, and Spotify search query.
- **Repertoire**: The catalog of `ClassicalWork` entries available for a quiz match.
- **QuizEngine**: Pure Kotlin state machine driving the match lifecycle (`LOBBY` -> `QUESTION` -> `REVEAL` -> `GAME_OVER`).
- **MusicPlayerGateway**: Domain seam for audio playback (`status`, `play`, `pause`).
- **SpotifySession**: Deep module owning Spotify OAuth 2.0 PKCE setup, token persistence/refresh, Spotify Connect playback, and Demo Mode fallback; implements `MusicPlayerGateway` and `SpotifyAuthSession`.
- **Demo Mode**: Zero-config playback fallback used when Spotify is not linked, simulating playback status without network audio.
