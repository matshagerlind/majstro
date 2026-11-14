package com.majstro.tv.data

import com.majstro.tv.domain.model.ClassicalWork
import com.majstro.tv.domain.model.MusicalEra
import com.majstro.tv.domain.port.RepertoireSource

/**
 * Curated 25-work seed repertoire across 5 difficulty levels (5 works per level),
 * each with exact composition year for Closest-Year scoring.
 */
class InMemoryRepertoireSource : RepertoireSource {

    override suspend fun loadWorks(): List<ClassicalWork> = seedWorks

    private val seedWorks = listOf(
        // ── Level 1: Iconic Classics ───────────────────────────────────────────────────────
        ClassicalWork(
            id = "vivaldi-rv269",
            title = "The Four Seasons: Spring (Allegro)",
            composer = "Antonio Vivaldi",
            era = MusicalEra.BAROQUE,
            styleOrForm = "Violin Concerto (Ritornello)",
            catalogNumber = "RV 269",
            yearWritten = 1723,
            difficultyLevel = 1,
            stylisticClue = "Bouncy Venetian ritornello strings alternating with bird-call solo violin episodes and harpsichord continuo.",
            spotifyQuery = "Vivaldi Four Seasons Spring Allegro",
            startPositionMs = 20_000L
        ),
        ClassicalWork(
            id = "mozart-k525",
            title = "Eine kleine Nachtmusik (I. Allegro)",
            composer = "Wolfgang Amadeus Mozart",
            era = MusicalEra.CLASSICAL,
            styleOrForm = "String Serenade",
            catalogNumber = "K. 525",
            yearWritten = 1787,
            difficultyLevel = 1,
            stylisticClue = "Crisp Mannheim-rocket arpeggio opening in G major for chamber strings.",
            spotifyQuery = "Mozart Eine kleine Nachtmusik Allegro",
            startPositionMs = 5_000L
        ),
        ClassicalWork(
            id = "beethoven-op67",
            title = "Symphony No. 5 in C minor (I. Allegro con brio)",
            composer = "Ludwig van Beethoven",
            era = MusicalEra.CLASSICAL,
            styleOrForm = "Symphony",
            catalogNumber = "Op. 67",
            yearWritten = 1808,
            difficultyLevel = 1,
            stylisticClue = "Obsessive four-note rhythmic cell hammered across the entire orchestra with relentless motivic development.",
            spotifyQuery = "Beethoven Symphony No 5 Op 67 Allegro con brio",
            startPositionMs = 25_000L
        ),
        ClassicalWork(
            id = "bizet-carmen-toreador",
            title = "Carmen: Votre toast (Toreador Song)",
            composer = "Georges Bizet",
            era = MusicalEra.ROMANTIC,
            styleOrForm = "Opéra-Comique Aria & March",
            catalogNumber = "WD 31",
            yearWritten = 1875,
            difficultyLevel = 1,
            stylisticClue = "Swaggering Spanish-inflected orchestral march building into an unmistakable heroic refrain.",
            spotifyQuery = "Bizet Carmen Toreador Song Votre toast",
            startPositionMs = 15_000L
        ),
        ClassicalWork(
            id = "tchaikovsky-op71",
            title = "The Nutcracker: Waltz of the Flowers",
            composer = "Pyotr Ilyich Tchaikovsky",
            era = MusicalEra.ROMANTIC,
            styleOrForm = "Ballet Suite Waltz",
            catalogNumber = "Op. 71a",
            yearWritten = 1892,
            difficultyLevel = 1,
            stylisticClue = "Sweeping harp cadenza launching into lush Russian French-horn phrasing and crystalline orchestral color.",
            spotifyQuery = "Tchaikovsky Waltz of the Flowers",
            startPositionMs = 45_000L
        ),

        // ── Level 2: Concert Favorites ─────────────────────────────────────────────────────
        ClassicalWork(
            id = "bach-bwv565",
            title = "Toccata and Fugue in D minor",
            composer = "Johann Sebastian Bach",
            era = MusicalEra.BAROQUE,
            styleOrForm = "Organ Toccata & Fugue",
            catalogNumber = "BWV 565",
            yearWritten = 1704,
            difficultyLevel = 2,
            stylisticClue = "Dramatic solo pipe organ rhetoric followed by tight four-voice Baroque counterpoint.",
            spotifyQuery = "Bach Toccata and Fugue in D minor BWV 565",
            startPositionMs = 15_000L
        ),
        ClassicalWork(
            id = "mozart-k550",
            title = "Symphony No. 40 in G minor (I. Molto allegro)",
            composer = "Wolfgang Amadeus Mozart",
            era = MusicalEra.CLASSICAL,
            styleOrForm = "Classical Symphony (Sonata Form)",
            catalogNumber = "K. 550",
            yearWritten = 1788,
            difficultyLevel = 2,
            stylisticClue = "Restless half-step sighing motif over pulsing violas—Classical-era Sturm und Drang minor-key urgency.",
            spotifyQuery = "Mozart Symphony No 40 K 550 Molto allegro",
            startPositionMs = 12_000L
        ),
        ClassicalWork(
            id = "chopin-op9no2",
            title = "Nocturne in E-flat major",
            composer = "Frédéric Chopin",
            era = MusicalEra.ROMANTIC,
            styleOrForm = "Solo Piano Nocturne",
            catalogNumber = "Op. 9, No. 2",
            yearWritten = 1832,
            difficultyLevel = 2,
            stylisticClue = "Bel canto vocal cantilena in the right hand with ornamental fioritura over a steady waltz-like bass.",
            spotifyQuery = "Chopin Nocturne Op 9 No 2",
            startPositionMs = 20_000L
        ),
        ClassicalWork(
            id = "debussy-l75",
            title = "Clair de lune (Suite bergamasque)",
            composer = "Claude Debussy",
            era = MusicalEra.IMPRESSIONIST,
            styleOrForm = "Impressionist Piano Suite",
            catalogNumber = "L. 75",
            yearWritten = 1905,
            difficultyLevel = 2,
            stylisticClue = "Floating ninth chords, blurred pedal resonance, and gentle 9/8 rubato avoiding functional cadences.",
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
            yearWritten = 1928,
            difficultyLevel = 2,
            stylisticClue = "Unchanging ostinato snare drum and two-part modal melody passed instrument by instrument.",
            spotifyQuery = "Ravel Bolero",
            startPositionMs = 60_000L
        ),

        // ── Level 3: Standard Repertoire ───────────────────────────────────────────────────
        ClassicalWork(
            id = "handel-hwv349",
            title = "Water Music Suite No. 2: Alla Hornpipe",
            composer = "George Frideric Handel",
            era = MusicalEra.BAROQUE,
            styleOrForm = "Orchestral Baroque Suite",
            catalogNumber = "HWV 349",
            yearWritten = 1717,
            difficultyLevel = 3,
            stylisticClue = "Festive outdoor antiphony trading bright natural horns and trumpets against oboe-led strings.",
            spotifyQuery = "Handel Water Music Alla Hornpipe",
            startPositionMs = 10_000L
        ),
        ClassicalWork(
            id = "schubert-d759",
            title = "Symphony No. 8 'Unfinished' (I. Allegro moderato)",
            composer = "Franz Schubert",
            era = MusicalEra.ROMANTIC,
            styleOrForm = "Lyrical Romantic Symphony",
            catalogNumber = "D. 759",
            yearWritten = 1822,
            difficultyLevel = 3,
            stylisticClue = "Brooding low-string unison followed by an oboe/clarinet cantabile over rippling sixteenth-note strings.",
            spotifyQuery = "Schubert Symphony No 8 Unfinished Allegro moderato",
            startPositionMs = 35_000L
        ),
        ClassicalWork(
            id = "brahms-op90",
            title = "Symphony No. 3 in F major (III. Poco allegretto)",
            composer = "Johannes Brahms",
            era = MusicalEra.ROMANTIC,
            styleOrForm = "Symphonic Movement",
            catalogNumber = "Op. 90",
            yearWritten = 1883,
            difficultyLevel = 3,
            stylisticClue = "Autumnal cello melody wrapped in Brahmsian cross-rhythms and warm horn sonorities.",
            spotifyQuery = "Brahms Symphony No 3 Poco allegretto",
            startPositionMs = 22_000L
        ),
        ClassicalWork(
            id = "dvorak-op95",
            title = "Symphony No. 9 'From the New World' (II. Largo)",
            composer = "Antonín Dvořák",
            era = MusicalEra.ROMANTIC,
            styleOrForm = "Symphony Slow Movement",
            catalogNumber = "Op. 95",
            yearWritten = 1893,
            difficultyLevel = 3,
            stylisticClue = "Hushed brass chorale modulation opening onto an aching solo English horn melody.",
            spotifyQuery = "Dvorak Symphony No 9 Largo English horn",
            startPositionMs = 45_000L
        ),
        ClassicalWork(
            id = "mahler-sym5",
            title = "Symphony No. 5 (IV. Adagietto)",
            composer = "Gustav Mahler",
            era = MusicalEra.ROMANTIC,
            styleOrForm = "Late-Romantic Strings & Harp",
            catalogNumber = "Symphony No. 5",
            yearWritten = 1902,
            difficultyLevel = 3,
            stylisticClue = "Suspended chromatic suspensions scored purely for strings and harp with bittersweet longing.",
            spotifyQuery = "Mahler Symphony No 5 Adagietto",
            startPositionMs = 40_000L
        ),

        // ── Level 4: Musician / Advanced ───────────────────────────────────────────────────
        ClassicalWork(
            id = "bach-bwv582",
            title = "Passacaglia and Fugue in C minor",
            composer = "Johann Sebastian Bach",
            era = MusicalEra.BAROQUE,
            styleOrForm = "Organ Ground-Bass Variations",
            catalogNumber = "BWV 582",
            yearWritten = 1713,
            difficultyLevel = 4,
            stylisticClue = "Eight-measure pedal ostinato climbing through twenty polyphonic variations into a double fugue.",
            spotifyQuery = "Bach Passacaglia and Fugue in C minor BWV 582",
            startPositionMs = 25_000L
        ),
        ClassicalWork(
            id = "mozart-k361",
            title = "Serenade No. 10 'Gran Partita' (III. Adagio)",
            composer = "Wolfgang Amadeus Mozart",
            era = MusicalEra.CLASSICAL,
            styleOrForm = "Wind Serenade (13 Instruments)",
            catalogNumber = "K. 361",
            yearWritten = 1781,
            difficultyLevel = 4,
            stylisticClue = "Pulsing accompaniment beneath trading solos for oboe, clarinet, and velvety basset horns.",
            spotifyQuery = "Mozart Gran Partita K 361 Adagio",
            startPositionMs = 20_000L
        ),
        ClassicalWork(
            id = "haydn-op76no2",
            title = "String Quartet in D minor 'Fifths' (I. Allegro)",
            composer = "Joseph Haydn",
            era = MusicalEra.CLASSICAL,
            styleOrForm = "String Quartet",
            catalogNumber = "Op. 76, No. 2",
            yearWritten = 1797,
            difficultyLevel = 4,
            stylisticClue = "Stark falling fifths in the first violin answered by terse contrapuntal wit and canonic drive.",
            spotifyQuery = "Haydn String Quartet Op 76 No 2 Fifths Allegro",
            startPositionMs = 10_000L
        ),
        ClassicalWork(
            id = "berlioz-op14",
            title = "Symphonie fantastique (III. Scène aux champs)",
            composer = "Hector Berlioz",
            era = MusicalEra.ROMANTIC,
            styleOrForm = "Program Symphony",
            catalogNumber = "Op. 14",
            yearWritten = 1830,
            difficultyLevel = 4,
            stylisticClue = "Pastoral dialogue between offstage oboe and onstage English horn later shadowed by four-timpani thunder.",
            spotifyQuery = "Berlioz Symphonie fantastique Scene aux champs",
            startPositionMs = 15_000L
        ),
        ClassicalWork(
            id = "brahms-op98",
            title = "Symphony No. 4 in E minor (IV. Allegro energico)",
            composer = "Johannes Brahms",
            era = MusicalEra.ROMANTIC,
            styleOrForm = "Symphonic Passacaglia",
            catalogNumber = "Op. 98",
            yearWritten = 1885,
            difficultyLevel = 4,
            stylisticClue = "Eight-bar Baroque-inspired chaconne theme hammered by winds and brass across thirty symphonic variations.",
            spotifyQuery = "Brahms Symphony No 4 Allegro energico e passionato",
            startPositionMs = 12_000L
        ),

        // ── Level 5: Conservatory Virtuoso ─────────────────────────────────────────────────
        ClassicalWork(
            id = "beethoven-op131",
            title = "String Quartet No. 14 in C-sharp minor (I. Adagio ma non troppo)",
            composer = "Ludwig van Beethoven",
            era = MusicalEra.ROMANTIC,
            styleOrForm = "Late String Quartet Fugue",
            catalogNumber = "Op. 131",
            yearWritten = 1826,
            difficultyLevel = 5,
            stylisticClue = "Subdominant-answering opening fugue full of agonizing chromatic half-steps without pause.",
            spotifyQuery = "Beethoven String Quartet Op 131 Adagio ma non troppo",
            startPositionMs = 18_000L
        ),
        ClassicalWork(
            id = "mahler-sym9",
            title = "Symphony No. 9 in D major (I. Andante comodo)",
            composer = "Gustav Mahler",
            era = MusicalEra.MODERN,
            styleOrForm = "Late-Romantic / Early Modern Symphony",
            catalogNumber = "Symphony No. 9",
            yearWritten = 1909,
            difficultyLevel = 5,
            stylisticClue = "Halt-stepped cello, horn, and harp heartbeat fracturing into soaring, nostalgic string polyphony.",
            spotifyQuery = "Mahler Symphony No 9 Andante comodo",
            startPositionMs = 30_000L
        ),
        ClassicalWork(
            id = "stravinsky-rite",
            title = "The Rite of Spring (Augurs of Spring)",
            composer = "Igor Stravinsky",
            era = MusicalEra.MODERN,
            styleOrForm = "Modernist Ballet",
            catalogNumber = "K015",
            yearWritten = 1913,
            difficultyLevel = 5,
            stylisticClue = "Polychordal string stomping with jagged, unpredictable accents and primitive Russian folk-inspired ostinatos.",
            spotifyQuery = "Stravinsky Rite of Spring Augurs of Spring",
            startPositionMs = 15_000L
        ),
        ClassicalWork(
            id = "bartok-sz116",
            title = "Concerto for Orchestra (II. Giuoco delle coppie)",
            composer = "Béla Bartók",
            era = MusicalEra.MODERN,
            styleOrForm = "20th-Century Concerto for Orchestra",
            catalogNumber = "Sz. 116",
            yearWritten = 1943,
            difficultyLevel = 5,
            stylisticClue = "Wry snareless side drum introduction followed by wind pairs marching in fixed parallel intervals.",
            spotifyQuery = "Bartok Concerto for Orchestra Giuoco delle coppie",
            startPositionMs = 15_000L
        ),
        ClassicalWork(
            id = "shostakovich-op93",
            title = "Symphony No. 10 in E minor (II. Allegro)",
            composer = "Dmitri Shostakovich",
            era = MusicalEra.MODERN,
            styleOrForm = "Soviet Symphonic Scherzo",
            catalogNumber = "Op. 93",
            yearWritten = 1953,
            difficultyLevel = 5,
            stylisticClue = "Relentless four-minute whirlwind of biting piccolos, shrieking E-flat clarinet, and brutal snare-drum snaps.",
            spotifyQuery = "Shostakovich Symphony No 10 II Allegro",
            startPositionMs = 15_000L
        )
    )
}
