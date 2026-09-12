package com.ismael.daybyday.coach

import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.SportLevel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Le coeur de l'algorithme : il regarde la photo du moment et dresse la liste
 * de tout ce dont il aurait quelque chose a dire. Le tri et le choix final se
 * font ensuite dans [CoachEngine].
 *
 * Deux principes tiennent tout le fichier :
 *
 * - **On ne devine rien.** Une donnee absente ne declenche jamais de relance :
 *   il faut que l'habitude existe (cochee plusieurs fois ces dernieres
 *   semaines) pour qu'on se permette de remarquer qu'elle manque. Sans ca,
 *   l'application reprocherait a quelqu'un de ne pas suivre quelque chose
 *   qu'il n'a jamais voulu suivre.
 * - **On ne reproche jamais.** Les prieres et les candidatures, par exemple,
 *   ne declenchent que des messages positifs ou tres doux.
 *
 * Chaque regle est independante et sans effet de bord : on peut donc toutes
 * les tester une par une sur machine.
 */
object CoachRules {

    private val monthNames = listOf(
        "janvier", "février", "mars", "avril", "mai", "juin",
        "juillet", "août", "septembre", "octobre", "novembre", "décembre",
    )

    /** Heures pendant lesquelles on se permet de proposer quelque chose a faire. */
    private val PROPOSAL_HOURS = 8..21

    fun candidates(
        snapshot: CoachSnapshot,
        findings: List<FactorFinding> = CoachFactors.analyse(snapshot),
    ): List<NudgeCandidate> {
        val out = mutableListOf<NudgeCandidate>()
        out += supportRules(snapshot)
        out += careRules(snapshot)
        out += upswingRules(snapshot)
        out += factorRules(snapshot, findings)
        out += habitRules(snapshot)
        out += workAndMoneyRules(snapshot)
        out += fillingRules(snapshot)
        out += smallTalkRules(snapshot)
        // Une carte masquee ne parle plus : masquer « Prieres » ou
        // « Traitements » parce que ca ne concerne pas la periode qu'on
        // traverse doit suffire a ne plus jamais en entendre parler.
        return out
            .filter { it.rule.card?.key !in snapshot.hiddenCardKeys }
            .sortedByDescending { it.score }
    }

    // --- Soutien quand c'est dur -----------------------------------------

    private fun supportRules(snapshot: CoachSnapshot): List<NudgeCandidate> {
        val out = mutableListOf<NudgeCandidate>()
        val today = snapshot.todayDay

        val darkDays = snapshot.coloredStreakDays { (it.score ?: 3) <= DayColor.RED.score }
        val blackStreak = snapshot.coloredStreak { it.color == DayColor.BLACK }

        if (darkDays.size >= 3 && darkDays.count { it.color == DayColor.BLACK } >= 2) {
            out += NudgeCandidate(CoachRule.DAYS_VERY_DARK, mapOf("n" to darkDays.size.toString()))
        }
        if (blackStreak >= 2) {
            out += NudgeCandidate(
                rule = CoachRule.BLACK_STREAK,
                values = mapOf("n" to blackStreak.toString()),
                boost = ((blackStreak - 2) * 4).coerceAtMost(12),
            )
        }
        if (today.color == DayColor.BLACK) out += NudgeCandidate(CoachRule.BLACK_DAY)
        if (today.color == DayColor.RED) out += NudgeCandidate(CoachRule.RED_DAY)
        if (today.tagSlugs.contains("anxiety")) out += NudgeCandidate(CoachRule.ANXIETY)
        if (today.tagSlugs.contains("cried")) out += NudgeCandidate(CoachRule.CRIED)

        val weekAverage = snapshot.averageOver(0, 6)
        if (snapshot.notedCount(0, 6) >= 4 && weekAverage != null && weekAverage < 1.2) {
            out += NudgeCandidate(CoachRule.HARD_WEEK)
        }
        return out
    }

    // --- Sante et soin de soi ---------------------------------------------

