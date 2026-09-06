package com.ismael.daybyday.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class DayRepository(context: Context) {

    private val dao = AppDatabase.get(context).dayDao()
    val media = MediaFiles(context)

    // --- Traitements ------------------------------------------------------

    fun observeTreatments(): Flow<List<Treatment>> = dao.observeTreatments()

    fun observeDosesForDay(date: LocalDate): Flow<List<DoseTaken>> =
        dao.observeDosesForDay(date.toEpochDay())

    fun observeDosesBetween(start: LocalDate, end: LocalDate): Flow<List<DoseTaken>> =
        dao.observeDosesBetween(start.toEpochDay(), end.toEpochDay())

    suspend fun allTreatments(): List<Treatment> = dao.allTreatments()

    suspend fun allDoses(): List<DoseTaken> = dao.allDoses()

    suspend fun saveTreatment(treatment: Treatment) = dao.upsertTreatment(treatment)

    /** Supprimer un traitement enleve aussi l'historique de ses prises. */
    suspend fun deleteTreatment(treatment: Treatment) {
        dao.deleteDosesOfTreatment(treatment.id)
        dao.deleteTreatment(treatment.id)
    }

    suspend fun setDoseTaken(date: LocalDate, treatmentId: Long, time: DoseTime, taken: Boolean) {
        val epochDay = date.toEpochDay()
        if (taken) {
            dao.markDose(DoseTaken(epochDay, treatmentId, time.key))
        } else {
            dao.unmarkDose(epochDay, treatmentId, time.key)
        }
    }

    // --- Journees ---------------------------------------------------------

    fun observeDay(date: LocalDate): Flow<DayEntry?> = dao.observeDay(date.toEpochDay())

    fun observeDaysBetween(start: LocalDate, end: LocalDate): Flow<Map<Long, DayEntry>> =
        dao.observeRange(start.toEpochDay(), end.toEpochDay())
            .map { list -> list.associateBy { it.epochDay } }

    fun observeAllDays(): Flow<List<DayEntry>> = dao.observeAll()

    fun search(text: String): Flow<List<DayEntry>> = dao.search(text)

    fun observeWeights(): Flow<List<WeightPoint>> = dao.observeWeights()

    suspend fun allDays(): List<DayEntry> = dao.allDays()

    suspend fun dayOnce(date: LocalDate): DayEntry? = dao.dayOnce(date.toEpochDay())

    /**
     * Le meme jour, les annees d'avant.
     *
     * On remonte annee par annee et on s'arrete a la premiere qui a quelque
     * chose a montrer — une couleur, un texte ou une photo. C'est la regle qui
     * compte : une carte « il y a un an » qui s'affiche pour dire qu'il ne
     * s'est rien passe il y a un an n'apprend rien et encombre l'ecran. Si
     * l'annee derniere est vide mais pas celle d'avant, c'est celle d'avant
     * qu'on montre.
     *
     * `minusYears` gere le 29 fevrier tout seul : il retombe sur le 28.
     */
    suspend fun memoryFor(date: LocalDate, maxYearsBack: Int = 12): Memory? {
        for (yearsAgo in 1..maxYearsBack) {
            val then = date.minusYears(yearsAgo.toLong())
            val entry = dao.dayOnce(then.toEpochDay())
            val media = dao.mediaForDay(then.toEpochDay())
            val worthShowing = entry != null &&
                (entry.colorKey != null || entry.title.isNotBlank() || entry.note.isNotBlank())
            if (worthShowing || media.isNotEmpty()) {
                return Memory(
                    date = then,
                    yearsAgo = yearsAgo,
                    entry = entry ?: DayEntry(epochDay = then.toEpochDay()),
                    photo = media.firstOrNull { it.kind == MediaKind.PHOTO } ?: media.firstOrNull(),
                    mediaCount = media.size,
                )
            }
        }
        return null
    }

    /**
     * Le bilan d'une semaine, charge en une fois.
     *
     * On lit **deux** semaines : celle qu'on raconte et celle d'avant, parce
     * que la seule comparaison qui ait un sens ici est avec la precedente.
     */
    suspend fun weekReview(monday: LocalDate): WeekReview {
        val from = monday.minusWeeks(1)
        val to = monday.plusDays(6)
        val days = dao.rangeOnce(from.toEpochDay(), to.toEpochDay()).associateBy { it.epochDay }
        val mediaCounts = dao.mediaForRange(monday.toEpochDay(), to.toEpochDay())
            .groupingBy { it.epochDay }
            .eachCount()
        return WeekReviewBuilder.build(
            monday = monday,
            days = days,
            mediaCounts = mediaCounts,
            tags = dao.allTags(),
            links = dao.allDayTags(),
            money = dao.allMoney(),
        )
    }

    /**
     * Enregistre le contenu d'une journee. Une journee totalement vide, sans
     * etiquette ni media, est supprimee pour ne pas polluer les statistiques.
     */
    suspend fun saveDay(entry: DayEntry) {
        val epochDay = entry.epochDay
        val hasExtras = dao.tagCountForDay(epochDay) > 0 || dao.mediaForDay(epochDay).isNotEmpty()
        if (entry.isEmpty && !hasExtras) {
            dao.deleteDay(epochDay)
        } else {
            dao.upsertDay(entry.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    /** Cree la ligne du jour si elle n'existe pas encore (media, etiquette...). */
    private suspend fun ensureDayExists(epochDay: Long) {
        if (dao.dayOnce(epochDay) == null) dao.upsertDay(DayEntry(epochDay = epochDay))
    }

    // --- Etiquettes -------------------------------------------------------

    fun observeTags(): Flow<List<Tag>> = dao.observeTags()

    fun observeTagsForDay(date: LocalDate): Flow<List<Tag>> =
        dao.observeTagsForDay(date.toEpochDay())

    fun observeAllDayTags(): Flow<List<DayTagCrossRef>> = dao.observeAllDayTags()

    suspend fun allTags(): List<Tag> = dao.allTags()

    suspend fun allDayTags(): List<DayTagCrossRef> = dao.allDayTags()

    suspend fun createTag(name: String, emoji: String, category: TagCategory) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val order = (dao.allTags().maxOfOrNull { it.sortOrder } ?: 0) + 1
        dao.insertTag(
            Tag(
                name = trimmed,
                emoji = emoji.trim(),
                sortOrder = order,
                category = category.key,
            )
        )
    }

    suspend fun deleteTag(tag: Tag) {
        dao.deleteTagLinks(tag.id)
        dao.deleteTag(tag.id)
    }

    suspend fun toggleTag(date: LocalDate, tag: Tag, selected: Boolean) {
        val epochDay = date.toEpochDay()
        if (selected) {
            ensureDayExists(epochDay)
            dao.linkTag(DayTagCrossRef(epochDay = epochDay, tagId = tag.id))
        } else {
            dao.unlinkTag(epochDay, tag.id)
            cleanUpIfEmpty(epochDay)
        }
    }

    private suspend fun cleanUpIfEmpty(epochDay: Long) {
        val entry = dao.dayOnce(epochDay) ?: return
        val hasExtras = dao.tagCountForDay(epochDay) > 0 || dao.mediaForDay(epochDay).isNotEmpty()
        if (entry.isEmpty && !hasExtras) dao.deleteDay(epochDay)
    }

    // --- Medias -----------------------------------------------------------

    fun observeMediaForDay(date: LocalDate): Flow<List<MediaItem>> =
        dao.observeMediaForDay(date.toEpochDay())

    /** Le nombre de photos de chaque journee, pour la recherche. */
    fun observeAllMediaCounts(): Flow<Map<Long, Int>> =
        dao.observeAllMediaCounts().map { list -> list.associate { it.epochDay to it.count } }

    fun observeMediaCounts(start: LocalDate, end: LocalDate): Flow<Map<Long, Int>> =
        dao.observeMediaCounts(start.toEpochDay(), end.toEpochDay())
            .map { list -> list.associate { it.epochDay to it.count } }

    suspend fun allMedia(): List<MediaItem> = dao.allMedia()

    suspend fun addMedia(date: LocalDate, uri: Uri, card: DayCard? = null): Boolean {
        val item = media.importFrom(uri, date.toEpochDay()) ?: return false
        dao.insertMedia(item.copy(cardKey = card?.key))
        ensureDayExists(date.toEpochDay())
        return true
    }

    /** Enregistre la nouvelle place d'une photo sur la page du journal. */
    suspend fun updateMedia(item: MediaItem) {
        dao.updateMedia(item)
    }

    suspend fun deleteMedia(item: MediaItem) {
        dao.deleteMedia(item.id)
        media.delete(item.relativePath)
        cleanUpIfEmpty(item.epochDay)
    }

    // --- Argent -----------------------------------------------------------

    fun observeMoneyBetween(start: LocalDate, end: LocalDate): Flow<List<MoneyEntry>> =
        dao.observeMoneyBetween(start.toEpochDay(), end.toEpochDay())

    fun observeAllMoney(): Flow<List<MoneyEntry>> = dao.observeAllMoney()

    fun observeMoneyBalance(): Flow<Long> = dao.observeMoneyBalance()

    suspend fun allMoney(): List<MoneyEntry> = dao.allMoney()

    /** Ce que contient la base, pour la fiche technique des reglages. */
    suspend fun contents(): DatabaseContents = DatabaseContents(
        days = dao.countDays(),
        moneyEntries = dao.countMoney(),
        mediaFiles = dao.countMedia(),
        taggedDays = dao.countDayTags(),
        mediaBytes = media.totalBytes(),
    )

    suspend fun saveMoney(entry: MoneyEntry) {
        dao.upsertMoney(entry)
    }

    suspend fun deleteMoney(entry: MoneyEntry) {
        dao.deleteMoney(entry.id)
    }

    // --- Sauvegarde / remise a zero ---------------------------------------

    suspend fun clearEverything() {
        dao.deleteAllMedia()
        dao.deleteAllDayTags()
        dao.deleteAllMoney()
        dao.deleteAllDoses()
        dao.deleteAllTreatments()
        dao.deleteAllDays()
        media.deleteAll()
    }

    suspend fun replaceAll(
        days: List<DayEntry>,
        mediaItems: List<MediaItem>,
        tags: List<Tag>,
        links: List<DayTagCrossRef>,
        money: List<MoneyEntry>,
        treatments: List<Treatment>,
        doses: List<DoseTaken>,
    ) {
        dao.deleteAllMedia()
        dao.deleteAllDayTags()
        dao.deleteAllMoney()
        dao.deleteAllDoses()
        dao.deleteAllTreatments()
        dao.deleteAllDays()
        if (tags.isNotEmpty()) {
            dao.deleteAllTags()
            tags.forEach { dao.insertTag(it) }
        }
        days.forEach { dao.upsertDay(it) }
        mediaItems.forEach { dao.insertMedia(it.copy(id = 0)) }
        links.forEach { dao.linkTag(it) }
        money.forEach { dao.upsertMoney(it.copy(id = 0)) }
        // Les identifiants des traitements sont conserves tels quels : les
        // prises deja cochees y renvoient.
        treatments.forEach { dao.upsertTreatment(it) }
        doses.forEach { dao.markDose(it) }
    }
}

/**
 * Un souvenir : la meme date, une ou plusieurs annees plus tot, quand il y a
 * quelque chose a en dire. Voir [DayRepository.memoryFor].
 */
data class Memory(
    val date: LocalDate,
    val yearsAgo: Int,
    val entry: DayEntry,
    val photo: MediaItem?,
    val mediaCount: Int,
)
