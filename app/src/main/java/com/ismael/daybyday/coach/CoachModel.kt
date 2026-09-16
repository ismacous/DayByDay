package com.ismael.daybyday.coach

import com.ismael.daybyday.data.DayCard

/**
 * Le "coup de pouce" : quelqu'un qui te parle, puis s'en va.
 *
 * Ce n'est pas une carte posee dans la page. Une carte reste la, personne ne
 * la ferme, et elle finit par ne plus rien vouloir dire — on la lit comme du
 * decor. C'est une **apparition** : elle arrive, elle dit une chose, on
 * repond, elle disparait jusqu'a la prochaine fois. La difference entre une
 * application qui affiche du texte et un ami qui passe la tete.
 *
 * Trois regles portent tout le reste :
 *
 * 1. **Chaque phrase se suffit a elle-meme.** Elle apparait sans titre, sans
 *    carte autour, parfois dans une notification. « Les cinq, 6 jours
 *    d'affilee » ne veut rien dire hors contexte ; « Tes cinq prieres, six
 *    jours de suite » oui. Chaque situation declare les mots que ses phrases
 *    doivent contenir ([subjects]), et un test le verifie.
 * 2. **Elle parle la ou c'est pertinent.** Une remarque sur l'argent arrive
 *    dans l'onglet Argent, une lecture de tendance dans le Bilan. Une phrase
 *    juste au mauvais endroit est une phrase qu'on ne lit pas.
 * 3. **Elle ne parle pas pour rien.** Au plus une apparition par jour, et
 *    seulement si elle a quelque chose a dire.
 *
 * Tout ici est du Kotlin pur, sans dependance a Android : c'est ce qui permet
 * de le tester sur machine, sans emulateur.
 */

/** Ton d'un message : change la couleur, le symbole et le geste d'arrivee. */
enum class NudgeTone(val emoji: String) {
    /** Soutien quand c'est dur. Jamais de conseil, jamais de jugement. */
    CARE("🫂"),

    /** Remarque neutre, dite en passant. */
    SOFT("💬"),

    /** Encouragement chaleureux. */
    WARM("🌱"),

    /** On souligne quelque chose de bien. */
    PROUD("✨"),

    /** Proposition concrete, toujours formulee comme une question. */
    NUDGE("👉"),

    /** La fete : la journee est complete. Le seul ton qui lance des confettis. */
    CHEER("🎉"),
}

/**
 * Ou une apparition a le droit de se produire.
 *
 * Ce sont les ecrans de l'application, plus les notifications. Une situation
 * declare les ecrans ou elle a du sens : c'est ce qui fait la difference entre
 * « l'application affiche un message » et « on me dit quelque chose d'utile
 * au moment ou je regarde justement ca ».
 */
enum class NudgeSurface {
    /** L'onglet Mois. */
    MONTH,

    /** L'ecran d'une journee. */
    DAY,

    /** L'onglet Bilan. */
    STATS,

    /** L'onglet Argent. */
    MONEY,

    /** L'onglet Annee. */
    YEAR,

    /** Une notification, au maximum une ou deux par jour. */
    NOTIFICATION;

    companion object {
        /** Les ecrans, sans les notifications. */
        val SCREENS: Set<NudgeSurface> = setOf(MONTH, DAY, STATS, MONEY, YEAR)
    }
}

/** Partout dans l'application : pour ce qui compte quel que soit l'ecran. */
private val ANYWHERE: Set<NudgeSurface> = NudgeSurface.SCREENS

/** Partout, et aussi en notification. */
private val ANYWHERE_AND_NOTIFY: Set<NudgeSurface> =
    NudgeSurface.SCREENS + NudgeSurface.NOTIFICATION

/**
 * Une situation reconnue par l'algorithme.
 *
 * @param slug identifiant stable, utilise pour retenir ce qui a deja ete dit.
 *   Le renommer ferait reapparaitre des messages recemment vus : a eviter.
 * @param priority plus c'est haut, plus le message passe devant. Le soutien
 *   dans les moments durs passe toujours avant les propositions.
 * @param cooldownDays nombre de jours a attendre avant de reproposer cette
 *   situation, pour ne pas radoter.
 * @param surfaces les ecrans ou cette situation a du sens.
 * @param subjects les mots dont **chaque** phrase doit contenir au moins un.
 *   C'est le garde-fou contre les phrases telegraphiques : une phrase sur les
 *   prieres doit dire « priere », sinon elle ne veut rien dire toute seule.
 * @param card la carte de "Ma journee" dont cette situation parle. Une carte
 *   masquee fait taire ses regles : masquer une carte ne doit rien couter,
 *   pas plus ici que dans la note.
 */
