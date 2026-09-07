package com.ismael.daybyday.data

/**
 * Un geste qu'une journee peut porter.
 *
 * **Rien ne se perd, tout s'ajoute.** C'est la regle qui decide de tout le
 * reste : un geste fait ajoute des points, un geste non fait n'en retire
 * jamais. Une application qui suit un moral qui varie beaucoup ne peut pas se
 * permettre de punir un mauvais jour — c'est deja la regle des badges, et c'est
 * la meme ici.
 *
 * Ce sont d'ailleurs **les memes gestes que les badges**, et ce n'est pas un
 * hasard : les deux repondent a la meme question, « qu'est-ce que tu as
 * reussi a faire aujourd'hui ? ». Les faire diverger aurait donne une medaille
 * sans point, ou un point sans medaille, et personne n'aurait compris
 * lequel des deux comptait.
 *
 * Chaque geste **appartient a une carte**. Masquer une carte retire donc ses
 * gestes du calcul — des deux cotes de la fraction. C'est ce qui fait qu'on ne
 * perd rien a masquer les traitements quand on n'en prend pas, ni l'hygiene
 * quand on n'en a pas besoin : ces gestes n'etaient simplement jamais
 * possibles.
 */
enum class Deed(
    val key: String,
    val label: String,
    /** La carte sans laquelle ce geste n'est ni visible ni possible. */
    val card: DayCard,
    /** Le badge qui felicite le meme geste, quand il y en a un. */
    val badge: Badge?,
) {
    PRAYERS("prieres", "Les cinq prières", DayCard.PRAYER, Badge.PRAYERS),
    STEPS("pas", "6 000 pas", DayCard.ACTIVITY, Badge.STEPS),
    WORKOUT("seance", "Une vraie séance", DayCard.ACTIVITY, Badge.WORKOUT),
    OUTSIDE("sortie", "Être sorti", DayCard.ACTIVITY, Badge.OUTSIDE),
    WATER("eau", "Huit verres d'eau", DayCard.FOOD, Badge.WATER),
    APPLICATION("candidature", "Une candidature", DayCard.WORK, Badge.APPLICATION),
    JOURNAL("journal", "La journée écrite", DayCard.JOURNAL, Badge.JOURNAL),
    SHOWER("douche", "La douche", DayCard.HYGIENE, Badge.SHOWER),
    TEETH("dents", "Les trois brossages", DayCard.HYGIENE, Badge.TEETH),
    ;

    /** Ce geste a-t-il ete fait ce jour-la ? */
    fun doneOn(entry: DayEntry): Boolean = when (this) {
        PRAYERS -> (entry.prayerMask ?: 0) == Prayer.ALL_DONE
        STEPS -> (entry.steps ?: 0) >= STEPS_GOAL
        WORKOUT -> entry.sport == SportLevel.GOOD
        OUTSIDE -> entry.wentOut == true
        WATER -> (entry.waterGlasses ?: 0) >= WATER_GOAL
        APPLICATION -> (entry.jobApplications ?: 0) > 0
        JOURNAL -> entry.note.isNotBlank()
        SHOWER -> entry.showered == true
        TEETH -> (entry.brushMask ?: 0) == Brushing.ALL_DONE
    }

    companion object {
        const val STEPS_GOAL = 6_000
        const val WATER_GOAL = 8

        /**
         * Les gestes que les cartes affichees rendent possibles.
         *
         * Une carte masquee sort du calcul : ses gestes ne comptent ni au
         * numerateur ni au denominateur. Masquer les traitements quand on n'en
         * prend pas ne doit pas faire perdre de points — ce serait punir
         * quelqu'un pour une case qui ne le concerne pas.
         */
        fun possibleWith(hiddenCards: Set<DayCard>): List<Deed> =
            entries.filterNot { it.card in hiddenCards }

        /** Combien de ces gestes la journee a portes. */
        fun doneCount(entry: DayEntry, possible: List<Deed>): Int =
            possible.count { it.doneOn(entry) }
    }
}

/**
 * La note d'une periode : ce qu'on a ressenti, et ce qu'on a fait.
 *
 * **Deux moities de dix points, et elles ne disent pas la meme chose.** Le
 * ressenti vient de la couleur des journees, et lui seul — c'est la matiere de
 * l'application, et rien de ce qu'on fait ne doit pouvoir la corriger. Les
 * actions viennent des gestes reussis. Les additionner donne une note qui monte
 * quand la journee a ete bonne **ou** quand on s'est bouge, ce qui est
 * exactement ce qu'on veut voir sur une semaine difficile : le ressenti est
 * bas, et pourtant la note n'est pas a zero, parce qu'on a fait des choses.
 *
 * Les actions sont une **part** (gestes faits sur gestes possibles), pas un
 * compte : sans ca, quelqu'un qui affiche trois cartes aurait mecaniquement une
 * note plus basse que quelqu'un qui en affiche dix, sans rien avoir fait de
 * moins.
 */
data class Note(
    /** Moyenne des couleurs, de 0 a 3. `null` si aucune journee n'est notee. */
    val moodAverage: Double?,
    val deedsDone: Int,
    val deedsPossible: Int,
) {
    /** Le ressenti, sur dix. */
    val mood: Double? get() = moodAverage?.let { it / DayColor.MAX_SCORE * HALF }

    /** Les actions, sur dix. `null` quand aucun geste n'etait possible. */
    val actions: Double?
        get() = if (deedsPossible <= 0) null else deedsDone.toDouble() / deedsPossible * HALF

    /**
     * Le total, et le maximum atteignable.
     *
     * Sur vingt d'ordinaire ; sur dix si aucune carte a geste n'est affichee —
     * il n'y a alors rien a compter, et pretendre le contraire donnerait une
     * note plafonnee a la moitie sans que rien ne l'explique.
     */
    val outOf: Int get() = if (deedsPossible <= 0) HALF.toInt() else (HALF * 2).toInt()

    val total: Double?
        get() {
            val feeling = mood ?: return null
            return feeling + (actions ?: 0.0)
        }

    private companion object {
        const val HALF = 10.0
    }
}
