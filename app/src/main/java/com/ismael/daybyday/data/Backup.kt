package com.ismael.daybyday.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupSummary(val days: Int, val mediaFiles: Int)

/** Ce qu'on sait d'un fichier de sauvegarde sans l'avoir restaure. */
data class BackupInfo(
    val uri: Uri,
    val name: String,
    val exportedAt: Long,
    val days: Int,
    val mediaFiles: Int,
)

/**
 * Sauvegarde / restauration complete sous forme d'un fichier .zip choisi par
 * l'utilisateur (aucun envoi reseau : c'est un simple fichier local).
 */
object Backup {

    private const val JSON_NAME = "daybyday.json"
    private const val MEDIA_PREFIX = "media/"
    private const val FORMAT_VERSION = 9

    const val AUTO_BACKUP_NAME = "DayByDay-sauvegarde-auto.zip"

    // --- Export -----------------------------------------------------------

    suspend fun export(context: Context, repository: DayRepository, target: Uri): BackupSummary =
        withContext(Dispatchers.IO) {
            val output = context.contentResolver.openOutputStream(target)
                ?: error("Impossible d'ouvrir le fichier de destination.")
            writeZip(repository, output)
        }

    /**
     * Ecrit la sauvegarde dans un dossier choisi via SAF, en remplacant le
     * fichier precedent : une seule sauvegarde automatique occupe la place.
     */
    suspend fun exportToFolder(
        context: Context,
        repository: DayRepository,
        treeUri: Uri,
        fileName: String = AUTO_BACKUP_NAME,
    ): BackupSummary = withContext(Dispatchers.IO) {
        val folder = DocumentFile.fromTreeUri(context, treeUri)
            ?: error("Dossier de sauvegarde introuvable.")
        if (!folder.canWrite()) error("Le dossier de sauvegarde n'est plus accessible.")
        folder.findFile(fileName)?.delete()
        val file = folder.createFile("application/zip", fileName)
            ?: error("Impossible de créer le fichier de sauvegarde.")
        val output = context.contentResolver.openOutputStream(file.uri)
            ?: error("Impossible d'écrire dans le dossier choisi.")
        writeZip(repository, output)
    }

