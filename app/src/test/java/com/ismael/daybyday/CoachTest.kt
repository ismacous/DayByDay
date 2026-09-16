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
import com.ismael.daybyday.data.MoneyCategory
import com.ismael.daybyday.data.MoneyEntry
import com.ismael.daybyday.data.Prayer
import com.ismael.daybyday.data.SportLevel
import com.ismael.daybyday.data.Tag
import com.ismael.daybyday.data.TagCatalog
import com.ismael.daybyday.data.Treatment
import com.ismael.daybyday.ui.haloRadius
import com.ismael.daybyday.ui.presenceRadius
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
        /**
         * Les quatre moments sont remplis par defaut, et c'est important : une
         * journee dont les moments ne sont pas tous notes a une couleur encore
         * provisoire, dont l'algorithme n'a pas le droit de parler. Les tests
         * qui veulent une couleur qui compte partent donc d'une journee
         * complete ; ceux qui testent justement le garde-fou disent le
         * contraire explicitement.
         */
        parts: List<DayColor> = List(4) { DayColor.ORANGE },
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
        partEvening = parts.getOrNull(2)?.key,
        partNight = parts.getOrNull(3)?.key,
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
        on: LocalDate = today,
    ) = CoachSnapshot.build(
        today = on,
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

    /** Deux journees noires de suite : il y a forcement quelque chose a dire. */
    private fun talkingSnapshot() = snapshotOf(
        listOf(day(0, DayColor.BLACK), day(1, DayColor.BLACK)) +
            (2..10).map { day(it, DayColor.ORANGE) },
    )

    /** Un mouvement d'argent, [back] jours en arriere. */
    private fun money(back: Int, cents: Long, category: MoneyCategory) = MoneyEntry(
        epochDay = today.toEpochDay() - back,
        amountCents = cents,
        categoryKey = category.key,
    )

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

    /** De quoi remplir n'importe quel gabarit, comme a l'ecran. */
    private val sampleValues = mapOf(
        "n" to "6",
        "duree" to "5 h 20",
        "montant" to "1 200,00 €",
        "ecart" to "0,8",
        "mois" to "mars",
        "moment" to "matin",
        "constat" to "les jours où tu sors",
        "quoi" to "sortir faire un petit tour",
    )

    @Test
    fun `chaque phrase se comprend toute seule`() {
        // C'est le test le plus important du fichier. Une bulle apparait sans
        // titre et sans rien autour ; une notification encore moins. « Les
        // cinq, 6 jours d'affilée » ne veut rien dire hors contexte, et c'est
        // exactement ce qui s'etait glisse dans la premiere version.
        //
        // On verifie la phrase **telle qu'on la lit**, gabarits remplis : une
        // phrase qui commence par « {n} » commence en fait par un chiffre, et
        // une qui commence par « {constat} » commence par un mot en minuscule,
        // ce qui n'est pas la meme chose du tout.
        CoachRule.entries.forEach { rule ->
            CoachMessages.variantsFor(rule).forEachIndexed { index, template ->
                val phrase = CoachMessages
                    .render(NudgeCandidate(rule, sampleValues), index, "Ismael")!!
                    .text

                assertFalse("Gabarit non rempli (${rule.slug}) : $phrase", phrase.contains("{"))
                assertTrue(
                    "Trop courte pour se comprendre seule (${rule.slug}) : $phrase",
                    phrase.length >= 30,
                )
                assertTrue(
                    "Commence mal (${rule.slug}) : $phrase",
                    phrase.first().isUpperCase() || phrase.first().isDigit(),
                )
                assertTrue(
                    "Ne finit pas par une ponctuation (${rule.slug}) : $phrase",
                    phrase.last() in ".!?»",
                )
                if (rule.subjects.isNotEmpty()) {
                    assertTrue(
                        "Ne nomme pas son sujet ${rule.subjects} (${rule.slug}) : $template",
                        rule.subjects.any { phrase.lowercase().contains(it.lowercase()) },
                    )
                }
            }
        }
    }

    @Test
    fun `une felicitation felicite vraiment`() {
        // Le deuxieme reproche d'Ismael, apres les phrases telegraphiques :
        // « Tes cinq prieres, six jours de suite. C'est une regularite qui se
        // remarque. » n'est pas un compliment, c'est un releve — « un resume en
        // attendant notre mort ». Un ton PROUD ou CHEER doit donc porter un mot
        // d'elan, et c'est ce test qui le tient, pas la relecture.
        CoachRule.entries
            .filter { it.tone == NudgeTone.PROUD || it.tone == NudgeTone.CHEER }
            .forEach { rule ->
                CoachMessages.variantsFor(rule).forEachIndexed { index, _ ->
                    val phrase = CoachMessages
                        .render(NudgeCandidate(rule, sampleValues), index, "Ismael")!!
                        .text
                        .lowercase()

                    assertTrue(
                        "Felicitation sans elan (${rule.slug}) : $phrase",
                        CoachMessages.LIFT_WORDS.any { phrase.contains(it) },
                    )
                }
            }
    }

    @Test
    fun `le disque de la bulle a toujours un rayon`() {
        // Le crash qui fermait l'application deux secondes apres son ouverture :
        // a la premiere image, l'arrivee de la bulle vaut zero, le rayon tombait
        // a zero, et Android refuse un degrade radial de rayon nul. On balaie
        // toute l'arrivee et toute la respiration plutot que de regarder l'ecran.
        for (appear in 0..20) {
            for (breath in 0..20) {
                val radius = presenceRadius(74f, breath / 20f, appear / 20f)
                assertTrue("Rayon nul a $appear/$breath : $radius", radius > 0f)
            }
        }
        // Et une boite pas encore mesuree ne doit pas plus faire tomber l'appli.
        assertTrue(presenceRadius(0f, 0f, 0f) > 0f)
        assertTrue(haloRadius(0f) > 0f)
    }

    @Test
    fun `toute situation qui parle d'un sujet le nomme`() {
        // Les situations sans mot impose sont l'exception, pas la regle : si
        // la liste grossit, c'est que le catalogue redevient telegraphique.
        val withoutSubject = CoachRule.entries.filter { it.subjects.isEmpty() }
        assertTrue("Trop de situations sans sujet : $withoutSubject", withoutSubject.size <= 3)
    }

    @Test
    fun `la journee bouclee a de quoi ne pas se repeter`() {
        // C'est la seule situation qui peut revenir tous les jours : c'est donc
        // elle qui s'userait le plus vite.
        assertTrue(CoachMessages.variantsFor(CoachRule.DAY_COMPLETE).size >= 20)
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

    // --- Le garde-fou contre les faux positifs ------------------------------

    @Test
    fun `un seul moment rempli ne fait pas une journee verte`() {
        // Le bug tel qu'Ismael l'a vu : matin note en vert, la couleur du jour
        // se calcule donc en vert, et l'appli fete « enfin une journee verte »
        // a dix-huit heures — alors que la journee a fini en jaune.
        val history = (7..30).map { day(it, DayColor.ORANGE) }
        val matinVert = day(0, DayColor.GREEN, parts = listOf(DayColor.GREEN))
        val rules = rulesOf(snapshotOf(history + matinVert, hour = 18))

        assertFalse(rules.contains(CoachRule.FIRST_GREEN))
        assertFalse(rules.contains(CoachRule.GREEN_DAY))
    }

    @Test
    fun `une journee complete peut etre fetee`() {
        // Le meme jour, les quatre moments remplis : la couleur ne bougera
        // plus, on a le droit de s'en rejouir.
        val history = (7..30).map { day(it, DayColor.ORANGE) }
        val complete = day(0, DayColor.GREEN, parts = List(4) { DayColor.GREEN })
        val rules = rulesOf(snapshotOf(history + complete, hour = 18))

        assertTrue(rules.contains(CoachRule.FIRST_GREEN))
        assertTrue(rules.contains(CoachRule.GREEN_DAY))
    }

    @Test
    fun `une couleur choisie a la main est prise au mot`() {
        // Poser la couleur soi-meme est un choix, pas un calcul : il n'y a rien
        // a attendre de plus.
        val history = (7..30).map { day(it, DayColor.ORANGE) }
        val chosen = day(0, DayColor.GREEN, manual = true, parts = emptyList())

        assertTrue(rulesOf(snapshotOf(history + chosen, hour = 9)).contains(CoachRule.GREEN_DAY))
    }

    @Test
    fun `une journee passee compte telle qu'elle est`() {
        // Hier ne bougera plus, meme si un seul moment y a ete note : sinon
        // toutes les series du passe se mettraient a trouer.
        val entries = (0..10).map { back ->
            day(back, DayColor.GREEN, parts = listOf(DayColor.GREEN))
        }
        // Aujourd'hui est provisoire, mais les trois jours d'avant suffisent.
        assertTrue(rulesOf(snapshotOf(entries, hour = 9)).contains(CoachRule.GREEN_STREAK))
    }

    @Test
    fun `un matin noir recoit quand meme du soutien`() {
        // Le garde-fou ne doit pas rendre l'appli muette au pire moment : la
        // journee n'est pas encore jouee, mais le matin, lui, a bien eu lieu.
        val history = (1..10).map { day(it, DayColor.ORANGE) }
        val matinNoir = day(0, DayColor.BLACK, parts = listOf(DayColor.BLACK))
        val candidates = CoachRules.candidates(snapshotOf(history + matinNoir, hour = 14))
        val rules = candidates.map { it.rule }

        assertFalse("On ne declare pas la journee noire avant la fin", rules.contains(CoachRule.BLACK_DAY))
        assertTrue(rules.contains(CoachRule.DARK_MOMENT))
        assertEquals("matin", candidates.first { it.rule == CoachRule.DARK_MOMENT }.values["moment"])
    }

    @Test
    fun `le moment sombre se tait une fois la journee arretee`() {
        // Sinon on dirait deux fois la meme chose : une fois sur le moment, une
        // fois sur la journee.
        val history = (1..10).map { day(it, DayColor.ORANGE) }
        val noire = day(0, DayColor.BLACK, parts = List(4) { DayColor.BLACK })
        val rules = rulesOf(snapshotOf(history + noire, hour = 23))

        assertTrue(rules.contains(CoachRule.BLACK_DAY))
        assertFalse(rules.contains(CoachRule.DARK_MOMENT))
    }

    @Test
    fun `il ne doit plus manquer qu'un seul moment`() {
        val history = (1..10).map { day(it, DayColor.ORANGE) }

        // Deux moments sur quatre : la moitie de la journee reste a ecrire.
        val moitie = day(0, DayColor.RED, parts = List(2) { DayColor.RED })
        assertFalse(rulesOf(snapshotOf(history + moitie, hour = 20)).contains(CoachRule.RED_DAY))

        // Trois sur quatre, le soir : il ne reste que la nuit, on peut parler.
        val presque = day(0, DayColor.RED, parts = List(3) { DayColor.RED })
        assertTrue(rulesOf(snapshotOf(history + presque, hour = 22)).contains(CoachRule.RED_DAY))
    }

    @Test
    fun `un moment deja passe qu'on a saute laisse la couleur en suspens`() {
        // Trois moments notes, mais pas le matin, et il est quatorze heures :
        // le matin a bien eu lieu, il manque a l'appel, la couleur ne raconte
        // donc pas la journee.
        val history = (1..10).map { day(it, DayColor.ORANGE) }
        val sansMatin = DayEntry(
            epochDay = today.toEpochDay(),
            colorKey = DayColor.RED.key,
            colorManual = false,
            partAfternoon = DayColor.RED.key,
            partEvening = DayColor.RED.key,
            partNight = DayColor.RED.key,
        )

        assertFalse(rulesOf(snapshotOf(history + sansMatin, hour = 14)).contains(CoachRule.RED_DAY))
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

    // --- Argent : la ou on regarde justement ces chiffres --------------------

    @Test
    fun `une rentree d'argent se remarque, et seulement dans l'onglet Argent`() {
        val entries = (0..20).map { day(it, DayColor.ORANGE) }
        val income = listOf(money(2, 120_000L, MoneyCategory.SALARY))

        val candidate = CoachRules.candidates(snapshotOf(entries, money = income))
            .firstOrNull { it.rule == CoachRule.MONEY_INCOME }
        assertNotNull(candidate)
        assertTrue(candidate!!.values["montant"]!!.contains("1 200"))

        // Elle n'a rien a faire au milieu du calendrier.
        assertFalse(CoachRule.MONEY_INCOME.surfaces.contains(NudgeSurface.STATS))
        assertTrue(CoachRule.MONEY_INCOME.surfaces.contains(NudgeSurface.MONEY))
    }

    @Test
    fun `une correction de solde n'est ni une depense ni une rentree`() {
        // Sans ca, remettre le compteur juste apres une depense la compterait
        // une seconde fois, a l'envers.
        val entries = (0..20).map { day(it, DayColor.ORANGE) }
        val adjustments = listOf(money(1, 90_000L, MoneyCategory.ADJUSTMENT))
        assertFalse(
            rulesOf(snapshotOf(entries, money = adjustments)).contains(CoachRule.MONEY_INCOME),
        )
    }

    @Test
    fun `un mois dans le vert propose de mettre de cote`() {
        // Avant le milieu du mois, le solde ne veut rien dire : les grosses
        // depenses n'ont pas encore eu lieu. Le test se place donc un 20.
        val on = LocalDate.of(2026, 3, 20)
        val entries = (0..20).map { back ->
            DayEntry(epochDay = on.toEpochDay() - back, colorKey = DayColor.ORANGE.key)
        }
        val movements = listOf(
            MoneyEntry(
                epochDay = on.toEpochDay() - 8,
                amountCents = 150_000L,
                categoryKey = MoneyCategory.SALARY.key,
            ),
            MoneyEntry(
                epochDay = on.toEpochDay() - 5,
                amountCents = -30_000L,
                categoryKey = MoneyCategory.FOOD.key,
            ),
        )
        val snapshot = snapshotOf(entries, money = movements, on = on)
        val candidate = CoachRules.candidates(snapshot)
            .firstOrNull { it.rule == CoachRule.MONEY_SAVING }
        assertNotNull(candidate)
        assertEquals(NudgeTone.NUDGE, CoachRule.MONEY_SAVING.tone)
    }

    @Test
    fun `on ne reclame un suivi d'argent qu'a qui en tient un`() {
        val entries = (0..40).map { day(it, DayColor.ORANGE) }

        // Personne n'a jamais rien note : on se tait.
        assertFalse(rulesOf(snapshotOf(entries)).contains(CoachRule.MONEY_QUIET))

        // Le suivi existe, mais plus rien depuis deux semaines.
        val past = (20..30).map { money(it, -2_000L, MoneyCategory.FOOD) }
        val candidate = CoachRules.candidates(snapshotOf(entries, money = past))
            .firstOrNull { it.rule == CoachRule.MONEY_QUIET }
        assertNotNull(candidate)
        assertEquals("20", candidate!!.values["n"])
    }

    // --- Bilan ----------------------------------------------------------------

    @Test
    fun `le bilan dit d'abord qu'il n'a pas assez de matiere`() {
        val young = snapshotOf((0..9).map { day(it, DayColor.ORANGE) })
        val rules = rulesOf(young)
        assertTrue(rules.contains(CoachRule.STATS_YOUNG))
        // Et rien d'autre du bilan : commenter une tendance sur dix journees
        // serait pire que de se taire.
        assertFalse(rules.contains(CoachRule.STATS_TREND_UP))
        assertFalse(rules.contains(CoachRule.STATS_TREND_DOWN))
        assertFalse(rules.contains(CoachRule.STATS_HARD_PART))
    }

    @Test
    fun `le bilan repere le moment de la journee le plus dur`() {
        // Les matins noirs, le reste de la journee correct.
        val entries = (0..40).map { back ->
            day(
                back, DayColor.ORANGE,
                parts = listOf(DayColor.BLACK, DayColor.GREEN, DayColor.GREEN, DayColor.GREEN),
            )
        }
        val candidate = CoachRules.candidates(snapshotOf(entries))
            .firstOrNull { it.rule == CoachRule.STATS_HARD_PART }
        assertNotNull(candidate)
        assertEquals("matin", candidate!!.values["moment"])
    }

    @Test
    fun `le bilan ne designe aucun moment quand ils se valent`() {
        val entries = (0..40).map { back ->
            day(
                back, DayColor.ORANGE,
                parts = listOf(DayColor.ORANGE, DayColor.ORANGE, DayColor.ORANGE, DayColor.ORANGE),
            )
        }
        assertFalse(rulesOf(snapshotOf(entries)).contains(CoachRule.STATS_HARD_PART))
    }

    @Test
    fun `le bilan raconte la tendance des trente derniers jours`() {
        // Trente jours corrects, puis trente jours durs avant.
        val entries = (0..59).map { back ->
            day(back, if (back < 30) DayColor.GREEN else DayColor.RED)
        }
        val candidate = CoachRules.candidates(snapshotOf(entries))
            .firstOrNull { it.rule == CoachRule.STATS_TREND_UP }
        assertNotNull(candidate)
        assertFalse(rulesOf(snapshotOf(entries)).contains(CoachRule.STATS_TREND_DOWN))
    }

    // --- Le choix du message ------------------------------------------------

    @Test
    fun `une situation deja vue attend son tour`() {
        val snapshot = talkingSnapshot()
        val memory = TemporaryCoachMemory()

        val first = CoachEngine.choose(snapshot, memory, NudgeSurface.MONTH)
        assertNotNull(first)
        CoachEngine.markShown(memory, first!!, NudgeSurface.MONTH, snapshot.todayEpochDay)

        // Le lendemain, le quota du jour est neuf — mais le delai d'attente de
        // la situation, lui, court toujours : elle ne revient pas.
        val tomorrow = snapshot.copy(today = today.plusDays(1))
        val next = CoachEngine.choose(tomorrow, memory, NudgeSurface.MONTH)
        assertTrue(next == null || next.rule != first.rule)
    }

    @Test
    fun `l'apparition du jour ne revient pas une seconde fois`() {
        val snapshot = talkingSnapshot()
        val memory = TemporaryCoachMemory()

        val first = CoachEngine.choose(snapshot, memory, NudgeSurface.MONTH)
        assertNotNull(first)
        CoachEngine.markShown(memory, first!!, NudgeSurface.MONTH, snapshot.todayEpochDay)
        assertNull(CoachEngine.choose(snapshot, memory, NudgeSurface.MONTH))
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
    fun `une journee sans rien a dire ne fait apparaitre personne`() {
        // Il n'y a plus de phrase de politesse : une bulle qui interrompt pour
        // dire « rien de special aujourd'hui » est exactement ce qui fait qu'on
        // arrete de lire les suivantes. Trente-cinq journees banales : assez
        // pour que le bilan ne se plaigne pas non plus du manque de matiere.
        val snapshot = snapshotOf(
            (0..34).map { day(it, DayColor.ORANGE, note = "ok") },
        )
        NudgeSurface.entries.forEach { surface ->
            assertNull(
                "Rien a dire, et pourtant ça parle sur $surface",
                CoachEngine.choose(snapshot, TemporaryCoachMemory(), surface),
            )
        }
    }

    @Test
    fun `une seule apparition par jour`() {
        val entries = listOf(day(0, DayColor.BLACK), day(1, DayColor.BLACK)) +
            (2..10).map { day(it, DayColor.ORANGE) }
        val snapshot = snapshotOf(entries)
        val memory = TemporaryCoachMemory()

        val first = CoachEngine.choose(snapshot, memory, NudgeSurface.MONTH)
        assertNotNull(first)
        CoachEngine.markShown(memory, first!!, NudgeSurface.MONTH, snapshot.todayEpochDay)

        // Plus rien aujourd'hui, sur aucun ecran : le quota est celui de la
        // journee, pas celui de l'ecran.
        NudgeSurface.SCREENS.forEach { surface ->
            assertNull(CoachEngine.choose(snapshot, memory, surface))
        }
    }

    @Test
    fun `la fete de la journee bouclee ne consomme pas le quota`() {
        val snapshot = snapshotOf((0..10).map { day(it, DayColor.ORANGE) })
        val memory = TemporaryCoachMemory()

        val party = CoachEngine.forRule(snapshot, memory, CoachRule.DAY_COMPLETE)
        assertNotNull(party)
        CoachEngine.markShown(memory, party!!, NudgeSurface.DAY, snapshot.todayEpochDay)
        assertEquals(0, memory.popupsShown(snapshot.todayEpochDay))

        // Et elle ne se rejoue pas le meme jour.
        assertNull(CoachEngine.forRule(snapshot, memory, CoachRule.DAY_COMPLETE))
    }

    @Test
    fun `chaque message part sur la bonne surface`() {
        val snapshot = talkingSnapshot()
        NudgeSurface.entries.forEach { surface ->
            val nudge = CoachEngine.choose(snapshot, TemporaryCoachMemory(), surface)
            if (nudge != null) assertTrue(surface in nudge.rule.surfaces)
        }
    }

    @Test
    fun `l'apercu des reglages ne consomme rien`() {
        val snapshot = talkingSnapshot()
        val memory = TemporaryCoachMemory()

        CoachEngine.preview(snapshot)
        CoachEngine.preview(snapshot)
        assertEquals(0, memory.popupsShown(snapshot.todayEpochDay))
        assertNotNull(CoachEngine.choose(snapshot, memory, NudgeSurface.MONTH))
    }

    // --- Les garde-fous ------------------------------------------------------

    @Test
    fun `une base vide ne fait pas planter l'algorithme`() {
        val snapshot = snapshotOf(emptyList())
        // Une seule chose a dire sur une base vide, et seulement dans le
        // bilan : qu'il n'y a pas encore de quoi comparer.
        val rules = rulesOf(snapshot)
        assertEquals(listOf(CoachRule.STATS_YOUNG), rules)
        assertNotNull(CoachEngine.choose(snapshot, TemporaryCoachMemory(), NudgeSurface.STATS))
        assertNull(CoachEngine.choose(snapshot, TemporaryCoachMemory(), NudgeSurface.MONTH))
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
