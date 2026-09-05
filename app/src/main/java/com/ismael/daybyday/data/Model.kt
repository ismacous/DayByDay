package com.ismael.daybyday.data

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Les quatre couleurs disponibles pour qualifier une journee.
 * [score] sert a calculer les moyennes par semaine / mois / annee.
 */
enum class DayColor(val key: Int, val score: Int, val label: String, val color: Color) {
    GREEN(3, 3, "Bonne journée", Color(0xFF3FBF6A)),
    ORANGE(2, 2, "Journée mitigée", Color(0xFFF2A93B)),
    RED(1, 1, "Journée difficile", Color(0xFFE1483F)),
    BLACK(0, 0, "Journée très noire", Color(0xFF15171D));

    companion object {
        val MAX_SCORE = GREEN.score

        fun fromKey(key: Int?): DayColor? = entries.firstOrNull { it.key == key }

        /** Couleur representant une moyenne (0..3), utilisee pour les resumes. */
        fun fromAverage(average: Double): Color {
            val clamped = average.coerceIn(0.0, MAX_SCORE.toDouble())
            val stops = listOf(
                0.0 to BLACK.color,
                1.0 to RED.color,
                2.0 to ORANGE.color,
                3.0 to GREEN.color,
            )
            for (i in 0 until stops.size - 1) {
                val (lowValue, lowColor) = stops[i]
                val (highValue, highColor) = stops[i + 1]
                if (clamped <= highValue) {
                    val t = ((clamped - lowValue) / (highValue - lowValue)).toFloat()
                    return lerpColor(lowColor, highColor, t)
                }
            }
            return GREEN.color
        }

        private fun lerpColor(from: Color, to: Color, t: Float): Color = Color(
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
) {
    val color: DayColor? get() = DayColor.fromKey(colorKey)

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
) {
    val kind: MediaKind get() = if (kindKey == 1) MediaKind.VIDEO else MediaKind.PHOTO
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