    private suspend fun writeZip(repository: DayRepository, output: OutputStream): BackupSummary {
        val days = repository.allDays()
        val mediaItems = repository.allMedia()
        val tags = repository.allTags()
        val links = repository.allDayTags()
        val money = repository.allMoney()
        val treatments = repository.allTreatments()
        val doses = repository.allDoses()
        val voiceNotes = repository.allVoiceNotes()
        val blocks = repository.allBlocks()

        val root = JSONObject()
        root.put("version", FORMAT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val daysJson = JSONArray()
        days.forEach { day ->
            daysJson.put(
                JSONObject()
                    .put("epochDay", day.epochDay)
                    .put("colorKey", day.colorKey ?: JSONObject.NULL)
                    .put("title", day.title)
                    .put("note", day.note)
                    .put("updatedAt", day.updatedAt)
                    .put("sportLevel", day.sportLevel ?: JSONObject.NULL)
                    .put("foodLevel", day.foodLevel ?: JSONObject.NULL)
                    .put("wentOut", day.wentOut ?: JSONObject.NULL)
                    .put("weightKg", day.weightKg ?: JSONObject.NULL)
                    .put("partMorning", day.partMorning ?: JSONObject.NULL)
                    .put("partAfternoon", day.partAfternoon ?: JSONObject.NULL)
                    .put("partEvening", day.partEvening ?: JSONObject.NULL)
                    .put("partNight", day.partNight ?: JSONObject.NULL)
                    .put("colorManual", day.colorManual ?: JSONObject.NULL)
                    .put("steps", day.steps ?: JSONObject.NULL)
                    .put("screenMinutes", day.screenMinutes ?: JSONObject.NULL)
                    .put("sleepStartMinutes", day.sleepStartMinutes ?: JSONObject.NULL)
                    .put("sleepEndMinutes", day.sleepEndMinutes ?: JSONObject.NULL)
                    .put("sleepFromDevice", day.sleepFromDevice ?: JSONObject.NULL)
                    .put("waterGlasses", day.waterGlasses ?: JSONObject.NULL)
                    .put("mealsNote", day.mealsNote)
                    .put("noteSpans", day.noteSpans)
                    .put("prayerMask", day.prayerMask ?: JSONObject.NULL)
                    .put("snackNote", day.snackNote)
                    .put("medicalWith", day.medicalWith)
                    .put("medicalNote", day.medicalNote)
                    .put("jobApplications", day.jobApplications ?: JSONObject.NULL)
                    .put("showered", day.showered ?: JSONObject.NULL)
                    .put("brushMask", day.brushMask ?: JSONObject.NULL)
                    .put("checkedCards", day.checkedCards)
            )
        }
        root.put("days", daysJson)

        val mediaJson = JSONArray()
        mediaItems.forEach { item ->
            mediaJson.put(
                JSONObject()
                    .put("epochDay", item.epochDay)
                    .put("relativePath", item.relativePath)
                    .put("kindKey", item.kindKey)
                    .put("addedAt", item.addedAt)
                    .put("cardKey", item.cardKey ?: JSONObject.NULL)
                    // Le placement sur la page fait partie du journal : sans
                    // lui, restaurer une sauvegarde rendrait la mise en page.
                    .put("placedX", item.placedX ?: JSONObject.NULL)
                    .put("placedY", item.placedY ?: JSONObject.NULL)
                    .put("placedWidth", item.placedWidth)
                    .put("placedHeight", item.placedHeight)
                    .put("placedRotation", item.placedRotation)
                    .put("layerKey", item.layerKey)
                    .put("shapeKey", item.shapeKey)
                    .put("stickerOutline", item.stickerOutline)
            )
        }
        root.put("media", mediaJson)

        val tagsJson = JSONArray()
        tags.forEach { tag ->
            tagsJson.put(
                JSONObject()
                    .put("id", tag.id)
                    .put("name", tag.name)
                    .put("emoji", tag.emoji)
                    .put("sortOrder", tag.sortOrder)
                    .put("category", tag.category ?: JSONObject.NULL)
            )
        }
        root.put("tags", tagsJson)

        val linksJson = JSONArray()
        links.forEach { link ->
            linksJson.put(
                JSONObject()
                    .put("epochDay", link.epochDay)
                    .put("tagId", link.tagId)
            )
        }
        root.put("dayTags", linksJson)

        val moneyJson = JSONArray()
        money.forEach { entry ->
            moneyJson.put(
                JSONObject()
                    .put("epochDay", entry.epochDay)
                    .put("amountCents", entry.amountCents)
                    .put("label", entry.label)
                    .put("categoryKey", entry.categoryKey ?: JSONObject.NULL)
                    .put("createdAt", entry.createdAt)
            )
        }
        root.put("money", moneyJson)

        val treatmentsJson = JSONArray()
        treatments.forEach { treatment ->
            treatmentsJson.put(
                JSONObject()
                    .put("id", treatment.id)
                    .put("name", treatment.name)
                    .put("dose", treatment.dose)
                    .put("timesMask", treatment.timesMask)
                    .put("active", treatment.active)
                    .put("sortOrder", treatment.sortOrder)
            )
        }
        root.put("treatments", treatmentsJson)

        val dosesJson = JSONArray()
        doses.forEach { dose ->
            dosesJson.put(
                JSONObject()
                    .put("epochDay", dose.epochDay)
                    .put("treatmentId", dose.treatmentId)
                    .put("timeKey", dose.timeKey)
                    .put("takenAt", dose.takenAt)
            )
        }
        root.put("doses", dosesJson)

        // Les vocaux comptent autant que les photos : les oublier ici, c'est
        // les perdre a la premiere restauration, sans que rien ne le signale.
        val voiceJson = JSONArray()
        voiceNotes.forEach { note ->
            voiceJson.put(
                JSONObject()
                    // L'identifiant est garde : ce sont les blocs de la page
                    // qui designent un vocal par lui. Le regenerer a la
                    // restauration detacherait chaque vocal de sa place.
                    .put("id", note.id)
                    .put("epochDay", note.epochDay)
                    .put("relativePath", note.relativePath)
                    .put("durationMs", note.durationMs)
                    .put("recordedAt", note.recordedAt)
                    // La place sur la page et la silhouette du son font partie
                    // de la page : sans elles, restaurer rendrait la mise en
                    // page et les barres de lecture.
                    .put("placedX", note.placedX ?: JSONObject.NULL)
                    .put("placedY", note.placedY ?: JSONObject.NULL)
                    .put("wide", note.wide)
                    .put("waveform", note.waveform)
            )
        }
        root.put("voiceNotes", voiceJson)

        // Les blocs : la page elle-meme. Sans eux, une restauration rendrait le
        // texte (il est dans `note`) mais plus sa mise en page — citations
        // redevenues des paragraphes, vocaux entasses a la fin.
        val blocksJson = JSONArray()
        blocks.forEach { block ->
            blocksJson.put(
                JSONObject()
                    .put("epochDay", block.epochDay)
                    .put("position", block.position)
                    .put("kindCode", block.kindCode)
                    .put("text", block.text)
                    .put("spans", block.spans)
                    .put("voiceId", block.voiceId ?: JSONObject.NULL)
                    .put("barCode", block.barCode)
                    .put("fillCode", block.fillCode)
                    .put("ruleCode", block.ruleCode)
            )
        }
        root.put("blocks", blocksJson)

        var copied = 0
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(JSON_NAME))
            zip.write(root.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            (mediaItems.map { it.relativePath } + voiceNotes.map { it.relativePath })
                .forEach { relativePath ->
                    val file = repository.media.file(relativePath)
                    if (file.exists()) {
                        zip.putNextEntry(ZipEntry(MEDIA_PREFIX + relativePath))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                        copied += 1
                    }
                }
        }
        return BackupSummary(days = days.size, mediaFiles = copied)
    }

