package com.ismael.daybyday.data

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Les quatre couleurs disponibles pour qualifier une journee.
 * [score] sert a calculer les moyennes par semaine / mois / annee.
 */
enum class DayColor(
    val key: Int,
    val score: Int,
    val label: String,
    val color: Color,
    /** La teinte claire du degrade : la couleur va vers elle, jamais vers le blanc. */
    val light: Color,
) {
    // Les quatre teintes ont ete accordees a l'application : meme saturation,
    // meme fraicheur, et chacune un degrade plutot qu'un aplat. Le "tres noire"
    // n'est plus un noir pur mais un indigo tres sombre — un noir franc fait un
    // trou dans une interface coloree, alors qu'un indigo profond dit la meme
    // chose en restant de la famille.
    //
    // La cle et le score ne bougent pas : ce sont eux qui sont enregistres, et
    // toutes les journees deja notees les utilisent.
    GREEN(3, 3, "Bonne journée", Color(0xFF15C48E), Color(0xFF5BE3B4)),
    ORANGE(2, 2, "Journée mitigée", Color(0xFFFFA92E), Color(0xFFFFCB6B)),
    RED(1, 1, "Journée difficile", Color(0xFFFF5D6E), Color(0xFFFF95A3)),
    BLACK(0, 0, "Journée très noire", Color(0xFF322C5C), Color(0xFF4E4682));

    /** Le degrade de la journee, du plus profond au plus clair. */
    val gradient: List<Color> get() = listOf(color, light)

    companion object {
        val MAX_SCORE = GREEN.score

        fun fromKey(key: Int?): DayColor? = entries.firstOrNull { it.key == key }

        /**
         * Couleur representant une moyenne (0..3), utilisee pour les resumes.
         *
         * Deux essais ont echoue avant celui-ci, et ils disent pourquoi la
         * regle est ce qu'elle est. Melanger deux couleurs canal par canal
         * donne un kaki terne au milieu ; les melanger par la teinte donne un
         * vert-jaune fluo, qui a l'air **meilleur** que le vert de la bonne
         * journee — une moyenne de 2,5 avait l'air d'un 10 sur 10.
         *
         * On ne fabrique donc plus de couleur intermediaire. La moyenne prend
         * la couleur de la journee **la plus proche**, simplement eclaircie a
         * mesure qu'elle s'en eloigne. Une moyenne reste toujours dans sa
         * famille, ne depasse jamais la couleur qu'elle approche, et le chiffre
         * exact est de toute facon ecrit a cote.
         */
        fun fromAverage(average: Double): Color {
            val clamped = average.coerceIn(0.0, MAX_SCORE.toDouble())
            val nearest = entries.minByOrNull { kotlin.math.abs(it.score - clamped) } ?: GREEN
            // A mi-chemin entre deux notes, la couleur est a moitie eclaircie.
            val distance = kotlin.math.abs(nearest.score - clamped).toFloat()
            return blend(nearest.color, nearest.light, distance.coerceIn(0f, 0.5f))
        }

        private fun blend(from: Color, to: Color, t: Float): Color = Color(
            red = from.red + (to.red - from.red) * t,
            green = from.green + (to.green - from.green) * t,
            blue = from.blue + (to.blue - from.blue) * t,
            alpha = 1f,
        )

    }
}

/** Niveau d'activite physique de la journee. */
enum class SportLevel(val key: Int, val label: String, val emoji: String) {
    NONE(0, "Rien bougé", "😴"),
    LIGHT(1, "Un peu bougé", "🚶"),
    GOOD(2, "Vraie séance", "💪");

    companion object {
        fun fromKey(key: Int?): SportLevel? = entries.firstOrNull { it.key == key }
    }
}

/** Ressenti sur l'alimentation de la journee. */
enum class FoodLevel(val key: Int, val label: String, val emoji: String) {
    HARD(0, "Compliquée", "🍔"),
    OK(1, "Correcte", "🍽️"),
    GOOD(2, "Bien mangé", "🥗");

