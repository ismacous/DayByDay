package com.ismael.daybyday.coach

import com.ismael.daybyday.data.DayCard

/**
 * Le "coup de pouce" : des petits messages de soutien, calcules a partir de ce
 * qui est coche dans les cartes. Aucune intelligence artificielle, aucun
 * reseau : des regles simples sur les donnees deja presentes, et un catalogue
 * de phrases ecrites a l'avance.
 *
 * Il lit ce que les cartes savent deja — la couleur et ses quatre moments, le
 * sommeil, la douche, les brossages, les traitements pris, les prieres, l'eau,
 * les pas, l'ecran, les candidatures, l'argent, les etiquettes. Il ne lit
 * **jamais** le journal, et il ne touche **jamais** a la couleur d'une journee.
 *
 * Tout ce qui se trouve dans ce fichier et dans les regles est du Kotlin pur,
 * sans dependance a Android : c'est ce qui permet de le tester sur machine
 * avec `./gradlew testDebugUnitTest`, sans emulateur.
 */

/** Ton d'un message : change le petit symbole et la couleur de la bulle. */
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
}

/** Les trois endroits ou un message peut apparaitre. */
enum class NudgeSurface {
    /** La carte en haut de l'onglet Mois. */
    HOME,

    /** La bulle en haut de l'ecran "Ma journee". */
    DAY,

    /** Une notification, au maximum une ou deux par jour. */
    NOTIFICATION,
}

/**
 * Une situation reconnue par l'algorithme.
 *
 * @param slug identifiant stable, utilise pour retenir ce qui a deja ete dit.
 *   Le renommer ferait reapparaitre des messages recemment vus : a eviter.
 * @param priority plus c'est haut, plus le message passe devant. Le soutien
 *   dans les moments durs passe toujours avant les propositions.
 * @param cooldownDays nombre de jours a attendre avant de reproposer cette
 *   situation, pour ne pas radoter.
 * @param surfaces ou cette situation a le droit de s'afficher.
 * @param notificationTitle titre court quand elle part en notification.
 * @param card la carte de "Ma journee" dont cette situation parle.
 *
 * [card] n'est pas decoratif : **une carte masquee fait taire ses regles**.
 * Masquer une carte ne doit rien couter — c'est deja vrai pour la note, ou
 * une carte masquee sort des deux cotes de la fraction, et ce serait absurde
 * qu'elle continue a parler ici. Quelqu'un qui masque « Prieres » ou
 * « Traitements » parce que ca ne le concerne pas ne doit plus jamais en
 * entendre parler.
 */