    private fun careRules(snapshot: CoachSnapshot): List<NudgeCandidate> {
        val out = mutableListOf<NudgeCandidate>()
        val today = snapshot.todayDay

        // Les traitements : il faut qu'il y en ait d'actifs **et** que les
        // prises soient cochees d'habitude. Une carte remplie une fois il y a
        // six mois ne doit pas declencher un rappel tous les jours.
        val dosesAreHabit = snapshot.countWhere(21) { it.dosesTaken > 0 } >= 3
        val sinceDose = snapshot.daysSince { it.dosesTaken > 0 }
        if (snapshot.hasActiveTreatments && dosesAreHabit && sinceDose != null && sinceDose >= 2) {
            out += NudgeCandidate(
                rule = CoachRule.MEDS_MISSING,
                values = mapOf("n" to sinceDose.toString()),
                boost = (sinceDose * 2).coerceAtMost(10),
            )
        }

        if (snapshot.hourOfDay in PROPOSAL_HOURS) {
            val showerIsTracked = snapshot.countWhere(21) { it.showered == true } >= 3
            val sinceShower = snapshot.daysSince { it.showered == true }
            if (showerIsTracked && sinceShower != null && sinceShower >= 3 && today.showered != true) {
                out += NudgeCandidate(
                    rule = CoachRule.SHOWER_MISSING,
                    values = mapOf("n" to sinceShower.toString()),
                    boost = sinceShower.coerceAtMost(10),
                )
            }
        }

        // Le brossage se compte sur les jours **passes** : a huit heures du
        // matin, zero brossage ne veut encore rien dire.
        val brushIsTracked = snapshot.countWhere(21) { it.brushingsDone > 0 } >= 5
        val brushGap = snapshot.streakOf(startBack = 1) { it.brushingsDone == 0 }
        if (brushIsTracked && brushGap >= 3) {
            out += NudgeCandidate(
                rule = CoachRule.BRUSHING_LOW,
                values = mapOf("n" to brushGap.toString()),
                boost = brushGap.coerceAtMost(8),
            )
        }

        val nights = (1..4).mapNotNull { back -> snapshot.dayBefore(back)?.sleepMinutes }
        if (nights.size >= 3 && nights.average() < 6 * 60) {
            out += NudgeCandidate(
                rule = CoachRule.SHORT_NIGHTS,
                values = mapOf("duree" to formatMinutes(nights.average().toInt())),
            )
        }

        // Coucher apres minuit : l'heure est comptee depuis minuit, donc une
        // valeur basse veut dire « tres tard », pas « tres tot ».
        val lateNights = snapshot.streakOf(startBack = 1) { day ->
            day.sleepStartMinutes?.let { it in 1 until LATE_BEDTIME_LIMIT } == true
        }
        if (lateNights >= 3) {
            out += NudgeCandidate(CoachRule.LATE_NIGHTS, mapOf("n" to lateNights.toString()))
        }

        val waterIsTracked = snapshot.countWhere(21) { it.waterGlasses != null } >= 5
        val dryDays = snapshot.streakOf(startBack = 1) { day ->
            day.waterGlasses?.let { it < 4 } == true
        }
        if (waterIsTracked && dryDays >= 3 && snapshot.hourOfDay in PROPOSAL_HOURS) {
            out += NudgeCandidate(CoachRule.WATER_LOW, mapOf("n" to dryDays.toString()))
        }
        return out
    }

    // --- Ce qui remonte ----------------------------------------------------