    companion object {
        fun fromKey(key: Int?): FoodLevel? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Les cinq prieres du jour, dans leur ordre.
 *
 * Elles sont enregistrees en **masque de bits** dans une seule colonne
 * ([DayEntry.prayerMask]) plutot qu'en cinq colonnes ou cinq lignes : c'est
 * cinq oui-ou-non par journee, et une seule valeur suffit a les porter. Le
 * [bit] ne doit jamais changer — c'est lui qui est ecrit dans la base.
 */
enum class Prayer(val key: Int, val label: String) {
    FAJR(0, "Fajr"),
    DHUHR(1, "Dhuhr"),
    ASR(2, "Asr"),
    MAGHRIB(3, "Maghrib"),
    ISHA(4, "Isha");

    val bit: Int get() = 1 shl key

    companion object {
        /** Toutes faites : les cinq bits a un. */
        val ALL_DONE: Int = entries.fold(0) { mask, prayer -> mask or prayer.bit }
    }
}

/** Les quatre moments d'une journee, pour nuancer une humeur qui bouge. */
enum class DayPart(val key: Int, val label: String, val emoji: String) {
    MORNING(0, "Matin", "🌅"),
    AFTERNOON(1, "Après-midi", "☀️"),
    EVENING(2, "Soir", "🌆"),
    NIGHT(3, "Nuit", "🌙"),
}

/** Familles d'etiquettes, pour que la liste reste rangee au lieu d'etre en vrac. */
enum class TagCategory(val key: String, val label: String) {
    SLEEP("sommeil", "Sommeil"),
    SOCIAL("social", "Social"),
    ACTIVITY("activite", "Activité"),
    FOOD("alimentation", "Alimentation"),
    WORK("travail", "Travail & démarches"),
    SCREENS("ecrans", "Écrans"),
    HEALTH("sante", "Santé"),
    MONEY("argent", "Argent"),
    OTHER("autre", "Autre");

    companion object {
        fun fromKey(key: String?): TagCategory = entries.firstOrNull { it.key == key } ?: OTHER
    }
}

/**
 * Une journee du calendrier. La cle primaire est le numero de jour epoch
 * (LocalDate.toEpochDay) : simple a trier et a interroger par plage.
 * Tous les champs de suivi sont nullables : "non renseigne" est une reponse.
 */
@Entity(tableName = "day_entries")
data class DayEntry(
    @PrimaryKey val epochDay: Long,
    val colorKey: Int? = null,
    val title: String = "",
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val sportLevel: Int? = null,
    val foodLevel: Int? = null,
    val wentOut: Boolean? = null,
    val weightKg: Double? = null,
    val partMorning: Int? = null,
    val partAfternoon: Int? = null,
    val partEvening: Int? = null,
    val partNight: Int? = null,
    /** true si la couleur du jour a ete choisie a la main plutot que calculee. */
    val colorManual: Boolean? = null,
    /** Pas du jour, lus depuis Health Connect quand l'acces est accorde. */
    val steps: Int? = null,
    /** Minutes passees sur les applications ce jour-la. */
    val screenMinutes: Int? = null,
    /**
     * Heure du coucher et du lever, en minutes depuis minuit. Le coucher est
     * souvent la veille au soir : quand la fin est plus petite que le debut,
     * la nuit a simplement passe minuit. La nuit est rattachee au jour du
     * reveil, celui qu'elle a reellement fait.
     */
    val sleepStartMinutes: Int? = null,
    val sleepEndMinutes: Int? = null,
    /** true si la nuit vient du telephone (montre, Health Connect) et non de la saisie. */
    val sleepFromDevice: Boolean? = null,
    /** Verres d'eau bus dans la journee. */
    val waterGlasses: Int? = null,
    /** Ce qui a ete mange, en texte libre. */
    val mealsNote: String = "",
    /**
     * Mise en forme du journal, au format compact de [RichText]. Le texte lui
     * meme reste brut dans [note] : la recherche et l'export continuent de le
     * lire sans rien savoir de la decoration.
     */
    val noteSpans: String = "",
    /**
     * Les prieres faites dans la journee, en masque de bits (voir [Prayer]).
     * `null` tant que la carte n'a pas ete touchee : une journee ou l'on n'a
     * rien coche n'est pas une journee ou l'on n'a rien fait, c'est une journee
     * dont on ne sait rien.
     */
    val prayerMask: Int? = null,
) {
    val color: DayColor? get() = DayColor.fromKey(colorKey)

    /** Cette priere est-elle cochee ? */
    fun isPrayerDone(prayer: Prayer): Boolean = (prayerMask ?: 0) and prayer.bit != 0

    /** Le masque une fois [prayer] cochee ou decochee. */
    fun withPrayer(prayer: Prayer, done: Boolean): Int {
        val current = prayerMask ?: 0
        return if (done) current or prayer.bit else current and prayer.bit.inv()
    }

    val prayersDone: Int get() = Prayer.entries.count { isPrayerDone(it) }

    val sport: SportLevel? get() = SportLevel.fromKey(sportLevel)

    val food: FoodLevel? get() = FoodLevel.fromKey(foodLevel)

    fun partColorKey(part: DayPart): Int? = when (part) {
        DayPart.MORNING -> partMorning
        DayPart.AFTERNOON -> partAfternoon
        DayPart.EVENING -> partEvening
        DayPart.NIGHT -> partNight
    }

    fun partColor(part: DayPart): DayColor? = DayColor.fromKey(partColorKey(part))

    fun withPartColor(part: DayPart, colorKey: Int?): DayEntry = when (part) {
        DayPart.MORNING -> copy(partMorning = colorKey)
        DayPart.AFTERNOON -> copy(partAfternoon = colorKey)
        DayPart.EVENING -> copy(partEvening = colorKey)
        DayPart.NIGHT -> copy(partNight = colorKey)
    }

    val filledParts: List<DayColor>
        get() = DayPart.entries.mapNotNull { partColor(it) }

    /** Couleur moyenne des moments notes, arrondie au plus proche. */
    val averagePartColor: DayColor?
        get() {
            val parts = filledParts
            if (parts.isEmpty()) return null
            val average = parts.sumOf { it.score }.toDouble() / parts.size
            return DayColor.entries.minByOrNull { kotlin.math.abs(it.score - average) }
        }

    /** Duree de la nuit en minutes, en tenant compte du passage de minuit. */
    val sleepMinutes: Int?
        get() {
            val start = sleepStartMinutes ?: return null
            val end = sleepEndMinutes ?: return null
            val length = if (end >= start) end - start else end + MINUTES_PER_DAY - start
            return if (length in 1 until MINUTES_PER_DAY) length else null
        }

    val isEmpty: Boolean
        get() = colorKey == null && title.isBlank() && note.isBlank() &&
            sportLevel == null && foodLevel == null && wentOut == null && weightKg == null &&
            sleepStartMinutes == null && sleepEndMinutes == null &&
            waterGlasses == null && mealsNote.isBlank() &&
            (prayerMask ?: 0) == 0 &&
            filledParts.isEmpty()

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
    }
}

/** Moment de prise d'un traitement dans la journee. */
enum class DoseTime(val key: Int, val label: String, val emoji: String) {
    MORNING(0, "Matin", "🌅"),
    NOON(1, "Midi", "☀️"),
    EVENING(2, "Soir", "🌆"),
    NIGHT(3, "Nuit", "🌙");

