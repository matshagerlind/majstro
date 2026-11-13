package com.majstro.tv.data

import com.majstro.tv.domain.model.ClassicalWork
import com.majstro.tv.domain.model.MusicalEra
import com.majstro.tv.domain.port.RepertoireSource

/**
 * Curated 12-work seed repertoire for the v1 Proof of Concept.
 * Later versions can decorate or replace this with an OpenOpus / MusicBrainz RepertoireSource.
 */
class InMemoryRepertoireSource : RepertoireSource {

    override suspend fun loadWorks(): List<ClassicalWork> = seedWorks

    private val seedWorks = listOf(
        ClassicalWork(
            id = "bach-bwv565",
            title = "Toccata and Fugue in D minor",
            composer = "Johann Sebastian Bach",
            era = MusicalEra.BAROQUE,
            styleOrForm = "Organ Toccata & Fugue",
            catalogNumber = "BWV 565",
            stylisticClue = "Dramatic solo pipe organ rhetoric followed by tight four-voice Baroque counterpoint.",
            spotifyQuery = "Bach Toccata and Fugue in D minor BWV 565",
            startPositionMs = 15_000L
        ),
        ClassicalWork(
            id = "vivaldi-rv269",
            title = "The Four Seasons: Spring (Allegro)",
            composer = "Antonio Vivaldi",
            era = MusicalEra.BAROQUE,
            styleOrForm = "Violin Concerto (Ritornello)",
            catalogNumber = "RV 269",
            stylisticClue = "Bouncy Venetian ritornello strings alternating with bird-call solo violin episodes and harpsichord continuo.",
            spotifyQuery = "Vivaldi Four Seasons Spring Allegro",
            startPositionMs = 20_000L
        ),
        ClassicalWork(
            id = "mozart-k550",
            title = "Symphony No. 40 in G minor (I. Molto allegro)",
            composer = "Wolfgang Amadeus Mozart",
            era = MusicalEra.CLASSICAL,
            styleOrForm = "Classical Symphony (Sonata Form)",
            catalogNumber = "K. 550",
            stylisticClue = "Restless half-step sighing motif over pulsing violas—rare Classical-era Sturm und Drang minor-key urgency.",
            spotifyQuery = "Mozart Symphony No 40 K 550 Molto allegro",
            startPositionMs = 12_000L
        ),
        ClassicalWork(
            id = "beethoven-op67",
            title = "Symphony No. 5 in C minor (I. Allegro con brio)",
            composer = "Ludwig van Beethoven",
            era = MusicalEra.CLASSICAL,
            styleOrForm = "Symphony",
            catalogNumber = "Op. 67",
            stylisticClue = "Obsessive four-note rhythmic cell hammered across the entire orchestra with relentless motivic development.",
            spotifyQuery = "Beethoven Symphony No 5 Op 67 Allegro con brio",
            startPositionMs = 25_000L
        ),
        ClassicalWork(
            id = "schubert-d759",
            title = "Symphony No. 8 'Unfinished' (I. Allegro moderato)",
            composer = "Franz Schubert",
            era = MusicalEra.ROMANTIC,
            styleOrForm = "Lyrical Romantic Symphony",
            catalogNumber = "D. 759",
            stylisticClue = "Brooding low-string unison followed by a shimmering oboe/clarinet cantabile over rippling sixteenth-note strings.",
            spotifyQuery = "Schubert Symphony No 8 Unfinished Allegro moderato",
            startPositionMs = 35_000L
        ),
        ClassicalWork(
            id = "chopin-op9no2",
            title = "Nocturne in E-flat major",
            composer = "Frédéric Chopin",
            era = MusicalEra.ROMANTIC,
            styleOrForm = "Solo Piano Nocturne",
            catalogNumber = "Op. 9, No. 2",
            stylisticClue = "Bel canto vocal cantilena in the right hand with ornamental fioritura over a steady waltz-like bass.",
            spotifyQuery = "Chopin Nocturne Op 9 No 2",
            startPositionMs = 20_000L
        ),
        ClassicalWork(
            id = "brahms-op90",
            title = "Symphony No. 3 in F major (III. Poco allegretto)",
            composer = "Johannes Brahms",
            era = MusicalEra.ROMANTIC,
            styleOrForm = "Symphonic Movement",
            catalogNumber = "Op. 90",
            stylisticClue = "Autumnal cello melody wrapped in dense Brahmsian cross-rhythms (3-against-2) and warm horn sonorities.",
            spotifyQuery = "Brahms Symphony No 3 Poco allegretto",
            startPositionMs = 22_000L
        ),
        ClassicalWork(
            id = "tchaikovsky-op71",
            title = "The Nutcracker: Waltz of the Flowers",
            composer = "Pyotr Ilyich Tchaikovsky",
            era = MusicalEra.ROMANTIC,
            styleOrForm = "Ballet Suite Waltz",
            catalogNumber = "Op. 71a",
            stylisticClue = "Sweeping harp cadenza launching into lush Russian French-horn phrasing and crystalline orchestral color.",
            spotifyQuery = "Tchaikovsky Waltz of the Flowers",
            startPositionMs = 45_000L
        ),
        ClassicalWork(
            id = "mahler-sym5",
            title = "Symphony No. 5 (IV. Adagietto)",
            composer = "Gustav Mahler",
            era = MusicalEra.ROMANTIC,
            styleOrForm = "Late-Romantic Strings & Harp",
            catalogNumber = "Symphony No. 5",
            stylisticClue = "Suspended, aching chromatic suspensions scored purely for strings and harp—suspended time and bittersweet longing.",
            spotifyQuery = "Mahler Symphony No 5 Adagietto",
            startPositionMs = 40_000L
        ),
        ClassicalWork(
            id = "debussy-l75",
            title = "Clair de lune (Suite bergamasque)",
            composer = "Claude Debussy",
            era = MusicalEra.IMPRESSIONIST,
            styleOrForm = "Impressionist Piano Suite",
            catalogNumber = "L. 75",
            stylisticClue = "Floating ninth chords, blurred pedal resonance, and gentle 9/8 rubato avoiding functional Romantic cadences.",
            spotifyQuery = "Debussy Clair de lune",
            startPositionMs = 30_000L
        ),
        ClassicalWork(
            id = "ravel-bolero",
            title = "Boléro",
            composer = "Maurice Ravel",
            era = MusicalEra.IMPRESSIONIST,
            styleOrForm = "Orchestral Crescendo Study",
            catalogNumber = "M. 81",
            stylisticClue = "Unchanging ostinato snare drum and two-part modal melody passed instrument by instrument across a 15-minute crescendo.",
            spotifyQuery = "Ravel Bolero",
            startPositionMs = 60_000L
        ),
        ClassicalWork(
            id = "stravinsky-firebird",
            title = "The Rite of Spring (Augurs of Spring)",
            composer = "Igor Stravinsky",
            era = MusicalEra.MODERN,
            styleOrForm = "Modernist Ballet",
            catalogNumber = "K015",
            stylisticClue = "Polychordal string stomping with jagged, unpredictable accents and primitive Russian folk-inspired ostinatos.",
            spotifyQuery = "Stravinsky Rite of Spring Augurs of Spring",
            startPositionMs = 15_000L
        )
    )
}
