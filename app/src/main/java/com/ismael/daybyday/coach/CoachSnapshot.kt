package com.ismael.daybyday.coach

import com.ismael.daybyday.data.DayCard
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.DayPart
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.DayTagCrossRef
import com.ismael.daybyday.data.DoseTaken
import com.ismael.daybyday.data.FoodLevel
import com.ismael.daybyday.data.MoneyCategory
import com.ismael.daybyday.data.MoneyEntry
import com.ismael.daybyday.data.Prayer
import com.ismael.daybyday.data.SportLevel
import com.ismael.daybyday.data.Tag
import com.ismael.daybyday.data.Treatment
import java.time.LocalDate

/**
 * Une journee reduite a ce dont l'algorithme a besoin.
 *
 * Le titre et le texte du journal n'y sont pas, et c'est une regle : le coup
 * de pouce ne lit jamais ce qui est ecrit, il regarde seulement ce qui est
 * coche. Le seul champ qui evoque le journal est [hasNote], un oui-ou-non.
 */
data class CoachDay(
    val epochDay: Long,
    val colorKey: Int? = null,
    val colorManual: Boolean = false,
    /**
     * La couleur du jour est-elle **arretee** ?
     *
     * C'est le garde-fou contre les faux positifs de tout l'algorithme, et il
     * tient en une phrase : tant que la journee n'est pas jouee, la couleur
     * affichee n'est que la moyenne des moments deja notes. Un matin vert seul
     * fait une journee verte a l'ecran — et le soir peut tout changer.
     * Repondre a cette couleur-la, c'est commenter le debut d'un film.
     *
     * Voir [CoachSnapshot.build] pour les quatre facons de l'arreter.
     */
    val colorSettled: Boolean = true,
    /**
     * Les quatre moments, dans l'ordre matin, apres-midi, soir, nuit.
     * `null` pour un moment non rempli — c'est ce qui permet de dire quel
     * moment de la journee est le plus dur **sans** compter les trous.
     */
    val partScores: List<Int?> = listOf(null, null, null, null),
    val sportLevel: Int? = null,
    val foodLevel: Int? = null,
    val wentOut: Boolean? = null,
    val steps: Int? = null,
    val screenMinutes: Int? = null,
    val weightKg: Double? = null,
    val hasNote: Boolean = false,
    /** Duree de la nuit en minutes, quand les deux heures sont saisies. */
    val sleepMinutes: Int? = null,
    /** Heure du coucher en minutes depuis minuit. */
    val sleepStartMinutes: Int? = null,
    val waterGlasses: Int? = null,
    val showered: Boolean? = null,
    val brushingsDone: Int = 0,
    val prayersDone: Int = 0,
    /** `true` quand la carte des prieres a ete touchee ce jour-la. */
    val prayersTouched: Boolean = false,
    val jobApplications: Int? = null,
    val tagSlugs: Set<String> = emptySet(),
    val mediaCount: Int = 0,
    /** Nombre de prises de traitement cochees ce jour-la. */
    val dosesTaken: Int = 0,
    /**
     * Depenses du jour en centimes, en valeur positive, **corrections de solde
     * exclues** : une correction n'est pas une depense, et la compter ferait
     * dire n'importe quoi aux comparaisons de mois.
     */
    val spentCents: Long = 0,
    /** Rentrees du jour en centimes, corrections de solde exclues. */
    val earnedCents: Long = 0,
) {
    /** Nombre de moments remplis dans la journee. */
    val partCount: Int get() = partScores.count { it != null }

    /** La couleur telle qu'elle est affichee, arretee ou non. */
    val rawColor: DayColor? get() = DayColor.fromKey(colorKey)

    /**
     * La couleur dont les regles ont le droit de parler : celle qui ne bougera
     * plus. Une couleur encore provisoire vaut `null`, exactement comme une
     * journee non notee — les regles n'ont alors rien a dire, et c'est voulu.
     */
    val color: DayColor? get() = if (colorSettled) rawColor else null

    val score: Int? get() = color?.score

    /**
     * Une couleur existe sur le calendrier. Volontairement **non** filtre par
     * [colorSettled] : pour compter les trous du mois ou les jours notes
     * d'affilee, ce qui compte est qu'il y ait quelque chose, pas que ce soit
     * definitif.
     */
    val isNoted: Boolean get() = colorKey != null

    val moved: Boolean? get() = sportLevel?.let { it >= SportLevel.LIGHT.key }

    val ateWell: Boolean? get() = foodLevel?.let { it == FoodLevel.GOOD.key }

    val allPrayersDone: Boolean get() = prayersDone == Prayer.entries.size

    /** Une vraie nuit : sept heures ou plus. */
    val sleptWell: Boolean? get() = sleepMinutes?.let { it >= 7 * 60 }
}

