package com.ismael.daybyday

import com.ismael.daybyday.coach.CoachEngine
import com.ismael.daybyday.coach.CoachFactor
import com.ismael.daybyday.coach.CoachFactors
import com.ismael.daybyday.coach.CoachMessages
import com.ismael.daybyday.coach.CoachRule
import com.ismael.daybyday.coach.CoachRules
import com.ismael.daybyday.coach.CoachSnapshot
import com.ismael.daybyday.coach.NudgeCandidate
import com.ismael.daybyday.coach.NudgeSurface
import com.ismael.daybyday.coach.NudgeTone
import com.ismael.daybyday.coach.TemporaryCoachMemory
import com.ismael.daybyday.data.Brushing
import com.ismael.daybyday.data.DayCard
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.DayTagCrossRef
import com.ismael.daybyday.data.DoseTaken
import com.ismael.daybyday.data.MoneyEntry
import com.ismael.daybyday.data.Prayer
import com.ismael.daybyday.data.SportLevel
import com.ismael.daybyday.data.Tag
import com.ismael.daybyday.data.TagCatalog
import com.ismael.daybyday.data.Treatment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * L'algorithme est du Kotlin pur : il se teste ici, sur machine, sans
 * emulateur. C'est important, parce que c'est la partie qui parle a Ismael les
 * jours difficiles : elle n'a pas le droit de dire n'importe quoi.
 */
class CoachTest {

    private val today = LocalDate.of(2026, 3, 10) // un mardi

    // --- Outils de construction ------------------------------------------

    private val catalogTags: List<Tag> = TagCatalog.tags.mapIndexed { index, builtin ->
        Tag(
            id = (index + 1).toLong(),
            name = builtin.name,
            emoji = builtin.emoji,
            sortOrder = index,
            category = builtin.category.key,
            slug = builtin.slug,
        )
    }

    private fun tagId(slug: String): Long = catalogTags.first { it.slug == slug }.id

    private fun day(
        back: Int,
        color: DayColor? = null,
        wentOut: Boolean? = null,
        sport: SportLevel? = null,
        note: String = "",
        steps: Int? = null,
        showered: Boolean? = null,
        brushings: Int? = null,
        prayers: Int? = null,
        water: Int? = null,
        applications: Int? = null,
        sleepFrom: Int? = null,
        sleepTo: Int? = null,
        manual: Boolean = false,
        parts: List<DayColor> = listOf(DayColor.ORANGE, DayColor.ORANGE),
    ) = DayEntry(
        epochDay = today.toEpochDay() - back,
        colorKey = color?.key,
        colorManual = manual,
        note = note,
        wentOut = wentOut,
        sportLevel = sport?.key,
        steps = steps,
        showered = showered,
        brushMask = brushings?.let { count ->
            Brushing.entries.take(count).fold(0) { mask, b -> mask or b.bit }
        },
        prayerMask = prayers?.let { count ->
            Prayer.entries.take(count).fold(0) { mask, p -> mask or p.bit }
        },
        waterGlasses = water,
        jobApplications = applications,
        sleepStartMinutes = sleepFrom,
        sleepEndMinutes = sleepTo,
        partMorning = parts.getOrNull(0)?.key,
        partAfternoon = parts.getOrNull(1)?.key,
    )

    private fun link(back: Int, slug: String) =
        DayTagCrossRef(epochDay = today.toEpochDay() - back, tagId = tagId(slug))

    private fun dose(back: Int) =
        DoseTaken(epochDay = today.toEpochDay() - back, treatmentId = 1L, timeKey = 0)

    private fun snapshotOf(
        entries: List<DayEntry>,
        links: List<DayTagCrossRef> = emptyList(),
        money: List<MoneyEntry> = emptyList(),
        treatments: List<Treatment> = emptyList(),
        doses: List<DoseTaken> = emptyList(),
        hidden: Set<DayCard> = emptySet(),
        hour: Int = 10,
        birthDate: LocalDate? = null,
    ) = CoachSnapshot.build(
        today = today,
        hourOfDay = hour,
        firstName = "Ismael",
        birthDate = birthDate,
        entries = entries,
        tags = catalogTags,
        links = links,
        money = money,
        treatments = treatments,
        doses = doses,
        hiddenCards = hidden,
    )