    private fun upswingRules(snapshot: CoachSnapshot): List<NudgeCandidate> {
        val out = mutableListOf<NudgeCandidate>()
        val today = snapshot.todayDay

        if (today.color == DayColor.GREEN) {
            val previousGreen = snapshot.days.values
                .filter { it.epochDay < snapshot.todayEpochDay && it.color == DayColor.GREEN }
                .maxOfOrNull { it.epochDay }
            val gap = previousGreen?.let { (snapshot.todayEpochDay - it).toInt() }
            if (gap != null && gap >= 6) {
                out += NudgeCandidate(
                    rule = CoachRule.FIRST_GREEN,
                    values = mapOf("n" to gap.toString()),
                    boost = (gap / 3).coerceAtMost(10),
                )
            }
            out += NudgeCandidate(CoachRule.GREEN_DAY)
        }

        val greenStreak = snapshot.coloredStreak { it.color == DayColor.GREEN }
        if (greenStreak >= 3) {
            out += NudgeCandidate(
                rule = CoachRule.GREEN_STREAK,
                values = mapOf("n" to greenStreak.toString()),
                boost = ((greenStreak - 3) * 3).coerceAtMost(12),
            )
        }

        val yesterdayScore = snapshot.dayBefore(1)?.score
        val todayScore = today.score
        if (todayScore != null && yesterdayScore != null && todayScore - yesterdayScore >= 2) {
            out += NudgeCandidate(CoachRule.REBOUND)
        }

        val notingStreak = snapshot.coloredStreak { it.isNoted }
        if (notingStreak in NOTING_MILESTONES) {
            out += NudgeCandidate(CoachRule.NOTING_STREAK, mapOf("n" to notingStreak.toString()))
        }

        // Les prieres : uniquement du positif. Remarquer une absence, la,
        // serait un reproche sur quelque chose qui ne regarde que lui.
        if (today.prayersTouched && today.allPrayersDone) {
            out += NudgeCandidate(CoachRule.PRAYERS_ALL)
        }
        val prayerStreak = snapshot.habitStreak { it.prayersTouched && it.allPrayersDone }
        if (prayerStreak >= 3) {
            out += NudgeCandidate(
                rule = CoachRule.PRAYER_STREAK,
                values = mapOf("n" to prayerStreak.toString()),
                boost = (prayerStreak - 3).coerceAtMost(10),
            )
        }

        val thisWeek = snapshot.averageOver(0, 6)
        val lastWeek = snapshot.averageOver(7, 13)
        if (snapshot.notedCount(0, 6) >= 4 && snapshot.notedCount(7, 13) >= 4 &&
            thisWeek != null && lastWeek != null && thisWeek - lastWeek >= 0.5
        ) {
            out += NudgeCandidate(CoachRule.BETTER_WEEK)
        }

        bestMonth(snapshot)?.let(out::add)
        return out
    }

    /** Le mois en cours depasse-t-il tous les mois complets precedents ? */
    private fun bestMonth(snapshot: CoachSnapshot): NudgeCandidate? {
        val current = YearMonth.from(snapshot.today)
        val byMonth = snapshot.days.values
            .filter { it.isNoted }
            .groupBy { YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) }
            .mapValues { (_, days) -> days.mapNotNull { it.score } }
            .filterValues { it.size >= 10 }
            .mapValues { (_, scores) -> scores.average() }

