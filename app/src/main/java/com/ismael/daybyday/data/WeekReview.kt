package com.ismael.daybyday.data

import java.time.LocalDate

/**
 * Le bilan d'une semaine.
 *
 * Deux partis pris, et ils tiennent l'ensemble.
 *
 * **On raconte des faits, pas des corrélations.** Sur sept jours, comparer
 * « les jours où tu as bougé » aux autres ne veut rien dire : deux journées
 * d'un côté, trois de l'autre, et le hasard décide. Les rapprochements de ce
 * genre restent dans « Ce qui va avec tes bonnes journées », qui travaille sur
 * des mois. Ici on compte : tu as bougé trois jours, tu es sorti deux fois.
 *
 * **On compare a la semaine d'avant, et a rien d'autre.** Pas de moyenne
 * ideale, pas d'objectif : la seule question interessante est « et par rapport
 * a la semaine derniere ? ».
 */
data class WeekReview(
    val start: LocalDate,
    val end: LocalDate,
    val weekNumber: Int,
    /** Les sept couleurs, du lundi au dimanche. `null` = journee non notee. */
    val colors: List<DayColor?>,
    val summary: PeriodSummary,
    val previous: PeriodSummary,
    val brightest: DayEntry?,
    val hardest: DayEntry?,
    val movedDays: Int,
    val wentOutDays: Int,
    val ateWellDays: Int,
    val writtenDays: Int,
    val photos: Int,
    val sleepAverageMinutes: Int?,
    val previousSleepAverageMinutes: Int?,
    val stepsAverage: Int?,
    val previousStepsAverage: Int?,
    val screenAverageMinutes: Int?,
    val previousScreenAverageMinutes: Int?,
    val topTags: List<Pair<Tag, Int>>,
    val money: MoneySummary,
) {
    val hasData: Boolean get() = summary.hasData || writtenDays > 0 || photos > 0

    /** Ecart de moyenne avec la semaine precedente, quand les deux existent. */
    val delta: Double?
        get() {
            val now = summary.average ?: return null
            val before = previous.average ?: return null
            return now - before
        }
}

/** Calcule le bilan d'une semaine a partir des donnees deja chargees. */
object WeekReviewBuilder {

    /** Le lundi de la semaine qui contient [date]. */
    fun mondayOf(date: LocalDate): LocalDate = date.minusDays(
        ((date.dayOfWeek.value + 6) % 7).toLong()
    )

    fun build(
        monday: LocalDate,
        days: Map<Long, DayEntry>,
        mediaCounts: Map<Long, Int>,
        tags: List<Tag>,
        links: List<DayTagCrossRef>,
        money: List<MoneyEntry>,
    ): WeekReview {
        val sunday = monday.plusDays(6)
        val week = (0..6).map { days[monday.plusDays(it.toLong()).toEpochDay()] }
        val present = week.filterNotNull()

        val previousMonday = monday.minusWeeks(1)
        val previousWeek = (0..6).mapNotNull { days[previousMonday.plusDays(it.toLong()).toEpochDay()] }

        val weekEpochDays = (0..6).map { monday.plusDays(it.toLong()).toEpochDay() }.toSet()
        val tagCounts = links
            .filter { it.epochDay in weekEpochDays }
            .groupingBy { it.tagId }
            .eachCount()
        val tagsById = tags.associateBy { it.id }

        return WeekReview(
            start = monday,
            end = sunday,
            weekNumber = Stats.weekNumber(monday),
            colors = week.map { it?.color },
            summary = Stats.summarize("Semaine", present, totalDays = 7),
            previous = Stats.summarize("Semaine précédente", previousWeek, totalDays = 7),
            brightest = present.filter { it.colorKey != null }.maxByOrNull { it.color?.score ?: -1 },
            hardest = present.filter { it.colorKey != null }.minByOrNull { it.color?.score ?: 99 },
            movedDays = present.count { (it.sportLevel ?: 0) >= SportLevel.LIGHT.key },
            wentOutDays = present.count { it.wentOut == true },
            ateWellDays = present.count { it.foodLevel == FoodLevel.GOOD.key },
            writtenDays = present.count { it.title.isNotBlank() || it.note.isNotBlank() },
            photos = weekEpochDays.sumOf { mediaCounts[it] ?: 0 },
            sleepAverageMinutes = averageOf(present.mapNotNull { it.sleepMinutes }),
            previousSleepAverageMinutes = averageOf(previousWeek.mapNotNull { it.sleepMinutes }),
            stepsAverage = averageOf(present.mapNotNull { it.steps }),
            previousStepsAverage = averageOf(previousWeek.mapNotNull { it.steps }),
            screenAverageMinutes = averageOf(present.mapNotNull { it.screenMinutes }),
            previousScreenAverageMinutes = averageOf(previousWeek.mapNotNull { it.screenMinutes }),
            topTags = tagCounts.entries
                .sortedByDescending { it.value }
                .mapNotNull { entry -> tagsById[entry.key]?.let { it to entry.value } }
                .take(4),
            money = Stats.summarizeMoney(money.filter { it.epochDay in weekEpochDays }),
        )
    }

    private fun averageOf(values: List<Int>): Int? =
        if (values.isEmpty()) null else values.sum() / values.size
}