/**
 * Tout ce que l'algorithme a sous les yeux a un instant donne. C'est du
 * Kotlin pur : les regles peuvent donc etre testees sans telephone.
 */
data class CoachSnapshot(
    val today: LocalDate,
    /** Heure locale, pour ne pas proposer une marche a 2 h du matin. */
    val hourOfDay: Int,
    val firstName: String,
    val birthDate: LocalDate?,
    val days: Map<Long, CoachDay>,
    /** Y a-t-il des traitements a prendre en ce moment ? */
    val hasActiveTreatments: Boolean = false,
    /**
     * Les cartes masquees dans "Organiser ma journee", par leurs cles.
     *
     * Une carte masquee fait taire les regles qui en parlent : masquer une
     * carte ne doit rien couter, pas plus ici que dans la note.
     */
    val hiddenCardKeys: Set<String> = emptySet(),
    /**
     * Les jours ou au moins un mouvement d'argent est enregistre.
     *
     * A part, et pas deduit des journees : un mouvement peut exister un jour
     * qui n'a aucune autre ligne, et c'est justement ces jours-la qu'il faut
     * voir pour savoir depuis quand le suivi decroche.
     */
    val moneyDays: Set<Long> = emptySet(),
) {
    val todayEpochDay: Long = today.toEpochDay()

    val todayDay: CoachDay get() = days[todayEpochDay] ?: CoachDay(todayEpochDay)

    /** La journee a [back] jours en arriere (0 = aujourd'hui). */
    fun dayBefore(back: Int): CoachDay? = days[todayEpochDay - back]

    /** Moyenne des couleurs sur une tranche de jours, ou null si rien n'est note. */
    fun averageOver(from: Int, to: Int): Double? {
        val scores = (from..to).mapNotNull { back -> days[todayEpochDay - back]?.score }
        return if (scores.isEmpty()) null else scores.average()
    }

    /** Nombre de jours notes sur une tranche. */
    fun notedCount(from: Int, to: Int): Int =
        (from..to).count { back -> days[todayEpochDay - back]?.isNoted == true }

    /**
     * Nombre de jours depuis la derniere fois ou [present] etait vrai, en
     * remontant jusqu'a [limit] jours. `null` si on ne trouve rien.
     */
    fun daysSince(limit: Int = 60, present: (CoachDay) -> Boolean): Int? {
        for (back in 0..limit) {
            val day = days[todayEpochDay - back] ?: continue
            if (present(day)) return back
        }
        return null
    }

    /** Combien de jours parmi les [count] derniers verifient [present]. */
    fun countWhere(count: Int, present: (CoachDay) -> Boolean): Int =
        (0 until count).count { back -> days[todayEpochDay - back]?.let(present) == true }

    /** Somme d'une mesure sur les [count] derniers jours. */
    fun sumOver(count: Int, value: (CoachDay) -> Int?): Int =
        (0 until count).sumOf { back -> days[todayEpochDay - back]?.let(value) ?: 0 }

    /**
     * Longueur de la serie de jours consecutifs qui verifient [present], en
     * partant de [startBack] et en remontant le temps.
     */
    fun streakOf(startBack: Int = 0, limit: Int = 60, present: (CoachDay) -> Boolean): Int {
        var length = 0
        for (back in startBack until startBack + limit) {
            val day = days[todayEpochDay - back] ?: return length
            if (!present(day)) return length
            length += 1
        }
        return length
    }

    companion object {

        /**
         * Assemble la photo du moment a partir de ce que contient la base.
         * Les donnees arrivent telles quelles : c'est ici, et nulle part
         * ailleurs, qu'on les met en forme pour l'algorithme.
         */
        fun build(
            today: LocalDate,
            hourOfDay: Int,
            firstName: String,
            birthDate: LocalDate?,
            entries: List<DayEntry>,
            tags: List<Tag>,
            links: List<DayTagCrossRef>,
            mediaCounts: Map<Long, Int> = emptyMap(),
            money: List<MoneyEntry> = emptyList(),
            treatments: List<Treatment> = emptyList(),
            doses: List<DoseTaken> = emptyList(),
            hiddenCards: Set<DayCard> = emptySet(),
        ): CoachSnapshot {
            val slugById = tags.mapNotNull { tag -> tag.slug?.let { tag.id to it } }.toMap()
            val slugsByDay = links
                .mapNotNull { link -> slugById[link.tagId]?.let { link.epochDay to it } }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, slugs) -> slugs.toSet() }
            // Les corrections de solde sortent des deux cotes : ce n'est ni une
            // depense ni une rentree, c'est une remise a zero du compteur.
            val realMoney = money.filter { it.category != MoneyCategory.ADJUSTMENT }
            val spentByDay = realMoney
                .filter { it.amountCents < 0 }
                .groupBy { it.epochDay }
                .mapValues { (_, list) -> -list.sumOf { it.amountCents } }
            val earnedByDay = realMoney
                .filter { it.amountCents > 0 }
                .groupBy { it.epochDay }
                .mapValues { (_, list) -> list.sumOf { it.amountCents } }
            val moneyDays = realMoney.map { it.epochDay }.toSet()
            val dosesByDay = doses.groupingBy { it.epochDay }.eachCount()

            val days = entries.associate { entry ->
                entry.epochDay to CoachDay(
                    epochDay = entry.epochDay,
                    colorKey = entry.colorKey,
                    colorManual = entry.colorManual == true,
                    colorSettled = isColorSettled(entry, today.toEpochDay(), hourOfDay),
                    partScores = DayPart.entries.map { part -> entry.partColorKey(part) },
                    sportLevel = entry.sportLevel,
                    foodLevel = entry.foodLevel,
                    wentOut = entry.wentOut,
                    steps = entry.steps,
                    screenMinutes = entry.screenMinutes,
                    weightKg = entry.weightKg,
                    hasNote = entry.note.isNotBlank() || entry.title.isNotBlank(),
                    sleepMinutes = entry.sleepMinutes,
                    sleepStartMinutes = entry.sleepStartMinutes,
                    waterGlasses = entry.waterGlasses,
                    showered = entry.showered,
                    brushingsDone = entry.brushingsDone,
                    prayersDone = entry.prayersDone,
                    prayersTouched = entry.prayerMask != null,
                    jobApplications = entry.jobApplications,
                    tagSlugs = slugsByDay[entry.epochDay].orEmpty(),
                    mediaCount = mediaCounts[entry.epochDay] ?: 0,
                    dosesTaken = dosesByDay[entry.epochDay] ?: 0,
                    spentCents = spentByDay[entry.epochDay] ?: 0L,
                    earnedCents = earnedByDay[entry.epochDay] ?: 0L,
                )
            }

            return CoachSnapshot(
                today = today,
                hourOfDay = hourOfDay,
                firstName = firstName,
                birthDate = birthDate,
                days = days,
                hasActiveTreatments = treatments.any { it.active },
                hiddenCardKeys = hiddenCards.map { it.key }.toSet(),
                moneyDays = moneyDays,
            )
        }

        /**
         * La couleur de cette journee est-elle arretee ? Voir
         * [CoachDay.colorSettled].
         *
         * Quatre facons de l'etre, et pas une de plus :
         *
         * 1. la journee est passee — elle ne bougera plus, quoi qu'il y ait
         *    dedans ;
         * 2. la couleur a ete posee a la main — c'est un choix, pas un calcul,
         *    et il n'y a rien a attendre de plus ;
         * 3. les quatre moments sont remplis ;
         * 4. il ne manque **au plus qu'un** moment, et aucun moment deja passe
         *    n'a ete oublie.
         *
         * Le quatrieme cas est celui qui fait tout le travail. Il laisse
         * l'application parler le soir, quand la journee est jouee a trois
         * quarts, et il la fait taire a midi, quand elle ne l'est pas — c'est
         * exactement la difference entre « voila ta journee » et « voila ton
         * matin ».
         */
        private fun isColorSettled(
            entry: DayEntry,
            todayEpochDay: Long,
            hourOfDay: Int,
        ): Boolean {
            // Pas de couleur : il n'y a rien a proteger, et les regles savent
            // deja ne rien dire d'une journee vide.
            if (entry.colorKey == null) return true
            if (entry.epochDay < todayEpochDay) return true
            // Une journee a venir n'existe pas encore.
            if (entry.epochDay > todayEpochDay) return false
            if (entry.colorManual == true) return true

            val filled = DayPart.entries.count { entry.partColorKey(it) != null }
            if (filled == DayPart.entries.size) return true
            if (filled < DayPart.entries.size - 1) return false
            // Trois moments sur quatre, mais lesquels ? Trois moments notes en
            // sautant le matin, a quatorze heures, laisse un trou dans ce qui a
            // deja eu lieu : la couleur ne raconte pas la journee.
            return DayPart.elapsedAt(hourOfDay).all { entry.partColorKey(it) != null }
        }
    }
}