        val currentAverage = byMonth[current] ?: return null
        val previous = byMonth.filterKeys { it < current }
        if (previous.size < 2) return null
        if (previous.values.any { it >= currentAverage }) return null
        return NudgeCandidate(CoachRule.BEST_MONTH, mapOf("mois" to monthLabel(current)))
    }

    // --- Ce qui va avec les bonnes journees --------------------------------

    private fun factorRules(
        snapshot: CoachSnapshot,
        findings: List<FactorFinding>,
    ): List<NudgeCandidate> {
        val out = mutableListOf<NudgeCandidate>()

        val suggestion = findings.firstOrNull { finding ->
            finding.delta >= CoachFactors.MIN_DELTA &&
                finding.factor.invitation != null &&
                finding.presentToday != true
        }
        if (suggestion != null && snapshot.hourOfDay in PROPOSAL_HOURS) {
            out += NudgeCandidate(
                rule = CoachRule.FACTOR_SUGGESTION,
                values = mapOf(
                    "quoi" to suggestion.factor.invitation.orEmpty(),
                    "constat" to suggestion.factor.constat,
                ),
                boost = ((suggestion.delta - CoachFactors.MIN_DELTA) * 20).toInt().coerceAtMost(15),
            )
        }

        findings.firstOrNull { it.delta >= CoachFactors.MIN_DELTA && it.presentToday == true }
            ?.let { out += NudgeCandidate(CoachRule.FACTOR_TODAY, mapOf("constat" to it.factor.constat)) }

        findings.firstOrNull { it.delta <= -CoachFactors.MIN_DELTA && it.presentToday == true }
            ?.let { out += NudgeCandidate(CoachRule.FACTOR_HEAVY, mapOf("constat" to it.factor.constat)) }
        return out
    }

    // --- Habitudes du quotidien --------------------------------------------

    private fun habitRules(snapshot: CoachSnapshot): List<NudgeCandidate> {
        val out = mutableListOf<NudgeCandidate>()

        // « Personne » se deduit d'une absence d'etiquette, donc il faut
        // d'abord savoir que ces etiquettes servent : sans ce garde-fou,
        // quelqu'un qui ne coche jamais qui il voit se ferait dire tous les
        // quatre jours qu'il ne voit personne. On ne devine rien.
        val socialIsTracked = snapshot.countWhere(30) { day ->
            SOCIAL_SLUGS.any { day.tagSlugs.contains(it) }
        } >= 2
        val aloneStreak = snapshot.habitStreak { day -> SOCIAL_SLUGS.none { day.tagSlugs.contains(it) } }
        if (socialIsTracked && aloneStreak >= 4) {
            out += NudgeCandidate(
                rule = CoachRule.ALONE_STREAK,
                values = mapOf("n" to aloneStreak.toString()),
                boost = (aloneStreak - 4).coerceAtMost(10),
            )
        }

        val insideStreak = snapshot.habitStreak { it.wentOut == false }
        if (insideStreak >= 3 && snapshot.hourOfDay in PROPOSAL_HOURS) {
            out += NudgeCandidate(
                rule = CoachRule.STAYED_IN,
                values = mapOf("n" to insideStreak.toString()),
                boost = (insideStreak - 3).coerceAtMost(10),
            )
        }

        val stillStreak = snapshot.habitStreak { it.clearlyStill }
        if (stillStreak >= 4 && snapshot.hourOfDay in PROPOSAL_HOURS) {
            out += NudgeCandidate(
                rule = CoachRule.NO_MOVEMENT,
                values = mapOf("n" to stillStreak.toString()),
                boost = (stillStreak - 4).coerceAtMost(10),
            )
        }

        if (snapshot.countWhere(4) { it.tagSlugs.contains("sleep_bad") } >= 3) {
            out += NudgeCandidate(CoachRule.BAD_SLEEP)
        }

        val screenMedian = snapshot.medianOf(30) { it.screenMinutes }
        val yesterdayScreen = snapshot.dayBefore(1)?.screenMinutes
        if (screenMedian != null && yesterdayScreen != null && yesterdayScreen >= screenMedian + 120) {
            out += NudgeCandidate(
                rule = CoachRule.HIGH_SCREEN,
                values = mapOf("duree" to formatMinutes(yesterdayScreen)),
            )
        }

        val stepsMedian = snapshot.medianOf(30) { it.steps }
        val todaySteps = snapshot.todayDay.steps
        if (stepsMedian != null && stepsMedian >= 3000 && todaySteps != null &&
            todaySteps < stepsMedian * 0.35 && snapshot.hourOfDay in 17..21
        ) {
            out += NudgeCandidate(CoachRule.LOW_STEPS)
        }
        return out
    }

    // --- Travail et argent --------------------------------------------------

    private fun workAndMoneyRules(snapshot: CoachSnapshot): List<NudgeCandidate> {
        val out = mutableListOf<NudgeCandidate>()
        val today = snapshot.todayDay

        val weekApplications = snapshot.sumOver(7) { it.jobApplications }
        if (weekApplications >= 3) {
            out += NudgeCandidate(
                rule = CoachRule.JOB_EFFORT,
                values = mapOf("n" to weekApplications.toString()),
                boost = (weekApplications / 3).coerceAtMost(10),
            )
        }

        // La recherche d'emploi ne se reproche pas. Le message est doux, rare,
        // et n'apparait que si des candidatures ont deja ete notees avant.
        val searchIsUnderway = snapshot.countWhere(28) { (it.jobApplications ?: 0) > 0 } >= 2
        val sinceApplication = snapshot.daysSince { (it.jobApplications ?: 0) > 0 }
        if (searchIsUnderway && sinceApplication != null && sinceApplication >= 7) {
            out += NudgeCandidate(CoachRule.JOB_PAUSE, mapOf("n" to sinceApplication.toString()))
        }

        if (today.spentCents >= BIG_SPENDING_CENTS) out += NudgeCandidate(CoachRule.BIG_SPENDING)

        val dayOfMonth = snapshot.today.dayOfMonth
        if (dayOfMonth >= 10) {
            val thisMonth = snapshot.spentBetween(snapshot.today.withDayOfMonth(1), snapshot.today)
            val previousStart = snapshot.today.minusMonths(1).withDayOfMonth(1)
            val previousEnd = previousStart.plusDays((dayOfMonth - 1).toLong())
            val lastMonth = snapshot.spentBetween(previousStart, previousEnd)
            if (lastMonth > 0 && thisMonth <= lastMonth * 0.8) {
                out += NudgeCandidate(CoachRule.CALM_MONEY)
            }
        }
        return out
    }

    // --- Remplir l'application ----------------------------------------------

    private fun fillingRules(snapshot: CoachSnapshot): List<NudgeCandidate> {
        val out = mutableListOf<NudgeCandidate>()
        val hasHistory = snapshot.days.values.count { it.isNoted } >= 5

        if (hasHistory) {
            // Une absence, ce n'est pas un oubli a rattraper : on accueille.
            val gap = unnotedStreak(snapshot)
            if (gap >= 4) {
                out += NudgeCandidate(CoachRule.BACK_AFTER_BREAK, mapOf("n" to gap.toString()))
            } else {
                val holes = (1..5).count { back -> snapshot.dayBefore(back)?.isNoted != true }
                if (holes >= 2) {
                    out += NudgeCandidate(CoachRule.GAPS, mapOf("n" to holes.toString()))
                }
            }

            val sinceNote = snapshot.daysSince { it.hasNote }
            if (sinceNote != null && sinceNote >= 6) {
                out += NudgeCandidate(CoachRule.EMPTY_JOURNAL, mapOf("n" to sinceNote.toString()))
            }
        }

        val today = snapshot.todayDay
        if (today.isNoted && today.colorManual && today.partCount == 0) {
            out += NudgeCandidate(CoachRule.EMPTY_PARTS)
        }
        return out
    }

    // --- Petites choses -------------------------------------------------------

    private fun smallTalkRules(snapshot: CoachSnapshot): List<NudgeCandidate> {
        val out = mutableListOf<NudgeCandidate>()
        val today = snapshot.today

        val birthday = snapshot.birthDate
        if (birthday != null && birthday.dayOfMonth == today.dayOfMonth &&
            birthday.monthValue == today.monthValue
        ) {
            out += NudgeCandidate(CoachRule.BIRTHDAY)
        }

        if (today.dayOfMonth == 1) {
            out += NudgeCandidate(CoachRule.NEW_MONTH, mapOf("mois" to monthLabel(YearMonth.from(today))))
        }

        if (snapshot.todayDay.mediaCount > 0) out += NudgeCandidate(CoachRule.PHOTO_ADDED)

        val weighDays = snapshot.days.values.count {
            it.weightKg != null && YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) == YearMonth.from(today)
        }
        if (weighDays >= 4) out += NudgeCandidate(CoachRule.WEIGHT_TRACKED)

        if (today.dayOfWeek == DayOfWeek.FRIDAY && snapshot.hourOfDay >= 15) {
            out += NudgeCandidate(CoachRule.WEEKEND)
        }

        out += NudgeCandidate(CoachRule.HELLO)
        return out
    }

    // --- Petits outils ---------------------------------------------------------

    /** Paliers ou l'on souligne la serie de jours notes. */
    private val NOTING_MILESTONES = setOf(7, 14, 21, 30, 50, 75, 100, 150, 200, 300, 365, 500, 730)

    /** Les trois etiquettes qui disent qu'on a vu quelqu'un. */
    private val SOCIAL_SLUGS = setOf("girlfriend", "friends", "family")

    /** Avant 5 h du matin : on est deja passe minuit, donc couche tres tard. */
    private const val LATE_BEDTIME_LIMIT = 5 * 60

    /** 150 euros dans la journee : au-dela, ca vaut le coup d'en dire un mot. */
    private const val BIG_SPENDING_CENTS = 15_000L

    fun monthLabel(month: YearMonth): String = monthNames[month.monthValue - 1]

    fun formatMinutes(minutes: Int): String = "${minutes / 60} h ${"%02d".format(minutes % 60)}"
}