    /** Bit de ce moment dans le masque d'un traitement. */
    val bit: Int get() = 1 shl key

    companion object {
        fun fromKey(key: Int?): DoseTime? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Un traitement a prendre. Les moments de prise sont ranges dans un masque de
 * bits pour tenir dans une colonne, sans table supplementaire.
 */
@Entity(tableName = "treatments")
data class Treatment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dose: String = "",
    val timesMask: Int = DoseTime.MORNING.bit,
    val active: Boolean = true,
    val sortOrder: Int = 0,
) {
    fun isDueAt(time: DoseTime): Boolean = timesMask and time.bit != 0

    val times: List<DoseTime> get() = DoseTime.entries.filter { isDueAt(it) }
}

/** Une prise cochee : l'absence de ligne veut dire "pas encore pris". */
@Entity(
    tableName = "doses_taken",
    primaryKeys = ["epochDay", "treatmentId", "timeKey"],
    indices = [Index("epochDay"), Index("treatmentId")],
)
data class DoseTaken(
    val epochDay: Long,
    val treatmentId: Long,
    val timeKey: Int,
    val takenAt: Long = System.currentTimeMillis(),
)

/** Etiquette personnalisable, attachable a autant de journees que voulu. */
@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "",
    val sortOrder: Int = 0,
    val category: String? = null,
    /** Identifiant stable d'une etiquette fournie avec l'application. */
    val slug: String? = null,
) {
    val display: String get() = if (emoji.isBlank()) name else "$emoji $name"

    val group: TagCategory get() = TagCategory.fromKey(category)
}

/** Association entre une journee et une etiquette. */
@Entity(
    tableName = "day_tags",
    primaryKeys = ["epochDay", "tagId"],
    indices = [Index("tagId")],
)
data class DayTagCrossRef(
    val epochDay: Long,
    val tagId: Long,
)

enum class MediaKind { PHOTO, VIDEO }

/** Une photo ou video rattachee a une journee, stockee dans le dossier prive de l'app. */
@Entity(
    tableName = "media_items",
    indices = [Index("epochDay")],
)
data class MediaItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    /** Chemin relatif au dossier "media" du stockage interne. */
    val relativePath: String,
    val kindKey: Int,
    val addedAt: Long = System.currentTimeMillis(),
    /**
     * Cle de la carte a laquelle ce media est rattache, ou null quand il
     * appartient simplement a la journee. Une photo de repas s'affiche ainsi
     * sous "Alimentation", sans quitter l'album complet de la journee.
     */
    val cardKey: String? = null,
    /**
     * Placement libre sur la page du journal : coin haut gauche, en points,
     * depuis le haut de la page — pas de l'ecran, sinon tout se decalerait au
     * premier defilement.
     *
     * `null` veut dire "jamais posee" : la photo sera rangee automatiquement a
     * la prochaine ouverture du journal. C'est le cas des photos ajoutees
     * avant que le placement libre existe.
     */
    val placedX: Float? = null,
    val placedY: Float? = null,
    val placedWidth: Float = 0f,
    val placedHeight: Float = 0f,
    /** Inclinaison en degres. Une photo posee de travers, comme sur un carnet. */
    val placedRotation: Float = 0f,
    /** Voir [MediaLayer] : devant ou derriere le texte. */
    val layerKey: Int = MediaLayer.FRONT.key,
    /** Voir [MediaShape] : la forme dans laquelle l'image vient se ranger. */
    val shapeKey: Int = MediaShape.RECTANGLE.key,
    /**
     * Le contour blanc des autocollants. Il suit la silhouette de l'image, pas
     * son cadre : sur un PNG detoure, c'est ce qui fait le sticker.
     */
    val stickerOutline: Boolean = false,
) {
    val kind: MediaKind get() = if (kindKey == 1) MediaKind.VIDEO else MediaKind.PHOTO

    val layer: MediaLayer get() = MediaLayer.fromKey(layerKey)

    val shape: MediaShape get() = MediaShape.fromKey(shapeKey)

    /** Posee sur la page, avec une position et une taille connues. */
    val isPlaced: Boolean
        get() = placedX != null && placedY != null && placedWidth > 0f && placedHeight > 0f

    /** Le cercle et le carre sont aussi hauts que larges, quoi qu'on enregistre. */
    val displayHeight: Float
        get() = if (shape.square) placedWidth else placedHeight
}