enum class CoachRule(
    val slug: String,
    val tone: NudgeTone,
    val priority: Int,
    val cooldownDays: Int,
    val surfaces: Set<NudgeSurface>,
    val notificationTitle: String = "Un petit mot",
    val card: DayCard? = null,
) {

    // --- Soutien quand c'est dur -----------------------------------------

    /** Plusieurs journees tres sombres de suite. Le seul message qui donne un numero. */
    DAYS_VERY_DARK(
        "jours_tres_sombres", NudgeTone.CARE, 120, 10,
        setOf(NudgeSurface.HOME, NudgeSurface.NOTIFICATION), "Je suis là",
    ),

    /** Deux journees noires ou plus a la suite. */
    BLACK_STREAK(
        "noires_de_suite", NudgeTone.CARE, 110, 3,
        setOf(NudgeSurface.HOME, NudgeSurface.DAY, NudgeSurface.NOTIFICATION), "Je suis là",
    ),

    /** La journee du jour vient d'etre notee en noir. */
    BLACK_DAY("journee_noire", NudgeTone.CARE, 100, 1, setOf(NudgeSurface.DAY, NudgeSurface.HOME)),

    /** Etiquette "Crise d'angoisse" cochee aujourd'hui. */
    ANXIETY("angoisse", NudgeTone.CARE, 96, 2, setOf(NudgeSurface.DAY)),

    /** Etiquette "J'ai pleure" cochee aujourd'hui. */
    CRIED("larmes", NudgeTone.CARE, 95, 2, setOf(NudgeSurface.DAY)),

    /** La journee du jour vient d'etre notee en rouge. */
    RED_DAY("journee_rouge", NudgeTone.CARE, 90, 1, setOf(NudgeSurface.DAY)),

    /** Semaine basse : moyenne des sept derniers jours sous 1,2 sur 3. */
    HARD_WEEK(
        "semaine_dure", NudgeTone.CARE, 80, 7,
        setOf(NudgeSurface.HOME, NudgeSurface.NOTIFICATION),
    ),

    // --- Sante et soin de soi --------------------------------------------

    /** Des traitements actifs, mais plus rien de coche depuis plusieurs jours. */
    MEDS_MISSING(
        "traitement_oublie", NudgeTone.CARE, 88, 2,
        setOf(NudgeSurface.HOME, NudgeSurface.NOTIFICATION), "Tes traitements", DayCard.TREATMENT,
    ),

    /** La douche laissee de cote plusieurs jours, alors qu'elle est suivie. */
    SHOWER_MISSING(
        "douche_oubliee", NudgeTone.NUDGE, 72, 3,
        setOf(NudgeSurface.HOME, NudgeSurface.NOTIFICATION), card = DayCard.HYGIENE,
    ),

    /** Plusieurs jours sans un seul brossage coche. */
    BRUSHING_LOW("brossage_oublie", NudgeTone.NUDGE, 60, 4, setOf(NudgeSurface.HOME), card = DayCard.HYGIENE),

    /** Des nuits nettement trop courtes, d'apres les heures saisies. */
    SHORT_NIGHTS("nuits_courtes", NudgeTone.NUDGE, 62, 5, setOf(NudgeSurface.HOME), card = DayCard.SLEEP),

    /** Plusieurs couchers tres tardifs de suite. */
    LATE_NIGHTS("couche_tard", NudgeTone.NUDGE, 40, 5, setOf(NudgeSurface.HOME), card = DayCard.SLEEP),

    /** Peu d'eau bue ces derniers jours, alors que le verre est suivi. */
    WATER_LOW("peu_d_eau", NudgeTone.NUDGE, 32, 4, setOf(NudgeSurface.HOME), card = DayCard.FOOD),

    // --- Ce qui remonte ---------------------------------------------------

    /** Premiere journee verte apres une longue serie sans. */
    FIRST_GREEN("premiere_verte", NudgeTone.PROUD, 85, 7, setOf(NudgeSurface.HOME, NudgeSurface.DAY)),

    /** Retour dans l'application apres plusieurs jours sans rien noter. */
    BACK_AFTER_BREAK("retour", NudgeTone.WARM, 78, 5, setOf(NudgeSurface.HOME)),

    /** Trois journees vertes d'affilee. */
    GREEN_STREAK("trois_vertes", NudgeTone.PROUD, 75, 4, setOf(NudgeSurface.HOME, NudgeSurface.DAY)),

    /** Aujourd'hui remonte nettement par rapport a hier. */
    REBOUND("remontee", NudgeTone.PROUD, 70, 1, setOf(NudgeSurface.DAY)),

    /** Palier de jours notes d'affilee (7, 14, 30, 50, 100...). */
    NOTING_STREAK(
        "serie_notee", NudgeTone.PROUD, 65, 5,
        setOf(NudgeSurface.HOME, NudgeSurface.NOTIFICATION),
    ),

    /** Les cinq prieres faites plusieurs jours de suite. */
    PRAYER_STREAK("serie_prieres", NudgeTone.PROUD, 64, 6, setOf(NudgeSurface.HOME, NudgeSurface.DAY), card = DayCard.PRAYER),

    /** Journee verte notee aujourd'hui. */
    GREEN_DAY("journee_verte", NudgeTone.WARM, 58, 1, setOf(NudgeSurface.DAY)),

    /** Des candidatures envoyees cette semaine. */
    JOB_EFFORT("candidatures", NudgeTone.PROUD, 57, 5, setOf(NudgeSurface.HOME, NudgeSurface.DAY), card = DayCard.WORK),

    /** Semaine en cours meilleure que la precedente. */
    BETTER_WEEK("semaine_meilleure", NudgeTone.PROUD, 55, 7, setOf(NudgeSurface.HOME)),

    /** Les cinq prieres faites aujourd'hui. */
    PRAYERS_ALL("cinq_prieres", NudgeTone.WARM, 54, 2, setOf(NudgeSurface.DAY), card = DayCard.PRAYER),

    /** Mois en cours au-dessus de tous les mois precedents. */
    BEST_MONTH("meilleur_mois", NudgeTone.PROUD, 50, 20, setOf(NudgeSurface.HOME)),

    // --- Ce qui va avec les bonnes journees -------------------------------

    /** Un facteur associe aux bonnes journees, absent aujourd'hui. */
    FACTOR_SUGGESTION(
        "facteur_positif", NudgeTone.NUDGE, 68, 2,
        setOf(NudgeSurface.HOME, NudgeSurface.NOTIFICATION),
    ),

    /** Ce facteur est la aujourd'hui : on le souligne. */
    FACTOR_TODAY("facteur_present", NudgeTone.WARM, 52, 3, setOf(NudgeSurface.DAY)),

    /** Un facteur qui accompagne souvent les journees plus dures. Descriptif. */
    FACTOR_HEAVY("facteur_lourd", NudgeTone.SOFT, 38, 5, setOf(NudgeSurface.DAY)),

    // --- Habitudes du quotidien -------------------------------------------

    /** Plusieurs jours d'affilee sans personne de note. */
    ALONE_STREAK(
        "personne", NudgeTone.NUDGE, 56, 4,
        setOf(NudgeSurface.HOME, NudgeSurface.NOTIFICATION), card = DayCard.SOCIAL,
    ),

    /** Plusieurs jours d'affilee sans sortir. */
    STAYED_IN(
        "pas_sorti", NudgeTone.NUDGE, 48, 3,
        setOf(NudgeSurface.HOME, NudgeSurface.NOTIFICATION), card = DayCard.ACTIVITY,
    ),

    /** Plusieurs jours d'affilee sans rien bouger. */
    NO_MOVEMENT(
        "pas_bouge", NudgeTone.NUDGE, 45, 4,
        setOf(NudgeSurface.HOME, NudgeSurface.NOTIFICATION), card = DayCard.ACTIVITY,
    ),

    /** Etiquette "Mal dormi" cochee plusieurs fois de suite. */
    BAD_SLEEP("mal_dormi", NudgeTone.NUDGE, 42, 5, setOf(NudgeSurface.HOME), card = DayCard.SLEEP),

    /** Temps d'ecran nettement au-dessus de l'habitude. */
    HIGH_SCREEN("ecran_haut", NudgeTone.SOFT, 35, 4, setOf(NudgeSurface.HOME), card = DayCard.OUTSIDE),

    /** Tres peu de pas alors que la journee est deja bien avancee. */
    LOW_STEPS("peu_de_pas", NudgeTone.NUDGE, 30, 3, setOf(NudgeSurface.HOME), card = DayCard.ACTIVITY),

    // --- Argent et travail ------------------------------------------------

    /** Grosse depense notee aujourd'hui. */
    BIG_SPENDING("grosse_depense", NudgeTone.SOFT, 36, 4, setOf(NudgeSurface.DAY), card = DayCard.MONEY),

    /** Mois plus calme que le precedent cote depenses. */
    CALM_MONEY("mois_calme", NudgeTone.WARM, 28, 15, setOf(NudgeSurface.HOME), card = DayCard.MONEY),

    /** Rien d'envoye depuis un moment, alors que la recherche est en cours. */
    JOB_PAUSE("pause_recherche", NudgeTone.SOFT, 27, 8, setOf(NudgeSurface.HOME), card = DayCard.WORK),

    // --- Remplir l'application --------------------------------------------

    /** Des trous recents dans le calendrier. */
    GAPS("trous", NudgeTone.SOFT, 25, 4, setOf(NudgeSurface.HOME)),

    /** Rien ecrit dans le journal depuis un moment. */
    EMPTY_JOURNAL("journal_vide", NudgeTone.SOFT, 22, 6, setOf(NudgeSurface.HOME), card = DayCard.JOURNAL),

    /** Couleur posee a la main, sans avoir rempli les moments. */
    EMPTY_PARTS("moments_vides", NudgeTone.SOFT, 20, 3, setOf(NudgeSurface.DAY)),

    // --- Petites choses ---------------------------------------------------

    /** Jour d'anniversaire. */
    BIRTHDAY(
        "anniversaire", NudgeTone.WARM, 130, 300,
        setOf(NudgeSurface.HOME, NudgeSurface.NOTIFICATION), "Joyeux anniversaire",
    ),

    /** Premier jour du mois. */
    NEW_MONTH("nouveau_mois", NudgeTone.SOFT, 26, 25, setOf(NudgeSurface.HOME)),

    /** Une photo ou une video ajoutee aujourd'hui. */
    PHOTO_ADDED("photo", NudgeTone.WARM, 24, 3, setOf(NudgeSurface.DAY), card = DayCard.MEDIA),

    /** Poids note regulierement ce mois-ci. */
    WEIGHT_TRACKED("poids_suivi", NudgeTone.WARM, 18, 20, setOf(NudgeSurface.HOME), card = DayCard.HEALTH),

    /** Vendredi apres-midi : le week-end arrive. */
    WEEKEND("week_end", NudgeTone.SOFT, 15, 6, setOf(NudgeSurface.HOME)),

    /** Rien de particulier a signaler : un mot au hasard, juste pour etre la. */
    HELLO("bonjour", NudgeTone.SOFT, 5, 3, setOf(NudgeSurface.HOME));

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

/** Un message pret a afficher. */
data class Nudge(
    val rule: CoachRule,
    val text: String,
) {
    val tone: NudgeTone get() = rule.tone

    val title: String get() = rule.notificationTitle
}