    private fun rulesOf(snapshot: CoachSnapshot): List<CoachRule> =
        CoachRules.candidates(snapshot).map { it.rule }

    // --- Le catalogue de phrases ------------------------------------------

    @Test
    fun `le catalogue couvre toutes les situations`() {
        CoachRule.entries.forEach { rule ->
            val variants = CoachMessages.variantsFor(rule)
            assertTrue("Aucune phrase pour ${rule.slug}", variants.size >= 3)
            variants.forEach { assertTrue("Phrase vide dans ${rule.slug}", it.isNotBlank()) }
        }
        assertTrue(
            "Il faut au moins 200 phrases, il y en a ${CoachMessages.phraseCount}",
            CoachMessages.phraseCount >= 200,
        )
    }

    @Test
    fun `toutes les phrases se completent entierement`() {
        val placeholder = Regex("\\{([a-z]+)\\}")
        CoachRule.entries.forEach { rule ->
            CoachMessages.variantsFor(rule).forEachIndexed { index, phrase ->
                val values = placeholder.findAll(phrase).associate { it.groupValues[1] to "X" }
                val nudge = CoachMessages.render(NudgeCandidate(rule, values), index, "Ismael")
                assertNotNull(nudge)
                assertFalse("Il reste un trou dans ${rule.slug} : ${nudge!!.text}", nudge.text.contains("{"))
            }
        }
    }

    @Test
    fun `les slugs sont uniques`() {
        val slugs = CoachRule.entries.map { it.slug }
        assertEquals(slugs.size, slugs.toSet().size)
    }

    @Test
    fun `la phrase change d'une fois sur l'autre`() {
        val candidate = NudgeCandidate(CoachRule.BLACK_DAY)
        assertTrue(
            CoachMessages.render(candidate, 0, "Ismael")!!.text !=
                CoachMessages.render(candidate, 1, "Ismael")!!.text,
        )
    }

    @Test
    fun `un prenom vide ne laisse pas de trou`() {
        val nudge = CoachMessages.render(NudgeCandidate(CoachRule.BIRTHDAY), 0, "  ")!!
        assertFalse(nudge.text.contains("{"))
        assertTrue(nudge.text.contains("toi"))
    }

    @Test
    fun `aucune phrase n'annonce une serie brisee`() {
        // Une serie ne sert qu'a souligner ce qui va bien. Annoncer qu'elle est
        // cassee punirait exactement les journees qu'il ne faut pas punir.
        val forbidden = listOf("série est cassée", "tu as cassé", "série perdue", "série brisée")
        CoachRule.entries.forEach { rule ->
            CoachMessages.variantsFor(rule).forEach { phrase ->
                forbidden.forEach { assertFalse("${rule.slug} : $phrase", phrase.contains(it)) }
            }
        }
    }

    // --- Les relances de soin ---------------------------------------------

    @Test
    fun `la douche oubliee depuis plusieurs jours declenche une relance`() {
        val entries = (0..20).map { day(it, DayColor.ORANGE, showered = it >= 4) }

        val candidate = CoachRules.candidates(snapshotOf(entries))
            .firstOrNull { it.rule == CoachRule.SHOWER_MISSING }

        assertNotNull(candidate)
        assertEquals("4", candidate!!.values["n"])
    }

    @Test
    fun `pas de relance sur une habitude qu'il ne suit pas`() {
        val entries = (0..20).map { day(it, DayColor.ORANGE) }
        assertFalse(rulesOf(snapshotOf(entries)).contains(CoachRule.SHOWER_MISSING))
    }

    @Test
    fun `une carte masquee ne dit plus rien`() {
        val entries = (0..20).map { day(it, DayColor.ORANGE, showered = it >= 4) }
        assertTrue(rulesOf(snapshotOf(entries)).contains(CoachRule.SHOWER_MISSING))

        val silent = rulesOf(snapshotOf(entries, hidden = setOf(DayCard.HYGIENE)))
        assertFalse(silent.contains(CoachRule.SHOWER_MISSING))
        assertFalse(silent.contains(CoachRule.BRUSHING_LOW))
    }

