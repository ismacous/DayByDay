package com.ismael.daybyday.data

import java.util.Locale

/**
 * Les mots du bilan de la semaine.
 *
 * Ils vivent ici, en Kotlin pur, et pas dans la notification : c'est le seul
 * moyen de les tester, et c'est le seul texte de l'application qui arrive un
 * lundi matin sans qu'on l'ait demande. Il n'a pas le droit d'etre froid.
 *
 * La premiere version ne disait que des chiffres — « 2,3 / 3 sur 7 journee(s)
 * notee(s). Bouge 2 jour(s). Sorti 2 jour(s). » Un releve de compteur. Pour
 * quelqu'un qui traverse une semaine difficile, recevoir ca le lundi a neuf
 * heures ne sert a rien : ca ne console pas, ca ne propose rien, et ca donne
 * surtout l'impression d'etre note.
 *
 * L'ordre a donc ete inverse, et c'est tout le fichier :
 *
 * 1. **D'abord ce qu'on ressent** — une phrase qui reconnait la semaine telle
 *    qu'elle a ete. C'est elle qu'on lit dans le bandeau, avant meme de
 *    derouler.
 * 2. **Ensuite ce qui s'est passe**, en francais et pas en tableau : « tu as
 *    bouge trois jours » plutot que « Bouge 3 jour(s) ».
 * 3. **Enfin une chose a emporter** — un encouragement ou une proposition,
 *    tiree de la semaine elle-meme et jamais d'un catalogue de bonnes
 *    resolutions.
 *
 * Les regles d'ecriture sont celles du coup de pouce : on tutoie, on ne fait
 * jamais la morale, on ne promet rien, et on ne crie pas « bravo » une semaine
 * noire.
 */
object WeekWords {

    /** Ce qu'il y a a dire, decoupe comme une notification le demande. */
    data class Words(
        val title: String,
        /** La ligne visible sans derouler. C'est la phrase qui accueille. */
        val short: String,
        /** Le texte complet, une fois deroule. */
        val long: String,
    )

    /**
     * Comment la semaine s'est passee, vu de tres haut. Sert a choisir le ton,
     * et rien d'autre : les chiffres exacts sont dits plus bas.
     */
    enum class Mood { EMPTY, DARK, DOWN, STEADY, UP, BRIGHT }

    /** En dessous, la semaine a ete vraiment dure. Meme seuil que le coup de pouce. */
    private const val DARK_AVERAGE = 1.2

    /** Au-dessus, la semaine a ete bonne dans l'ensemble. */
    private const val BRIGHT_AVERAGE = 2.4

    /** Ecart avec la semaine d'avant a partir duquel il vaut la peine d'etre dit. */
    private const val MOVE = 0.4

    fun moodOf(review: WeekReview): Mood {
        val average = review.summary.average ?: return Mood.EMPTY
        val delta = review.delta
        return when {
            average < DARK_AVERAGE -> Mood.DARK
            delta != null && delta >= MOVE -> Mood.UP
            delta != null && delta <= -MOVE -> Mood.DOWN
            average >= BRIGHT_AVERAGE -> Mood.BRIGHT
            else -> Mood.STEADY
        }
    }

    /**
     * Le texte de la semaine. [variant] fait tourner les formulations : deux
     * lundis de suite ne doivent pas se ressembler mot pour mot, sinon on
     * arrete de lire des la troisieme fois.
     */
    fun of(review: WeekReview, firstName: String, variant: Int): Words {
        val mood = moodOf(review)
        val name = firstName.trim()
        val opening = pick(OPENINGS.getValue(mood), variant)
        val closing = closingFor(review, mood, variant)
        val facts = factsOf(review)

        val long = listOf(opening, facts, closing)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")

        return Words(title = titleFor(mood, name), short = opening, long = long)
    }

    // --- Le titre -----------------------------------------------------------