    // --- Inspection -------------------------------------------------------

    /** Lit uniquement la fiche d'identite d'une sauvegarde, sans rien ecraser. */
    suspend fun peek(context: Context, source: Uri, name: String = ""): BackupInfo? =
        withContext(Dispatchers.IO) {
            runCatching {
                var json: JSONObject? = null
                val input = context.contentResolver.openInputStream(source)
                    ?: return@runCatching null
                ZipInputStream(input.buffered()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null && json == null) {
                        if (entry.name == JSON_NAME) {
                            json = JSONObject(zip.readBytes().toString(Charsets.UTF_8))
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
                json?.let { payload ->
                    BackupInfo(
                        uri = source,
                        name = name,
                        exportedAt = payload.optLong("exportedAt", 0L),
                        days = payload.optJSONArray("days")?.length() ?: 0,
                        mediaFiles = payload.optJSONArray("media")?.length() ?: 0,
                    )
                }
            }.getOrNull()
        }

    /**
     * Cherche la sauvegarde la plus recente dans un dossier choisi par
     * l'utilisateur. Utilise apres une reinstallation pour retrouver les
     * donnees sans rien avoir a chercher a la main.
     */
    suspend fun findLatestInFolder(context: Context, treeUri: Uri): BackupInfo? =
        withContext(Dispatchers.IO) {
            val folder = runCatching { DocumentFile.fromTreeUri(context, treeUri) }.getOrNull()
                ?: return@withContext null
            val candidates = runCatching { folder.listFiles().toList() }.getOrDefault(emptyList())
                .filter { it.isFile && it.name?.endsWith(".zip", ignoreCase = true) == true }
                .sortedByDescending { it.lastModified() }
                .take(15)
            candidates.firstNotNullOfOrNull { file ->
                peek(context, file.uri, file.name.orEmpty())
            }
        }

    // --- Import -----------------------------------------------------------

    suspend fun import(context: Context, repository: DayRepository, source: Uri): BackupSummary =
        withContext(Dispatchers.IO) {
            val staging = File(context.cacheDir, "restore_${System.currentTimeMillis()}")
            staging.mkdirs()
            var payload: JSONObject? = null
            var restoredFiles = 0

            try {
                val input = context.contentResolver.openInputStream(source)
                    ?: error("Impossible d'ouvrir la sauvegarde.")
                ZipInputStream(input.buffered()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        when {
                            entry.isDirectory -> Unit
                            name == JSON_NAME ->
                                payload = JSONObject(zip.readBytes().toString(Charsets.UTF_8))
                            name.startsWith(MEDIA_PREFIX) -> {
                                val relative = name.removePrefix(MEDIA_PREFIX)
                                val destination = safeChild(staging, relative)
                                if (destination != null) {
                                    destination.parentFile?.mkdirs()
                                    destination.outputStream().use { zip.copyTo(it) }
                                    restoredFiles += 1
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }

                val json = payload ?: error("Fichier de sauvegarde invalide (daybyday.json manquant).")

                val days = mutableListOf<DayEntry>()
                val daysJson = json.optJSONArray("days") ?: JSONArray()
                for (i in 0 until daysJson.length()) {
                    val item = daysJson.getJSONObject(i)
                    days += DayEntry(
                        epochDay = item.getLong("epochDay"),
                        colorKey = item.optIntOrNull("colorKey"),
                        title = item.optString("title", ""),
                        note = item.optString("note", ""),
                        updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                        sportLevel = item.optIntOrNull("sportLevel"),
                        foodLevel = item.optIntOrNull("foodLevel"),
                        wentOut = if (item.isNull("wentOut")) null else item.optBoolean("wentOut"),
                        weightKg = if (item.isNull("weightKg")) null else item.optDouble("weightKg"),
                        partMorning = item.optIntOrNull("partMorning"),
                        partAfternoon = item.optIntOrNull("partAfternoon"),
                        partEvening = item.optIntOrNull("partEvening"),
                        partNight = item.optIntOrNull("partNight"),
                        colorManual = if (item.isNull("colorManual")) {
                            null
                        } else {
                            item.optBoolean("colorManual")
                        },
                        steps = item.optIntOrNull("steps"),
                        screenMinutes = item.optIntOrNull("screenMinutes"),
                        sleepStartMinutes = item.optIntOrNull("sleepStartMinutes"),
                        sleepEndMinutes = item.optIntOrNull("sleepEndMinutes"),
                        sleepFromDevice = if (item.isNull("sleepFromDevice")) {
                            null
                        } else {
                            item.optBoolean("sleepFromDevice")
                        },
                        waterGlasses = item.optIntOrNull("waterGlasses"),
                        mealsNote = item.optString("mealsNote", ""),
                        noteSpans = item.optString("noteSpans", ""),
                        // Absent des sauvegardes d'avant les prieres : la
                        // journee revient alors sans rien de coche, ce qui est
                        // exactement ce qu'elle etait.
                        prayerMask = item.optIntOrNull("prayerMask"),
                        // Absents des sauvegardes plus anciennes : la journee
                        // revient alors sans, ce qui est exactement ce qu'elle
                        // etait.
                        snackNote = item.optString("snackNote", ""),
                        medicalWith = item.optString("medicalWith", ""),
                        medicalNote = item.optString("medicalNote", ""),
                        jobApplications = item.optIntOrNull("jobApplications"),
                        // Absents des sauvegardes d'avant la carte Hygiene : la
                        // journee revient alors sans, ce qui est exactement ce
                        // qu'elle etait.
                        showered = if (item.isNull("showered")) null else item.getBoolean("showered"),
                        brushMask = item.optIntOrNull("brushMask"),
                        // Absente des sauvegardes d'avant les cartes verifiees :
                        // la journee revient « pas encore verifiee ».
                        checkedCards = item.optString("checkedCards", ""),
                    )
                }

                val mediaItems = mutableListOf<MediaItem>()
                val mediaJson = json.optJSONArray("media") ?: JSONArray()
                for (i in 0 until mediaJson.length()) {
                    val item = mediaJson.getJSONObject(i)
                    mediaItems += MediaItem(
                        epochDay = item.getLong("epochDay"),
                        relativePath = item.getString("relativePath"),
                        kindKey = item.optInt("kindKey", 0),
                        addedAt = item.optLong("addedAt", System.currentTimeMillis()),
                        cardKey = if (item.isNull("cardKey")) null else item.optString("cardKey"),
                        placedX = item.optFloatOrNull("placedX"),
                        placedY = item.optFloatOrNull("placedY"),
                        placedWidth = item.optDouble("placedWidth", 0.0).toFloat(),
                        placedHeight = item.optDouble("placedHeight", 0.0).toFloat(),
                        placedRotation = item.optDouble("placedRotation", 0.0).toFloat(),
                        layerKey = item.optInt("layerKey", MediaLayer.FRONT.key),
                        shapeKey = item.optInt("shapeKey", MediaShape.RECTANGLE.key),
                        stickerOutline = item.optBoolean("stickerOutline"),
                    )
                }

                // Absents des sauvegardes d'avant les vocaux : la journee
                // revient alors sans, ce qui est exactement ce qu'elle etait.
                val voiceNotes = mutableListOf<VoiceNote>()
                val voiceJson = json.optJSONArray("voiceNotes") ?: JSONArray()
                for (i in 0 until voiceJson.length()) {
                    val item = voiceJson.getJSONObject(i)
                    voiceNotes += VoiceNote(
                        id = item.optLong("id", 0L),
                        epochDay = item.getLong("epochDay"),
                        relativePath = item.getString("relativePath"),
                        durationMs = item.optLong("durationMs", 0L),
                        recordedAt = item.optLong("recordedAt", System.currentTimeMillis()),
                        placedX = item.optFloatOrNull("placedX"),
                        placedY = item.optFloatOrNull("placedY"),
                        wide = item.optBoolean("wide", true),
                        waveform = item.optString("waveform", ""),
                    )
                }

                // Absents des sauvegardes d'avant les blocs. La page se
                // redecoupe alors toute seule a l'ouverture, a partir du texte
                // a plat : on retrouve le contenu, pas les places des vocaux.
                val blocks = mutableListOf<JournalBlock>()
                val blocksJson = json.optJSONArray("blocks") ?: JSONArray()
                for (i in 0 until blocksJson.length()) {
                    val item = blocksJson.getJSONObject(i)
                    blocks += JournalBlock(
                        epochDay = item.getLong("epochDay"),
                        position = item.optInt("position", i),
                        kindCode = item.optString("kindCode", BlockKind.TEXT.code),
                        text = item.optString("text", ""),
                        spans = item.optString("spans", ""),
                        voiceId = if (item.isNull("voiceId")) null else item.getLong("voiceId"),
                        barCode = item.optString("barCode", ""),
                        fillCode = item.optString("fillCode", ""),
                        ruleCode = item.optString("ruleCode", ""),
                    )
                }

                val tags = mutableListOf<Tag>()
                val tagsJson = json.optJSONArray("tags") ?: JSONArray()
                for (i in 0 until tagsJson.length()) {
                    val item = tagsJson.getJSONObject(i)
                    tags += Tag(
                        id = item.getLong("id"),
                        name = item.getString("name"),
                        emoji = item.optString("emoji", ""),
                        sortOrder = item.optInt("sortOrder", i),
                        category = if (item.isNull("category")) null else item.optString("category"),
                    )
                }

                val links = mutableListOf<DayTagCrossRef>()
                val linksJson = json.optJSONArray("dayTags") ?: JSONArray()
                for (i in 0 until linksJson.length()) {
                    val item = linksJson.getJSONObject(i)
                    links += DayTagCrossRef(
                        epochDay = item.getLong("epochDay"),
                        tagId = item.getLong("tagId"),
                    )
                }

                val money = mutableListOf<MoneyEntry>()
                val moneyJson = json.optJSONArray("money") ?: JSONArray()
                for (i in 0 until moneyJson.length()) {
                    val item = moneyJson.getJSONObject(i)
                    money += MoneyEntry(
                        epochDay = item.getLong("epochDay"),
                        amountCents = item.getLong("amountCents"),
                        label = item.optString("label", ""),
                        categoryKey = if (item.isNull("categoryKey")) {
                            null
                        } else {
                            item.optString("categoryKey")
                        },
                        createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                    )
                }

                val treatments = mutableListOf<Treatment>()
                val treatmentsJson = json.optJSONArray("treatments") ?: JSONArray()
                for (i in 0 until treatmentsJson.length()) {
                    val item = treatmentsJson.getJSONObject(i)
                    treatments += Treatment(
                        // L'identifiant est conserve : les prises y renvoient.
                        id = item.optLong("id", 0L),
                        name = item.optString("name", ""),
                        dose = item.optString("dose", ""),
                        timesMask = item.optInt("timesMask", DoseTime.MORNING.bit),
                        active = item.optBoolean("active", true),
                        sortOrder = item.optInt("sortOrder", 0),
                    )
                }

                val doses = mutableListOf<DoseTaken>()
                val dosesJson = json.optJSONArray("doses") ?: JSONArray()
                for (i in 0 until dosesJson.length()) {
                    val item = dosesJson.getJSONObject(i)
                    doses += DoseTaken(
                        epochDay = item.getLong("epochDay"),
                        treatmentId = item.getLong("treatmentId"),
                        timeKey = item.getInt("timeKey"),
                        takenAt = item.optLong("takenAt", System.currentTimeMillis()),
                    )
                }

                repository.media.deleteAll()
                staging.walkTopDown().filter { it.isFile }.forEach { file ->
                    val relative = file.relativeTo(staging).path.replace(File.separatorChar, '/')
                    file.inputStream().use { repository.media.writeFrom(relative, it) }
                }
                repository.replaceAll(
                    days, mediaItems, tags, links, money, treatments, doses, voiceNotes,
                    blocks,
                )

                BackupSummary(days = days.size, mediaFiles = restoredFiles)
            } finally {
                staging.deleteRecursively()
            }
        }

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (isNull(key)) null else optInt(key)

    private fun JSONObject.optFloatOrNull(key: String): Float? =
        if (isNull(key)) null else optDouble(key).toFloat()

    /** Empeche un chemin malveillant du type ../../ de sortir du dossier cible. */
    private fun safeChild(root: File, relative: String): File? {
        val candidate = File(root, relative).canonicalFile
        return if (candidate.path.startsWith(root.canonicalFile.path + File.separator)) candidate else null
    }

    // --- Export texte -----------------------------------------------------

    /**
     * Export texte d'une annee : pratique pour preparer la video annuelle de
     * fin d'annee (titres + notes, jour par jour, avec les statistiques).
     */
    suspend fun exportYearText(
        context: Context,
        repository: DayRepository,
        year: Int,
        target: Uri,
    ): Int = withContext(Dispatchers.IO) {
        val start = LocalDate.of(year, 1, 1)
        val end = LocalDate.of(year, 12, 31)
        val days = repository.allDays()
            .filter { it.epochDay in start.toEpochDay()..end.toEpochDay() }
            .sortedBy { it.epochDay }
        val mediaByDay = repository.allMedia().groupBy { it.epochDay }
        val tagsById = repository.allTags().associateBy { it.id }
        val tagsByDay = repository.allDayTags().groupBy({ it.epochDay }, { it.tagId })

        val dayFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE)
        val builder = StringBuilder()
        builder.appendLine("# DayByDay — $year")
        builder.appendLine()

        val yearSummary = Stats.summarize(year.toString(), days, if (start.isLeapYear) 366 else 365)
        builder.appendLine("Jours notés : ${yearSummary.filledDays} / ${yearSummary.totalDays}")
        yearSummary.average?.let { builder.appendLine("Moyenne de l'année : ${"%.2f".format(it)} / 3") }
        DayColor.entries.forEach { color ->
            builder.appendLine("- ${color.label} : ${yearSummary.countOf(color)} jour(s)")
        }
        builder.appendLine()

        (1..12).forEach { monthValue ->
            val monthDays = days.filter { LocalDate.ofEpochDay(it.epochDay).monthValue == monthValue }
            val monthName = LocalDate.of(year, monthValue, 1)
                .month.getDisplayName(TextStyle.FULL, Locale.FRANCE)
                .replaceFirstChar { it.uppercase() }
            val summary = Stats.summarize(
                monthName,
                monthDays,
                LocalDate.of(year, monthValue, 1).lengthOfMonth(),
            )
            builder.appendLine("## $monthName")
            builder.appendLine(
                summary.average?.let { "Moyenne : ${"%.2f".format(it)} / 3 — ${summary.filledDays} jour(s) noté(s)" }
                    ?: "Aucun jour noté."
            )
            builder.appendLine()
            monthDays.forEach { entry ->
                val date = LocalDate.ofEpochDay(entry.epochDay)
                val label = entry.color?.label ?: "Sans couleur"
                builder.appendLine("### ${date.format(dayFormat)} — $label")
                if (entry.title.isNotBlank()) builder.appendLine("**${entry.title}**")
                if (entry.note.isNotBlank()) builder.appendLine(entry.note)

                val moments = DayPart.entries.mapNotNull { part ->
                    entry.partColor(part)?.let { "${part.label} : ${it.label}" }
                }
                if (moments.isNotEmpty()) builder.appendLine(moments.joinToString(" · "))

                val details = buildList {
                    entry.sport?.let { add("Sport : ${it.label}") }
                    entry.food?.let { add("Alimentation : ${it.label}") }
                    entry.wentOut?.let { add(if (it) "Sorti" else "Pas sorti") }
                    entry.weightKg?.let { add("Poids : ${"%.1f".format(it)} kg") }
                    entry.steps?.let { add("$it pas") }
                }
                if (details.isNotEmpty()) builder.appendLine(details.joinToString(" · "))

                val dayTags = tagsByDay[entry.epochDay].orEmpty().mapNotNull { tagsById[it]?.display }
                if (dayTags.isNotEmpty()) builder.appendLine(dayTags.joinToString(" "))

                val mediaCount = mediaByDay[entry.epochDay]?.size ?: 0
                if (mediaCount > 0) builder.appendLine("_($mediaCount média(s) dans l'application)_")
                builder.appendLine()
            }
        }

        val output = context.contentResolver.openOutputStream(target)
            ?: error("Impossible d'ouvrir le fichier de destination.")
        output.bufferedWriter(Charsets.UTF_8).use { it.write(builder.toString()) }
        days.size
    }
}