enum class CoachRule(
    val slug: String,
    val tone: NudgeTone,
    val priority: Int,
    val cooldownDays: Int,
    val surfaces: Set<NudgeSurface>,
    val subjects: List<String> = emptyList(),
    val notificationTitle: String = "Un petit mot",
    val card: DayCard? = null,
) {

    // --- La journee bouclee ------------------------------------------------

    /**
     * Toutes les cartes affichees ont ete verifiees. C'est la seule situation
     * qui se declenche **en reaction directe a un geste**, et la seule qui
     * fait la fete.
     */
    DAY_COMPLETE(
        "journee_bouclee", NudgeTone.CHEER, 140, 1, setOf(NudgeSurface.DAY),
        subjects = listOf("journée", "cartes", "tout"),
    ),

    // --- Soutien quand c'est dur -------------------------------------------

    /** Plusieurs journees tres sombres de suite. Le seul message qui donne un numero. */
    DAYS_VERY_DARK(
        "jours_tres_sombres", NudgeTone.CARE, 120, 10, ANYWHERE_AND_NOTIFY,
        subjects = listOf("jour"), notificationTitle = "Je suis là",
    ),

    /** Deux journees noires ou plus a la suite. */
    BLACK_STREAK(
        "noires_de_suite", NudgeTone.CARE, 110, 3, ANYWHERE_AND_NOTIFY,
        subjects = listOf("jour"), notificationTitle = "Je suis là",
    ),

    /** La journee du jour vient d'etre notee en noir. */
    BLACK_DAY(
        "journee_noire", NudgeTone.CARE, 100, 1, ANYWHERE,
        subjects = listOf("journée", "noir"),
    ),

    /** Etiquette "Crise d'angoisse" cochee aujourd'hui. */
    ANXIETY(
        "angoisse", NudgeTone.CARE, 96, 2, ANYWHERE,
        subjects = listOf("angoisse"),
    ),

    /** Etiquette "J'ai pleure" cochee aujourd'hui. */
    CRIED(
        "larmes", NudgeTone.CARE, 95, 2, ANYWHERE,
        subjects = listOf("pleuré", "larmes"),
    ),

    /**
     * Un moment de la journee est note en rouge ou en noir, alors que la
     * couleur du jour n'est pas encore arretee.
     *
     * Elle existe pour une raison precise : les regles qui parlent de la
     * journee se taisent tant que sa couleur peut encore bouger, et sans
     * celle-ci un matin noir note a 9 h ne recevrait plus rien — exactement le
     * moment ou il faut etre la. Elle parle donc du **moment**, qui est
     * certain, et jamais de la journee, qui ne l'est pas encore.
     */
    DARK_MOMENT(
        "moment_sombre", NudgeTone.CARE, 92, 1, ANYWHERE,
        subjects = listOf("matin", "après-midi", "soir", "nuit"),
    ),

    /** La journee du jour vient d'etre notee en rouge. */
    RED_DAY(
        "journee_rouge", NudgeTone.CARE, 90, 1, ANYWHERE,
        subjects = listOf("journée"),
    ),

    /** Semaine basse : moyenne des sept derniers jours tres en dessous. */
    HARD_WEEK(
        "semaine_dure", NudgeTone.CARE, 80, 7, ANYWHERE_AND_NOTIFY,
        subjects = listOf("semaine"),
    ),

    // --- Sante et soin de soi ----------------------------------------------

    /** Des traitements actifs, mais plus aucune prise cochee depuis des jours. */
    MEDS_MISSING(
        "traitement_oublie", NudgeTone.CARE, 88, 2, ANYWHERE_AND_NOTIFY,
        subjects = listOf("traitement"), notificationTitle = "Tes traitements",
        card = DayCard.TREATMENT,
    ),

    /** La douche laissee de cote plusieurs jours, alors qu'elle est suivie. */
    SHOWER_MISSING(
        "douche_oubliee", NudgeTone.NUDGE, 72, 3, ANYWHERE_AND_NOTIFY,
        subjects = listOf("douche"), card = DayCard.HYGIENE,
    ),

    /** Plusieurs jours sans un seul brossage coche. */
    BRUSHING_LOW(
        "brossage_oublie", NudgeTone.NUDGE, 60, 4, ANYWHERE,
        subjects = listOf("dents", "brossage"), card = DayCard.HYGIENE,
    ),

    /** Des nuits nettement trop courtes, d'apres les heures saisies. */
    SHORT_NIGHTS(
        "nuits_courtes", NudgeTone.NUDGE, 62, 5, ANYWHERE,
        subjects = listOf("nuit", "dors", "sommeil", "dormi"), card = DayCard.SLEEP,
    ),

    /** Plusieurs couchers apres minuit de suite. */
    LATE_NIGHTS(
        "couche_tard", NudgeTone.NUDGE, 40, 5, ANYWHERE,
        subjects = listOf("couche", "couché", "nuit"), card = DayCard.SLEEP,
    ),

    /** Peu d'eau bue ces derniers jours, alors que le verre est suivi. */
    WATER_LOW(
        "peu_d_eau", NudgeTone.NUDGE, 32, 4, ANYWHERE,
        subjects = listOf("eau", "verre"), card = DayCard.FOOD,
    ),

    // --- Ce qui remonte ----------------------------------------------------

    /** Premiere journee verte apres une longue serie sans. */
    FIRST_GREEN(
        "premiere_verte", NudgeTone.PROUD, 85, 7, ANYWHERE,
        subjects = listOf("verte", "journée"),
    ),

    /** Retour dans l'application apres plusieurs jours sans rien noter. */
    BACK_AFTER_BREAK(
        "retour", NudgeTone.WARM, 78, 5, ANYWHERE,
        subjects = listOf("jour"),
    ),

    /** Trois journees vertes d'affilee. */
    GREEN_STREAK(
        "trois_vertes", NudgeTone.PROUD, 75, 4, ANYWHERE,
        subjects = listOf("journée", "verte"),
    ),

    /** Aujourd'hui remonte nettement par rapport a hier. */
    REBOUND(
        "remontee", NudgeTone.PROUD, 70, 1, ANYWHERE,
        subjects = listOf("hier"),
    ),

    /** Palier de jours notes d'affilee (7, 14, 30, 50, 100...). */
    NOTING_STREAK(
        "serie_notee", NudgeTone.PROUD, 65, 5, ANYWHERE_AND_NOTIFY,
        subjects = listOf("jour"),
    ),

    /** Les cinq prieres faites plusieurs jours de suite. */
    PRAYER_STREAK(
        "serie_prieres", NudgeTone.PROUD, 64, 6, ANYWHERE,
        subjects = listOf("prière"), card = DayCard.PRAYER,
    ),

    /** Journee verte notee aujourd'hui. */
    GREEN_DAY(
        "journee_verte", NudgeTone.WARM, 58, 1, ANYWHERE,
        subjects = listOf("journée"),
    ),

    /** Des candidatures envoyees cette semaine. */
    JOB_EFFORT(
        "candidatures", NudgeTone.PROUD, 57, 5, ANYWHERE,
        subjects = listOf("candidature"), card = DayCard.WORK,
    ),

    /** Semaine en cours meilleure que la precedente. */
    BETTER_WEEK(
        "semaine_meilleure", NudgeTone.PROUD, 55, 7, ANYWHERE,
        subjects = listOf("semaine"),
    ),

    /** Les cinq prieres faites aujourd'hui. */
    PRAYERS_ALL(
        "cinq_prieres", NudgeTone.WARM, 54, 2, ANYWHERE,
        subjects = listOf("prière"), card = DayCard.PRAYER,
    ),

    /** Mois en cours au-dessus de tous les mois precedents. */
    BEST_MONTH(
        "meilleur_mois", NudgeTone.PROUD, 50, 20, ANYWHERE,
        subjects = listOf("mois"),
    ),

    // --- Ce qui va avec les bonnes journees --------------------------------

    /** Un facteur associe aux bonnes journees, absent aujourd'hui. */
    FACTOR_SUGGESTION(
        "facteur_positif", NudgeTone.NUDGE, 68, 2, ANYWHERE_AND_NOTIFY,
        subjects = listOf("journée"),
    ),

    /** Ce facteur est la aujourd'hui : on le souligne. */
    FACTOR_TODAY(
        "facteur_present", NudgeTone.WARM, 52, 3, ANYWHERE,
        subjects = listOf("journée"),
    ),

    /** Un facteur qui accompagne souvent les journees plus dures. Descriptif. */
    FACTOR_HEAVY(
        "facteur_lourd", NudgeTone.SOFT, 38, 5, ANYWHERE,
        subjects = listOf("journée"),
    ),

    // --- Habitudes du quotidien --------------------------------------------

    /** Plusieurs jours d'affilee sans personne de note. */
    ALONE_STREAK(
        "personne", NudgeTone.NUDGE, 56, 4, ANYWHERE_AND_NOTIFY,
        subjects = listOf("jour"), card = DayCard.SOCIAL,
    ),

    /** Plusieurs jours d'affilee sans sortir. */
    STAYED_IN(
        "pas_sorti", NudgeTone.NUDGE, 48, 3, ANYWHERE_AND_NOTIFY,
        subjects = listOf("sortir", "sorti", "dehors"), card = DayCard.ACTIVITY,
    ),

    /** Plusieurs jours d'affilee sans rien bouger. */
    NO_MOVEMENT(
        "pas_bouge", NudgeTone.NUDGE, 45, 4, ANYWHERE_AND_NOTIFY,
        subjects = listOf("bouger", "bougé"), card = DayCard.ACTIVITY,
    ),

    /** Etiquette "Mal dormi" cochee plusieurs fois de suite. */
    BAD_SLEEP(
        "mal_dormi", NudgeTone.NUDGE, 42, 5, ANYWHERE,
        subjects = listOf("dor", "nuit", "sommeil"), card = DayCard.SLEEP,
    ),

    /** Temps d'ecran nettement au-dessus de l'habitude. */
    HIGH_SCREEN(
        "ecran_haut", NudgeTone.SOFT, 35, 4, ANYWHERE,
        subjects = listOf("écran", "téléphone"), card = DayCard.OUTSIDE,
    ),

    /** Tres peu de pas alors que la journee est deja bien avancee. */
    LOW_STEPS(
        "peu_de_pas", NudgeTone.NUDGE, 30, 3, ANYWHERE,
        subjects = listOf("pas"), card = DayCard.ACTIVITY,
    ),

    // --- Argent : dans l'onglet Argent, la ou on regarde ces chiffres -------

    /** Une rentree d'argent notee ces derniers jours. */
    MONEY_INCOME(
        "rentree_argent", NudgeTone.WARM, 66, 4, setOf(NudgeSurface.MONEY, NudgeSurface.MONTH),
        subjects = listOf("rentré", "€"), card = DayCard.MONEY,
    ),

    /** Le mois reste dans le vert a date : de quoi mettre un peu de cote. */
    MONEY_SAVING(
        "mettre_de_cote", NudgeTone.NUDGE, 44, 12, setOf(NudgeSurface.MONEY),
        subjects = listOf("€", "mois"), card = DayCard.MONEY,
    ),

    /** Mois nettement plus lourd que le precedent, a la meme date. */
    MONEY_HEAVY(
        "mois_lourd", NudgeTone.SOFT, 41, 10, setOf(NudgeSurface.MONEY),
        subjects = listOf("dépens", "mois"), card = DayCard.MONEY,
    ),

    /** Plus rien de note dans l'argent depuis un moment. */
    MONEY_QUIET(
        "argent_silencieux", NudgeTone.SOFT, 34, 8, setOf(NudgeSurface.MONEY),
        subjects = listOf("argent", "mouvement"), card = DayCard.MONEY,
    ),

    /** Grosse depense notee aujourd'hui. */
    BIG_SPENDING(
        "grosse_depense", NudgeTone.SOFT, 36, 4, setOf(NudgeSurface.DAY, NudgeSurface.MONEY),
        subjects = listOf("dépense"), card = DayCard.MONEY,
    ),

    /** Mois plus calme que le precedent cote depenses. */
    CALM_MONEY(
        "mois_calme", NudgeTone.WARM, 28, 15, setOf(NudgeSurface.MONEY),
        subjects = listOf("dépens", "mois"), card = DayCard.MONEY,
    ),

    /** Rien d'envoye depuis un moment, alors que la recherche est en cours. */
    JOB_PAUSE(
        "pause_recherche", NudgeTone.SOFT, 27, 8, ANYWHERE,
        subjects = listOf("candidature"), card = DayCard.WORK,
    ),

    // --- Bilan : des lectures qu'on ne verrait pas seul ---------------------

    /** Le facteur le plus lie a ses bonnes journees, explique. */
    STATS_TOP_FACTOR(
        "bilan_facteur", NudgeTone.SOFT, 47, 9, setOf(NudgeSurface.STATS),
        subjects = listOf("journée"),
    ),

    /** Le moment de la journee qui est systematiquement le plus dur. */
    STATS_HARD_PART(
        "bilan_moment", NudgeTone.SOFT, 46, 12, setOf(NudgeSurface.STATS),
        subjects = listOf("matin", "après-midi", "soir", "nuit", "moment"),
    ),

    /** Le mois glissant est au-dessus du precedent. */
    STATS_TREND_UP(
        "bilan_tendance_haut", NudgeTone.PROUD, 49, 10, setOf(NudgeSurface.STATS),
        subjects = listOf("trente derniers jours", "mois"),
    ),

    /** Le mois glissant est en dessous du precedent. Dit sans alarmer. */
    STATS_TREND_DOWN(
        "bilan_tendance_bas", NudgeTone.CARE, 51, 10, setOf(NudgeSurface.STATS),
        subjects = listOf("trente derniers jours", "mois"),
    ),

    /** Pas encore assez de journees notees pour que le bilan veuille dire quelque chose. */
    STATS_YOUNG(
        "bilan_trop_jeune", NudgeTone.SOFT, 23, 14, setOf(NudgeSurface.STATS),
        subjects = listOf("journée"),
    ),

    // --- Remplir l'application ----------------------------------------------

    /** Des trous recents dans le calendrier. */
    GAPS(
        "trous", NudgeTone.SOFT, 25, 4, setOf(NudgeSurface.MONTH, NudgeSurface.YEAR),
        subjects = listOf("jour"),
    ),

    /** Rien ecrit dans le journal depuis un moment. */
    EMPTY_JOURNAL(
        "journal_vide", NudgeTone.SOFT, 22, 6, ANYWHERE,
        subjects = listOf("journal", "écri"), card = DayCard.JOURNAL,
    ),

    /** Couleur posee a la main, sans avoir rempli les moments. */
    EMPTY_PARTS(
        "moments_vides", NudgeTone.SOFT, 20, 3, setOf(NudgeSurface.DAY),
        subjects = listOf("moment"),
    ),

    // --- Petites choses -----------------------------------------------------

    /** Jour d'anniversaire. */
    BIRTHDAY(
        "anniversaire", NudgeTone.CHEER, 130, 300, ANYWHERE_AND_NOTIFY,
        subjects = listOf("anniversaire"), notificationTitle = "Joyeux anniversaire",
    ),

    /** Premier jour du mois. */
    NEW_MONTH(
        "nouveau_mois", NudgeTone.SOFT, 26, 25, setOf(NudgeSurface.MONTH, NudgeSurface.YEAR),
        subjects = listOf("mois"),
    ),

    /** Une photo ou une video ajoutee aujourd'hui. */
    PHOTO_ADDED(
        "photo", NudgeTone.WARM, 24, 3, setOf(NudgeSurface.DAY),
        subjects = listOf("photo", "image"), card = DayCard.MEDIA,
    ),

    /** Poids note regulierement ce mois-ci. */
    WEIGHT_TRACKED(
        "poids_suivi", NudgeTone.WARM, 18, 20, ANYWHERE,
        subjects = listOf("poids"), card = DayCard.HEALTH,
    ),

    /** Vendredi apres-midi : le week-end arrive. */
    WEEKEND(
        "week_end", NudgeTone.SOFT, 15, 6, ANYWHERE,
        subjects = listOf("week-end", "semaine"),
    );

    companion object {
        fun fromSlug(slug: String?): CoachRule? = entries.firstOrNull { it.slug == slug }
    }
}

/**
 * Une situation reconnue pour aujourd'hui, avec de quoi completer la phrase.
 *
 * @param values valeurs a injecter dans le message : `{n}`, `{quoi}`, etc.
 * @param boost ajustement de priorite quand la situation est plus ou moins
 *   marquee (par exemple quatre jours sans sortir plutot que deux).
 */
data class NudgeCandidate(
    val rule: CoachRule,
    val values: Map<String, String> = emptyMap(),
    val boost: Int = 0,
) {
    val score: Int get() = rule.priority + boost
}

/** Un message pret a apparaitre. */
data class Nudge(
    val rule: CoachRule,
    val text: String,
) {
    val tone: NudgeTone get() = rule.tone

    val title: String get() = rule.notificationTitle

    /** La fete a son propre traitement a l'ecran : confettis, son plus present. */
    val isCheer: Boolean get() = tone == NudgeTone.CHEER
}