/**
 * A quelle profondeur une photo est posee sur la page.
 *
 * Le texte s'ecrit entre [MIDDLE] et [FRONT] : une photo de fond se laisse
 * ecrire par-dessus, une photo devant recouvre le texte. C'est ce qui permet
 * une mise en page, au lieu d'une simple suite d'images.
 */
enum class MediaLayer(val key: Int, val label: String) {
    /** Le plus loin : sous le texte, et sous les photos "au milieu". */
    BACK(0, "Au fond"),

    /** Sous le texte, mais par-dessus celles qui sont au fond. */
    MIDDLE(1, "Au milieu"),

    /** Par-dessus le texte, qu'elle recouvre. */
    FRONT(2, "Devant le texte");

    companion object {
        fun fromKey(key: Int): MediaLayer = entries.firstOrNull { it.key == key } ?: FRONT
    }
}

/**
 * La forme dans laquelle l'image vient se ranger, en la recadrant au centre.
 *
 * [FREE] est a part : elle ne recadre rien et ne coupe rien. C'est la forme des
 * autocollants — un PNG detoure garde sa transparence et sa silhouette, au lieu
 * d'etre force dans un cadre.
 */
enum class MediaShape(val key: Int, val label: String, val square: Boolean = false) {
    RECTANGLE(0, "Rectangle"),
    SQUARE(1, "Carré", square = true),
    CIRCLE(2, "Cercle", square = true),
    FREE(3, "Autocollant");