    private fun titleFor(mood: Mood, name: String): String {
        val suffix = if (name.isEmpty()) "" else " $name"
        return when (mood) {
            Mood.EMPTY -> "Ta semaine$suffix"
            Mood.DARK -> "Je suis là$suffix"
            Mood.DOWN -> "Ta semaine passée$suffix"
            Mood.STEADY -> "Ta semaine$suffix"
            Mood.UP -> "Ça remonte$suffix"
            Mood.BRIGHT -> "Belle semaine$suffix"
        }
    }

    // --- La phrase d'accueil ------------------------------------------------

    private val OPENINGS: Map<Mood, List<String>> = mapOf(
        Mood.EMPTY to listOf(
            "Aucune couleur posée la semaine dernière, et ce n'est pas grave. On repart d'aujourd'hui, sans rien rattraper.",
            "Semaine sans couleur au calendrier. Tu n'as rien à justifier : la page d'aujourd'hui t'attend, c'est tout.",
            "Rien de noté la semaine dernière. Ce carnet n'est pas un devoir à rendre — reprends quand tu veux.",
        ),
        Mood.DARK to listOf(
            "Cette semaine a été dure, et je ne vais pas faire semblant du contraire. Tu l'as traversée quand même.",
            "Une semaine lourde. Ce n'est pas toi qui fais mal les choses, c'est une période difficile à passer.",
            "Semaine sombre dans l'ensemble. Tu n'as rien à prouver, et sûrement rien à rattraper cette semaine-ci.",
            "Ça a été une semaine compliquée. Le simple fait de l'avoir notée, un jour comme ça, ce n'est pas rien.",
        ),
        Mood.DOWN to listOf(
            "Ta semaine a été plus lourde que la précédente. Ça arrive, et ça ne dit rien de la suite.",
            "Semaine un cran en dessous de celle d'avant. Pas de conclusion à en tirer : une semaine n'est qu'une semaine.",
            "Un peu plus difficile que la semaine d'avant. Tu as le droit de lever le pied sur celle qui commence.",
        ),
        Mood.STEADY to listOf(
            "Semaine dans la moyenne, sans grande secousse. C'est une base, et ça vaut plus qu'on ne croit.",
            "Une semaine ordinaire, ni haut ni bas. Les semaines comme ça sont celles qui tiennent le reste.",
            "Ta semaine est restée stable. Rien de spectaculaire, et c'est très bien comme ça.",
        ),
        Mood.UP to listOf(
            "Ta semaine remonte par rapport à la précédente, et ça se voit. Bravo, garde ce que tu as changé !",
            "Nettement mieux que la semaine d'avant. Chapeau — regarde ce que tu as fait de différent.",
            "Ça remonte franchement depuis la semaine dernière. Continue exactement comme ça !",
        ),
        Mood.BRIGHT to listOf(
            "Belle semaine dans l'ensemble. Savoure-la, tu l'as méritée !",
            "Une bonne semaine, et ce n'est pas un hasard. Profite, et retiens ce qu'il y avait dedans !",
            "Ta semaine a été lumineuse. Bravo — ce sont celles-là qu'on oublie le plus vite.",
        ),
    )

    // --- Ce qui s'est passe -------------------------------------------------

    /**
     * Les faits de la semaine, en une phrase de francais.
     *
     * On ne dit que ce qui est vrai et non nul : une ligne « Sorti 0 jour(s) »
     * est un reproche deguise, alors que ne rien dire n'en est pas un.
     */
    private fun factsOf(review: WeekReview): String {
        val bits = mutableListOf<String>()
        if (review.movedDays > 0) bits += "tu as bougé ${days(review.movedDays)}"
        if (review.wentOutDays > 0) bits += "tu es sorti ${times(review.wentOutDays)}"
        if (review.writtenDays > 0) bits += "tu as écrit ${days(review.writtenDays)}"
        if (review.photos > 0) bits += "tu as gardé ${count(review.photos, "photo", "photos")}"
        val average = review.summary.average
        val filled = review.summary.filledDays
        val head = when {
            average == null -> ""
            filled == 1 -> "Tu n'as noté qu'une journée, à ${decimal(average)} sur 3. "
            else -> "Sur les ${word(filled)} journées que tu as notées, " +
                "ta moyenne est de ${decimal(average)} sur 3. "
        }
        // Rien a raconter d'autre que la moyenne : on s'arrete la plutot que
        // d'aligner des zeros, qui seraient lus comme des reproches.
        if (bits.isEmpty()) return head.trim()
        return head + sentence(bits) + "."
    }