/**
 * Les journees consecutives notees qui verifient [present]. Si la journee du
 * jour n'est pas encore notee, on demarre a hier : sinon toute serie tomberait
 * a zero chaque matin.
 */
private fun CoachSnapshot.coloredStreakDays(present: (CoachDay) -> Boolean): List<CoachDay> {
    val start = if (todayDay.isNoted) 0 else 1
    val out = mutableListOf<CoachDay>()
    for (back in start until start + 400) {
        val day = days[todayEpochDay - back] ?: break
        if (!day.isNoted || !present(day)) break
        out += day
    }
    return out
}

/** Longueur de la serie ci-dessus. */
private fun CoachSnapshot.coloredStreak(present: (CoachDay) -> Boolean): Int =
    coloredStreakDays(present).size

/** Meme principe, pour une habitude cochee plutot que pour une couleur. */
private fun CoachSnapshot.habitStreak(present: (CoachDay) -> Boolean): Int {
    val start = if (present(todayDay)) 0 else 1
    return streakOf(startBack = start, present = present)
}

/** Mediane d'une mesure sur les [count] derniers jours, si elle est assez fournie. */
private fun CoachSnapshot.medianOf(count: Int, value: (CoachDay) -> Int?): Int? {
    val values = (0 until count).mapNotNull { back -> days[todayEpochDay - back]?.let(value) }.sorted()
    if (values.size < 8) return null
    return values[values.size / 2]
}

/** Depenses cumulees entre deux dates, bornes comprises. */
private fun CoachSnapshot.spentBetween(start: LocalDate, end: LocalDate): Long =
    days.values
        .filter { it.epochDay >= start.toEpochDay() && it.epochDay <= end.toEpochDay() }
        .sumOf { it.spentCents }

/**
 * Nombre de jours non notes juste avant aujourd'hui. Une journee jamais
 * ouverte n'a aucune ligne en base : elle compte comme non notee elle aussi.
 */
private fun unnotedStreak(snapshot: CoachSnapshot): Int {
    var length = 0
    for (back in 1..30) {
        if (snapshot.days[snapshot.todayEpochDay - back]?.isNoted == true) break
        length += 1
    }
    return length
}

/**
 * Journee ou l'on sait que le corps n'a pas bouge. Une journee sans
 * information ne compte pas : on ne devine rien.
 */
private val CoachDay.clearlyStill: Boolean
    get() = when {
        sportLevel != null -> sportLevel == SportLevel.NONE.key && (steps ?: 0) < 5000
        steps != null -> (steps ?: 0) < 2500
        else -> false
    }