    @Test
    fun `pas de relance au milieu de la nuit`() {
        val entries = (0..20).map { day(it, DayColor.ORANGE, showered = it >= 4) }
        assertFalse(rulesOf(snapshotOf(entries, hour = 3)).contains(CoachRule.SHOWER_MISSING))
    }

    @Test
    fun `le brossage ne se compte que sur les jours passes`() {
        // Brosse tous les jours sauf les quatre derniers. Aujourd'hui ne compte
        // pas — a huit heures du matin, zero brossage ne veut rien dire — donc
        // la serie des jours passes en vaut trois.
        val entries = (0..20).map { day(it, DayColor.ORANGE, brushings = if (it <= 3) 0 else 2) }
        val candidate = CoachRules.candidates(snapshotOf(entries))
            .firstOrNull { it.rule == CoachRule.BRUSHING_LOW }
        assertNotNull(candidate)
        assertEquals("3", candidate!!.values["n"])
    }

    @Test
    fun `on ne parle de solitude que si les etiquettes sociales servent`() {
        // Personne de coche nulle part : c'est peut-etre qu'il ne s'en sert
        // pas, pas qu'il ne voit personne. On se tait.
        val never = (0..20).map { day(it, DayColor.ORANGE) }
        assertFalse(rulesOf(snapshotOf(never)).contains(CoachRule.ALONE_STREAK))

        // La, les etiquettes servent, et il y a un vrai trou depuis six jours.
        val links = listOf(6, 8, 11).map { link(it, "friends") }
        val tracked = CoachRules.candidates(snapshotOf(never, links))
            .firstOrNull { it.rule == CoachRule.ALONE_STREAK }
        assertNotNull(tracked)
        assertEquals("6", tracked!!.values["n"])
    }

    @Test
    fun `le traitement n'est rappele que s'il y en a un a prendre`() {
        val entries = (0..20).map { day(it, DayColor.ORANGE) }
        val doses = (2..13).map { dose(it) }
        val treatment = Treatment(id = 1L, name = "Truc", active = true)

        assertTrue(rulesOf(snapshotOf(entries, treatments = listOf(treatment), doses = doses))
            .contains(CoachRule.MEDS_MISSING))

        // Plus de traitement actif : plus rien a rappeler.
        assertFalse(rulesOf(snapshotOf(entries, treatments = emptyList(), doses = doses))
            .contains(CoachRule.MEDS_MISSING))

        // Un traitement actif mais jamais coche : on ne reclame pas.
        assertFalse(rulesOf(snapshotOf(entries, treatments = listOf(treatment)))
            .contains(CoachRule.MEDS_MISSING))
    }

    @Test
    fun `des nuits courtes sont remarquees`() {
        // Couche a 2 h, leve a 6 h : quatre heures.
        val entries = (0..10).map { day(it, DayColor.ORANGE, sleepFrom = 2 * 60, sleepTo = 6 * 60) }
        assertTrue(rulesOf(snapshotOf(entries)).contains(CoachRule.SHORT_NIGHTS))

        val rested = (0..10).map { day(it, DayColor.ORANGE, sleepFrom = 23 * 60, sleepTo = 8 * 60) }
        assertFalse(rulesOf(snapshotOf(rested)).contains(CoachRule.SHORT_NIGHTS))
    }

    @Test
    fun `peu d'eau plusieurs jours de suite`() {
        val entries = (0..20).map { day(it, DayColor.ORANGE, water = 2) }
        assertTrue(rulesOf(snapshotOf(entries)).contains(CoachRule.WATER_LOW))

        val hydrated = (0..20).map { day(it, DayColor.ORANGE, water = 7) }
        assertFalse(rulesOf(snapshotOf(hydrated)).contains(CoachRule.WATER_LOW))
    }

    // --- Le soutien passe devant ------------------------------------------

    @Test
    fun `une journee noire declenche un message de soutien`() {
        val entries = (0..10).map {
            day(it, if (it == 0) DayColor.BLACK else DayColor.ORANGE, wentOut = false)
        }
        val candidates = CoachRules.candidates(snapshotOf(entries))

        assertTrue(candidates.map { it.rule }.contains(CoachRule.BLACK_DAY))
        val support = candidates.first { it.rule == CoachRule.BLACK_DAY }
        val proposals = candidates.filter { it.rule == CoachRule.STAYED_IN }
        assertTrue(proposals.isNotEmpty())
        proposals.forEach { assertTrue(support.score > it.score) }
    }