    companion object {
        fun fromKey(key: Int): MediaShape = entries.firstOrNull { it.key == key } ?: RECTANGLE
    }
}

/** Nombre de medias par jour, pour afficher une pastille dans le calendrier. */
data class DayMediaCount(val epochDay: Long, val count: Int)

/** Poids releve un jour donne, pour la courbe de suivi. */
data class WeightPoint(val epochDay: Long, val weightKg: Double)

/** Contenu reel de la base, affiche dans « A propos » des reglages. */
data class DatabaseContents(
    val days: Int,
    val moneyEntries: Int,
    val mediaFiles: Int,
    val taggedDays: Int,
    val mediaBytes: Long,
)

/**
 * Categories de mouvements d'argent.
 *
 * ADJUSTMENT n'est pas proposee a la saisie : elle marque les corrections de
 * solde, qui remettent le compte a la bonne valeur sans etre ni une vraie
 * rentree ni une vraie depense. Les melanger au reste faisait apparaitre une
 * correction de 14,90 comme un gain de 14,90.
 */
enum class MoneyCategory(val key: String, val label: String, val emoji: String, val isIncome: Boolean) {
    SALARY("salaire", "Salaire / aides", "💶", true),
    GIFT("aide", "Aide, remboursement", "🎁", true),
    OTHER_IN("autre_gain", "Autre rentrée", "➕", true),
    FOOD("courses", "Courses & repas", "🛒", false),
    HOME("logement", "Logement, factures", "🏠", false),
    TRANSPORT("transport", "Transport", "🚌", false),
    SUBSCRIPTION("abonnement", "Abonnements", "🔁", false),
    HEALTH("sante", "Santé", "💊", false),
    FUN("loisirs", "Loisirs, sorties", "🎮", false),
    OTHER_OUT("autre_depense", "Autre dépense", "➖", false),
    ADJUSTMENT("ajustement", "Correction du solde", "⚖️", false);

    companion object {
        /** Libelle historique des corrections, avant la categorie dediee. */
        const val ADJUSTMENT_LABEL = "Ajustement du solde"

        fun fromKey(key: String?): MoneyCategory? = entries.firstOrNull { it.key == key }

        fun incomes(): List<MoneyCategory> = entries.filter { it.isIncome && it != ADJUSTMENT }

        fun expenses(): List<MoneyCategory> = entries.filter { !it.isIncome && it != ADJUSTMENT }
    }
}

/**
 * Un mouvement d'argent. Le montant est en centimes et signe :
 * positif pour une rentree, negatif pour une depense.
 */
@Entity(
    tableName = "transactions",
    indices = [Index("epochDay")],
)
data class MoneyEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val amountCents: Long,
    val label: String = "",
    val categoryKey: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val isIncome: Boolean get() = amountCents >= 0

    val category: MoneyCategory? get() = MoneyCategory.fromKey(categoryKey)

    /** Correction de solde : ni une rentree ni une depense, juste un recalage. */
    val isAdjustment: Boolean get() = category == MoneyCategory.ADJUSTMENT

    /** Ce qui s'affiche dans une liste quand aucun libelle n'a ete saisi. */
    val displayLabel: String
        get() = label.ifBlank { category?.label ?: "Mouvement" }
}
