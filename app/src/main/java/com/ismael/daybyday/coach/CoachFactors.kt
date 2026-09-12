package com.ismael.daybyday.coach

/**
 * Une etiquette absente compte comme « non » : c'est le meme choix que dans
 * l'onglet Bilan, et c'est aussi ce que dit la carte — ne rien cocher est une
 * reponse. Sans ca, presque aucune comparaison ne serait possible.
 *
 * Fonction de fichier et non de companion : les entrees d'un enum sont
 * construites avant son companion object.
 */
private fun tag(slug: String): (CoachDay) -> Boolean? = { day -> day.tagSlugs.contains(slug) }

/**
 * Les facteurs que l'algorithme sait comparer a la couleur des journees.
 *
 * C'est la meme idee que « Ce qui va avec tes bonnes journees » dans l'onglet
 * Bilan : on compare la moyenne des journees ou le facteur est present a celle
 * ou il ne l'est pas. Ca ne prouve aucune cause, et les messages le disent.
 *
 * La couleur de la journee, elle, ne sort jamais d'ici : elle reste choisie a
 * la main ou calculee a partir des quatre moments. Sinon la comparaison se
 * mordrait la queue.
 *
 * @param constat complement de phrase : « ... les jours ou tu sors ».
 * @param invitation proposition concrete, ou `null` quand ca n'a pas de sens
 *   d'en faire une (on ne decide pas de bien dormir, ni de ne pas pleurer).
 */
enum class CoachFactor(
    val slug: String,
    val constat: String,
    val invitation: String?,
    private val test: (CoachDay) -> Boolean?,
) {
    // Les cartes, qui savent repondre oui ou non.
    OUTING("sortie", "les jours où tu sors", "sortir faire un petit tour", { it.wentOut }),
    MOVED("bouge", "les jours où tu bouges", "bouger un peu aujourd'hui", { it.moved }),
    GOOD_FOOD("bien_mange", "les jours où tu manges bien", "te préparer un vrai repas", { it.ateWell }),
    SHOWER("douche", "les jours où tu prends une douche", "prendre une douche", { it.showered }),
    GOOD_SLEEP("vraie_nuit", "les nuits où tu dors sept heures ou plus", null, { it.sleptWell }),
    WATER(
        "eau", "les jours où tu bois assez d'eau", "boire quelques verres d'eau",
        { day -> day.waterGlasses?.let { it >= 6 } },
    ),
    PRAYERS(
        "prieres", "les jours où tu fais tes cinq prières", null,
        { day -> if (day.prayersTouched) day.allPrayersDone else null },
    ),
    BRUSHING(
        "brossage", "les jours où tu te brosses les dents", "te brosser les dents",
        { day -> day.brushingsDone > 0 },
    ),
    JOB(
        "candidature", "les jours où tu envoies une candidature", "envoyer une candidature",
        { day -> day.jobApplications?.let { it > 0 } },
    ),

    // Les etiquettes.
    WALK("marche", "les jours où tu marches", "aller marcher vingt minutes", tag("walk")),
    CHORES("menage", "les jours où tu ranges un peu", "ranger un petit coin", tag("chores")),
    ERRANDS("courses", "les jours où tu fais tes courses", "faire une course qui traîne", tag("errands")),
    FRIENDS("amis", "les jours où tu vois tes ami·es", "envoyer un message à un pote", tag("friends")),
    GIRLFRIEND("moitie", "les jours avec ta moitié", "prendre un moment à deux", tag("girlfriend")),
    FAMILY("famille", "les jours où tu vois ta famille", "appeler quelqu'un de ta famille", tag("family")),
    INTERVIEW("entretien", "les jours d'entretien", null, tag("interview")),
    ADMIN("demarches", "les jours où tu avances tes démarches", "avancer une démarche qui traîne", tag("admin")),
    FREELANCE("freelance", "les jours de boulot freelance", null, tag("freelance")),
    TEMP_WORK("interim", "les jours d'intérim", null, tag("temp_work")),

    // Facteurs qu'on observe mais qu'on ne propose jamais.
    SLEEP_GOOD("bien_dormi", "les jours où tu as bien dormi", null, tag("sleep_good")),
    SLEEP_BAD("mal_dormi", "les jours où tu dors mal", null, tag("sleep_bad")),
    SNACKING("grignotage", "les jours de grignotage", null, tag("snacking")),
    SOCIAL_MEDIA("reseaux", "les jours à beaucoup de réseaux sociaux", null, tag("social_media")),
    SERIES("series", "les jours de séries ou de films", null, tag("series")),
    GAMES("jeux", "les jours de jeux vidéo", null, tag("games")),
    ANXIETY("angoisse", "les jours de crise d'angoisse", null, tag("anxiety")),
    CRIED("larmes", "les jours où tu pleures", null, tag("cried")),
    APPOINTMENT("rendez_vous", "les jours de rendez-vous médical", null, tag("appointment")),
    ;

    /** true / false, ou null quand la journee ne dit rien sur ce facteur. */
    fun presentIn(day: CoachDay): Boolean? = test(day)
}

/**
 * Resultat d'une comparaison. [delta] est l'ecart de moyenne entre les
 * journees avec et sans le facteur, sur l'echelle 0-3 des couleurs.
 */
data class FactorFinding(
    val factor: CoachFactor,
    val delta: Double,
    val withDays: Int,
    val withoutDays: Int,
    val presentToday: Boolean?,
)

object CoachFactors {

    /** En dessous de cet ecart, la difference ne vaut pas la peine d'etre dite. */
    const val MIN_DELTA = 0.45

    /** Il faut au moins ce nombre de journees de chaque cote. */
    const val MIN_DAYS = 5

    /** On regarde un peu plus de six mois en arriere : au-dela, la vie a change. */
    const val WINDOW_DAYS = 200

    /**
     * Compare chaque facteur a la couleur des journees, et renvoie les ecarts
     * les plus marques en premier.
     */
    fun analyse(
        snapshot: CoachSnapshot,
        minDays: Int = MIN_DAYS,
        windowDays: Int = WINDOW_DAYS,
    ): List<FactorFinding> {
        val oldest = snapshot.todayEpochDay - windowDays
        val colored = snapshot.days.values.filter { it.isNoted && it.epochDay >= oldest }
        if (colored.size < minDays * 2) return emptyList()

        val today = snapshot.days[snapshot.todayEpochDay]

        return CoachFactor.entries.mapNotNull { factor ->
            val withGroup = mutableListOf<Int>()
            val withoutGroup = mutableListOf<Int>()
            colored.forEach { day ->
                val score = day.score ?: return@forEach
                when (factor.presentIn(day)) {
                    true -> withGroup += score
                    false -> withoutGroup += score
                    null -> Unit
                }
            }
            if (withGroup.size < minDays || withoutGroup.size < minDays) return@mapNotNull null
            FactorFinding(
                factor = factor,
                delta = withGroup.average() - withoutGroup.average(),
                withDays = withGroup.size,
                withoutDays = withoutGroup.size,
                presentToday = today?.let { factor.presentIn(it) },
            )
        }.sortedByDescending { kotlin.math.abs(it.delta) }
    }
}