    @Test
    fun `plusieurs jours tres sombres proposent un numero d'ecoute`() {
        val entries = listOf(
            day(0, DayColor.BLACK),
            day(1, DayColor.BLACK),
            day(2, DayColor.RED),
        ) + (3..10).map { day(it, DayColor.ORANGE) }

        assertTrue(rulesOf(snapshotOf(entries)).contains(CoachRule.DAYS_VERY_DARK))

        val nudge = CoachMessages.render(
            NudgeCandidate(CoachRule.DAYS_VERY_DARK, mapOf("n" to "3")), 0, "Ismael",
        )!!
        assertTrue(nudge.text.contains("3114"))
    }

    @Test
    fun `une seule journee noire ne sort pas le numero d'ecoute`() {
        val entries = (0..10).map { day(it, if (it == 0) DayColor.BLACK else DayColor.GREEN) }
        assertFalse(rulesOf(snapshotOf(entries)).contains(CoachRule.DAYS_VERY_DARK))
    }

    // --- Prieres et recherche d'emploi : jamais de reproche -----------------

    @Test
    fun `les prieres ne declenchent que des messages positifs`() {
        val prayerRules = CoachRule.entries.filter { it.card == DayCard.PRAYER }
        assertTrue(prayerRules.isNotEmpty())
        prayerRules.forEach {
            assertTrue(
                "${it.slug} ne doit pas etre une relance",
                it.tone == NudgeTone.WARM || it.tone == NudgeTone.PROUD,
            )
        }
    }

    @Test
    fun `les cinq prieres faites sont soulignees`() {
        val entries = (0..10).map { day(it, DayColor.ORANGE, prayers = 5) }
        val rules = rulesOf(snapshotOf(entries))
        assertTrue(rules.contains(CoachRule.PRAYERS_ALL))
        assertTrue(rules.contains(CoachRule.PRAYER_STREAK))
    }

    @Test
    fun `les candidatures de la semaine sont comptees`() {
        val entries = (0..10).map { day(it, DayColor.ORANGE, applications = if (it < 5) 1 else 0) }
        val candidate = CoachRules.candidates(snapshotOf(entries))
            .firstOrNull { it.rule == CoachRule.JOB_EFFORT }
        assertNotNull(candidate)
        assertEquals("5", candidate!!.values["n"])
    }

    @Test
    fun `une pause dans la recherche se dit sans reproche`() {
        val entries = (0..20).map { day(it, DayColor.ORANGE, applications = if (it >= 8) 2 else 0) }
        val candidate = CoachRules.candidates(snapshotOf(entries))
            .firstOrNull { it.rule == CoachRule.JOB_PAUSE }
        assertNotNull(candidate)
        assertEquals(NudgeTone.SOFT, CoachRule.JOB_PAUSE.tone)
    }

    // --- Ce qui va avec les bonnes journees --------------------------------

    @Test
    fun `un facteur associe aux bonnes journees est propose quand il manque`() {
        val entries = (0..19).map { back ->
            if (back % 2 == 0) day(back, DayColor.GREEN, wentOut = true)
            else day(back, DayColor.RED, wentOut = false)
        }.toMutableList()
        entries[0] = day(0, DayColor.ORANGE, wentOut = false)

        val snapshot = snapshotOf(entries)
        val finding = CoachFactors.analyse(snapshot).first { it.factor == CoachFactor.OUTING }
        assertTrue(finding.delta > CoachFactors.MIN_DELTA)

        val candidate = CoachRules.candidates(snapshot)
            .firstOrNull { it.rule == CoachRule.FACTOR_SUGGESTION }
        assertNotNull(candidate)
        assertEquals("sortir faire un petit tour", candidate!!.values["quoi"])
        assertEquals("les jours où tu sors", candidate.values["constat"])
    }