    // --- La chose a emporter ------------------------------------------------

    /**
     * Une seule proposition, tiree de la semaine elle-meme.
     *
     * Elle n'apparait que si la semaine la justifie : une semaine noire ne
     * recoit jamais de conseil, seulement une presence. Proposer d'aller
     * marcher a quelqu'un qui n'arrive pas a se lever, c'est la meilleure
     * facon de lui faire desinstaller l'application.
     */
    private fun closingFor(review: WeekReview, mood: Mood, variant: Int): String {
        if (mood == Mood.EMPTY) return ""
        if (mood == Mood.DARK) {
            return pick(
                listOf(
                    "Cette semaine, vise le minimum : boire, manger, dormir. Le reste peut attendre.",
                    "Si tu peux parler à quelqu'un cette semaine — ta moitié, ta famille, un médecin — c'est peut-être le bon moment.",
                    "Pas d'objectif pour cette semaine. Juste la passer, ça suffira largement.",
                ),
                variant,
            )
        }

        val ideas = mutableListOf<String>()
        if (review.wentOutDays <= 1) {
            ideas += "Et si tu sortais une fois de plus cette semaine ? Même dix minutes dehors comptent."
        }
        if (review.movedDays <= 1) {
            ideas += "Une marche, même courte, dans la semaine qui vient ? Tu n'es obligé à rien."
        }
        if (review.writtenDays <= 2) {
            ideas += "Deux lignes dans ton journal un soir de cette semaine, et dans six mois tu sauras ce qu'il y avait dedans."
        }
        review.brightest?.let { best ->
            if (best.color == DayColor.GREEN) {
                ideas += "Ta meilleure journée de la semaine est encore fraîche : va voir ce que tu avais coché dessus."
            }
        }
        if (review.sleepAverageMinutes != null && review.sleepAverageMinutes < 6 * 60) {
            ideas += "Tes nuits ont été courtes. Te coucher un quart d'heure plus tôt, ça se tente sans rien bouleverser."
        }
        if (ideas.isEmpty()) {
            ideas += "Rien à changer : continue comme tu fais, c'est déjà ça de tenu."
        }
        return pick(ideas, variant)
    }

    // --- Petits outils ------------------------------------------------------

    private fun <T> pick(options: List<T>, variant: Int): T =
        options[Math.floorMod(variant, options.size)]

    /** « un jour », « trois jours » — et jamais « 1 jour(s) ». */
    private fun days(n: Int): String = count(n, "jour", "jours")

    private fun times(n: Int): String = if (n == 1) "une fois" else "${word(n)} fois"

    private fun count(n: Int, one: String, many: String): String =
        if (n == 1) "un $one" else "${word(n)} $many"

    /**
     * Les petits nombres s'ecrivent en lettres : « trois jours » se lit, « 3
     * jour(s) » se compte. Au-dela de dix, le chiffre reprend la main.
     */
    private fun word(n: Int): String = when (n) {
        1 -> "un"
        2 -> "deux"
        3 -> "trois"
        4 -> "quatre"
        5 -> "cinq"
        6 -> "six"
        7 -> "sept"
        8 -> "huit"
        9 -> "neuf"
        10 -> "dix"
        else -> n.toString()
    }

    private fun decimal(value: Double): String = String.format(Locale.FRANCE, "%.1f", value)

    /** « a, b et c » : une enumeration qui se lit a voix haute. */
    private fun sentence(bits: List<String>): String = when (bits.size) {
        1 -> bits[0].replaceFirstChar { it.uppercase(Locale.FRANCE) }
        else -> (bits.dropLast(1).joinToString(", ") + " et " + bits.last())
            .replaceFirstChar { it.uppercase(Locale.FRANCE) }
    }
}