    @Test
    fun `rien n'est propose quand le facteur est deja la aujourd'hui`() {
        val entries = (0..19).map { back ->
            if (back % 2 == 0) day(back, DayColor.GREEN, wentOut = true)
            else day(back, DayColor.RED, wentOut = false)
        }
        val rules = rulesOf(snapshotOf(entries))
        assertFalse(rules.contains(CoachRule.FACTOR_SUGGESTION))
        assertTrue(rules.contains(CoachRule.FACTOR_TODAY))
    }

    @Test
    fun `sans assez de journees aucune comparaison n'est faite`() {
        val entries = listOf(
            day(0, DayColor.GREEN, wentOut = true),
            day(1, DayColor.RED, wentOut = false),
            day(2, DayColor.GREEN, wentOut = true),
        )
        assertTrue(CoachFactors.analyse(snapshotOf(entries)).isEmpty())
    }

    @Test
    fun `on ne propose jamais ce qui ne se decide pas`() {
        assertNull(CoachFactor.GOOD_SLEEP.invitation)
        assertNull(CoachFactor.SLEEP_BAD.invitation)
        assertNull(CoachFactor.PRAYERS.invitation)
        assertNull(CoachFactor.CRIED.invitation)
        assertNull(CoachFactor.ANXIETY.invitation)
    }

    // --- Les series --------------------------------------------------------

    @Test
    fun `trois journees vertes de suite sont soulignees`() {
        val entries = (0..10).map { day(it, if (it < 3) DayColor.GREEN else DayColor.ORANGE) }
        val candidate = CoachRules.candidates(snapshotOf(entries))
            .first { it.rule == CoachRule.GREEN_STREAK }
        assertEquals("3", candidate.values["n"])
    }

    @Test
    fun `la serie tient meme si la journee du jour n'est pas encore notee`() {
        val entries = (1..10).map { day(it, if (it <= 3) DayColor.GREEN else DayColor.ORANGE) }
        assertTrue(rulesOf(snapshotOf(entries)).contains(CoachRule.GREEN_STREAK))
    }

    @Test
    fun `un retour apres une absence est accueilli sans reproche`() {
        // Rien de note du jour -1 au jour -5 : cinq jours d'absence.
        val entries = (6..20).map { day(it, DayColor.ORANGE) }
        val candidate = CoachRules.candidates(snapshotOf(entries))
            .firstOrNull { it.rule == CoachRule.BACK_AFTER_BREAK }
        assertNotNull(candidate)
        assertEquals("5", candidate!!.values["n"])
    }

    // --- Le choix du message ------------------------------------------------

    @Test
    fun `une situation deja vue attend son tour`() {
        val entries = (0..10).map { day(it, DayColor.ORANGE) }
        val snapshot = snapshotOf(entries)
        val memory = TemporaryCoachMemory()

        val first = CoachEngine.choose(snapshot, memory, NudgeSurface.HOME)
        assertNotNull(first)
        CoachEngine.markShown(memory, first!!, NudgeSurface.HOME, snapshot.todayEpochDay)

        // Le meme jour, la carte ne change pas de discours.
        val sameDay = CoachEngine.choose(snapshot, memory, NudgeSurface.HOME)
        assertEquals(first.rule, sameDay?.rule)
        assertEquals(first.text, sameDay?.text)

        // Le lendemain, le delai d'attente de la regle s'applique.
        val tomorrow = snapshot.copy(today = today.plusDays(1))
        val next = CoachEngine.choose(tomorrow, memory, NudgeSurface.HOME)
        assertTrue(next == null || next.rule != first.rule)
    }

    @Test
    fun `le message ferme a la main ne revient pas le jour meme`() {
        val snapshot = snapshotOf((0..10).map { day(it, DayColor.ORANGE) })
        val memory = TemporaryCoachMemory()

        assertNotNull(CoachEngine.choose(snapshot, memory, NudgeSurface.HOME))
        memory.dismiss(NudgeSurface.HOME, snapshot.todayEpochDay)
        assertNull(CoachEngine.choose(snapshot, memory, NudgeSurface.HOME))
    }

    @Test
    fun `jamais plus que le quota de notifications`() {
        val entries = listOf(day(0, DayColor.BLACK), day(1, DayColor.BLACK)) +
            (2..10).map { day(it, DayColor.ORANGE) }
        val snapshot = snapshotOf(entries)
        val memory = TemporaryCoachMemory()

        val first = CoachEngine.choose(snapshot, memory, NudgeSurface.NOTIFICATION)
        assertNotNull(first)
        assertEquals(CoachRule.BLACK_STREAK, first!!.rule)

        val used = TemporaryCoachMemory()
        used.recordNotification(snapshot.todayEpochDay)
        assertNull(CoachEngine.choose(snapshot, used, NudgeSurface.NOTIFICATION, maxNotifications = 1))
        assertNotNull(CoachEngine.choose(snapshot, used, NudgeSurface.NOTIFICATION, maxNotifications = 2))
    }

    @Test
    fun `une journee sans rien a dire ne declenche pas de notification`() {
        val snapshot = snapshotOf((0..10).map { day(it, DayColor.ORANGE, note = "ok") })

        // Dans l'application, on dit quand meme bonjour.
        assertEquals(
            CoachRule.HELLO,
            CoachEngine.choose(snapshot, TemporaryCoachMemory(), NudgeSurface.HOME)?.rule,
        )
        // Mais on ne reveille pas le telephone pour ca.
        assertNull(CoachEngine.choose(snapshot, TemporaryCoachMemory(), NudgeSurface.NOTIFICATION))
    }

    @Test
    fun `chaque message part sur la bonne surface`() {
        val snapshot = snapshotOf((0..10).map { day(it, DayColor.ORANGE) })
        NudgeSurface.entries.forEach { surface ->
            val nudge = CoachEngine.choose(snapshot, TemporaryCoachMemory(), surface)
            if (nudge != null) assertTrue(surface in nudge.rule.surfaces)
        }
    }

    @Test
    fun `l'apercu des reglages ne consomme rien`() {
        val snapshot = snapshotOf((0..10).map { day(it, DayColor.ORANGE) })
        val memory = TemporaryCoachMemory()

        CoachEngine.preview(snapshot)
        CoachEngine.preview(snapshot)
        assertNull(memory.lastShown(CoachRule.HELLO.slug))
        assertNotNull(CoachEngine.choose(snapshot, memory, NudgeSurface.HOME))
    }

    // --- Les garde-fous ------------------------------------------------------

    @Test
    fun `une base vide ne fait pas planter l'algorithme`() {
        val snapshot = snapshotOf(emptyList())
        val candidates = CoachRules.candidates(snapshot)
        assertTrue(candidates.isNotEmpty())
        assertEquals(CoachRule.HELLO, candidates.last().rule)
        assertNotNull(CoachEngine.choose(snapshot, TemporaryCoachMemory(), NudgeSurface.HOME))
    }

    @Test
    fun `sans information sur le sport on ne reproche rien`() {
        val entries = (0..10).map { day(it, DayColor.ORANGE) }
        assertFalse(rulesOf(snapshotOf(entries)).contains(CoachRule.NO_MOVEMENT))

        val still = (0..10).map { day(it, DayColor.ORANGE, sport = SportLevel.NONE) }
        assertTrue(rulesOf(snapshotOf(still)).contains(CoachRule.NO_MOVEMENT))
    }

    @Test
    fun `les propositions se taisent la nuit`() {
        val entries = (0..10).map { day(it, DayColor.ORANGE, wentOut = false) }
        assertFalse(rulesOf(snapshotOf(entries, hour = 4)).contains(CoachRule.STAYED_IN))
        assertTrue(rulesOf(snapshotOf(entries, hour = 14)).contains(CoachRule.STAYED_IN))
    }

    @Test
    fun `le coup de pouce ne lit pas le journal`() {
        val withText = (0..10).map { day(it, DayColor.ORANGE, note = "un secret que personne ne doit lire") }
        val withoutText = (0..10).map { day(it, DayColor.ORANGE, note = "x") }

        // Seule la presence d'un texte compte, jamais son contenu.
        assertEquals(rulesOf(snapshotOf(withText)), rulesOf(snapshotOf(withoutText)))
        assertFalse(snapshotOf(withText).days.values.any { it.toString().contains("secret") })
    }
}
