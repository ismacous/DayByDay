package com.ismael.daybyday.data

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import java.time.LocalDate
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Preferences locales : profil, verrouillage, rappel et sauvegarde
 * automatique. Le code PIN n'est jamais stocke en clair, seul un hash PBKDF2
 * sale est conserve.
 */
class Prefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("daybyday_prefs", Context.MODE_PRIVATE)

    // --- Profil -----------------------------------------------------------

    var firstName: String
        get() = prefs.getString(KEY_FIRST_NAME, "Ismael").orEmpty()
        set(value) = prefs.edit().putString(KEY_FIRST_NAME, value.trim()).apply()

    var heightCm: Int
        get() = prefs.getInt(KEY_HEIGHT_CM, 178)
        set(value) = prefs.edit().putInt(KEY_HEIGHT_CM, value).apply()

    var birthDate: LocalDate
        get() = LocalDate.ofEpochDay(
            prefs.getLong(KEY_BIRTH_DATE, LocalDate.of(1999, 6, 21).toEpochDay())
        )
        set(value) = prefs.edit().putLong(KEY_BIRTH_DATE, value.toEpochDay()).apply()

    /** IMC calcule a partir de la taille du profil, ou null si le poids manque. */
    fun bodyMassIndex(weightKg: Double?): Double? {
        val height = heightCm / 100.0
        if (weightKg == null || height <= 0.0) return null
        return weightKg / (height * height)
    }

    // --- Apparence de la page du journal ----------------------------------

    /** Les lignes d'ecriture sont dessinees derriere le texte. */
    var journalRuled: Boolean
        get() = prefs.getBoolean(KEY_JOURNAL_RULED, true)
        set(value) = prefs.edit().putBoolean(KEY_JOURNAL_RULED, value).apply()

    /**
     * Couleur du papier et couleur des lignes, en index dans les palettes de
     * [JournalPaper]. Un index, pas une couleur brute : le theme sombre a
     * besoin de ses propres teintes, et un index survit au changement de theme.
     */
    var journalPaperIndex: Int
        get() = prefs.getInt(KEY_JOURNAL_PAPER, 0)
        set(value) = prefs.edit().putInt(KEY_JOURNAL_PAPER, value).apply()

    var journalLineIndex: Int
        get() = prefs.getInt(KEY_JOURNAL_LINES, 0)
        set(value) = prefs.edit().putInt(KEY_JOURNAL_LINES, value).apply()

    /** L'aimantation des photos sur la grille, gardee d'une page a l'autre. */
    var journalSnapToGrid: Boolean
        get() = prefs.getBoolean(KEY_JOURNAL_SNAP, true)
        set(value) = prefs.edit().putBoolean(KEY_JOURNAL_SNAP, value).apply()

    // --- Disposition de l'ecran "Ma journee" ------------------------------

    /**
     * Ordre choisi pour les cartes. Les cles inconnues sont ignorees et les
     * cartes jamais vues sont ajoutees a leur place d'origine, donc une mise a
     * jour qui apporte une carte ne casse pas la disposition existante.
     */
    var dayCardOrder: List<DayCard>
        get() = DayCard.order(readKeys(KEY_DAY_CARD_ORDER))
        set(value) = writeKeys(KEY_DAY_CARD_ORDER, value.map { it.key })

    /** Cartes masquees. Une carte essentielle ne peut pas l'etre. */
    var hiddenDayCards: Set<DayCard>
        get() = readKeys(KEY_DAY_CARDS_HIDDEN)
            .mapNotNull { DayCard.fromKey(it) }
            .filterNot { it.essential }
            .toSet()
        set(value) = writeKeys(KEY_DAY_CARDS_HIDDEN, value.filterNot { it.essential }.map { it.key })

    /** Cartes repliees : le titre reste, le contenu est cache. */
    var collapsedDayCards: Set<DayCard>
        get() = readKeys(KEY_DAY_CARDS_COLLAPSED).mapNotNull { DayCard.fromKey(it) }.toSet()
        set(value) = writeKeys(KEY_DAY_CARDS_COLLAPSED, value.map { it.key })

    /** Les cartes reellement affichees, dans l'ordre. */
    val visibleDayCards: List<DayCard>
        get() {
            val hidden = hiddenDayCards
            return dayCardOrder.filterNot { it in hidden }
        }

    private fun readKeys(key: String): List<String> =
        prefs.getString(key, null)
            ?.split(SEPARATOR)
            ?.filter { it.isNotBlank() }
            .orEmpty()

    private fun writeKeys(key: String, keys: List<String>) {
        prefs.edit().putString(key, keys.joinToString(SEPARATOR)).apply()
    }

    // --- Verrouillage -----------------------------------------------------

    var lockEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_ENABLED, value).apply()

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()

    /**
     * Le blocage des captures est actif par defaut et ne peut etre desactive
     * qu'une fois un code defini : sans code, l'ecran reste protege.
     */
    var blockScreenshots: Boolean
        get() = if (!hasPin) true else prefs.getBoolean(KEY_SECURE_SCREEN, true)
        set(value) {
            if (hasPin) prefs.edit().putBoolean(KEY_SECURE_SCREEN, value).apply()
        }

    /** Vrai quand l'utilisateur peut choisir lui-meme d'autoriser les captures. */
    val canToggleScreenshots: Boolean get() = hasPin

    val hasPin: Boolean get() = prefs.getString(KEY_PIN_HASH, null) != null

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hash(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun clearPin() {
        prefs.edit().remove(KEY_PIN_SALT).remove(KEY_PIN_HASH).apply()
    }

    fun checkPin(pin: String): Boolean {
        val saltEncoded = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val hashEncoded = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = Base64.decode(saltEncoded, Base64.NO_WRAP)
        val expected = Base64.decode(hashEncoded, Base64.NO_WRAP)
        return constantTimeEquals(expected, hash(pin, salt))
    }

    // --- Rappel quotidien -------------------------------------------------

    var reminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMINDER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_REMINDER_ENABLED, value).apply()

    var reminderHour: Int
        get() = prefs.getInt(KEY_REMINDER_HOUR, 21)
        set(value) = prefs.edit().putInt(KEY_REMINDER_HOUR, value).apply()

    var reminderMinute: Int
        get() = prefs.getInt(KEY_REMINDER_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_REMINDER_MINUTE, value).apply()

    /**
     * L'heure pour laquelle le rappel est **deja** programme, ou `null` s'il ne
     * l'est pas. Elle sert a ne pas reprogrammer la tache a chaque ouverture de
     * l'application : reprogrammer, c'est repartir de zero, et un rappel du soir
     * qu'on repousse chaque matin n'arrive jamais.
     */
    var scheduledReminder: String?
        get() = prefs.getString(KEY_SCHEDULED_REMINDER, null)
        set(value) = prefs.edit().putString(KEY_SCHEDULED_REMINDER, value).apply()

    /** Meme chose pour la sauvegarde automatique. */
    var scheduledBackup: String?
        get() = prefs.getString(KEY_SCHEDULED_BACKUP, null)
        set(value) = prefs.edit().putString(KEY_SCHEDULED_BACKUP, value).apply()

    /** Quand le dernier rappel a ete envoye. Zero tant qu'il n'y en a pas eu. */
    var lastReminderAt: Long
        get() = prefs.getLong(KEY_LAST_REMINDER, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_REMINDER, value).apply()

    // --- Sauvegarde automatique -------------------------------------------

    var autoBackupEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_BACKUP, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_BACKUP, value).apply()

    var autoBackupHour: Int
        get() = prefs.getInt(KEY_AUTO_BACKUP_HOUR, 23)
        set(value) = prefs.edit().putInt(KEY_AUTO_BACKUP_HOUR, value).apply()

    var autoBackupMinute: Int
        get() = prefs.getInt(KEY_AUTO_BACKUP_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_AUTO_BACKUP_MINUTE, value).apply()

    var autoBackupFolder: String?
        get() = prefs.getString(KEY_AUTO_BACKUP_FOLDER, null)
        set(value) = prefs.edit().putString(KEY_AUTO_BACKUP_FOLDER, value).apply()

    var lastAutoBackupAt: Long
        get() = prefs.getLong(KEY_AUTO_BACKUP_LAST, 0L)
        set(value) = prefs.edit().putLong(KEY_AUTO_BACKUP_LAST, value).apply()

    /** Passe a true des qu'on a propose (ou fait) une reprise de sauvegarde. */
    var firstRunRestoreChecked: Boolean
        get() = prefs.getBoolean(KEY_FIRST_RUN_RESTORE, false)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_RUN_RESTORE, value).apply()

    var lastAutoBackupError: String?
        get() = prefs.getString(KEY_AUTO_BACKUP_ERROR, null)
        set(value) = prefs.edit().putString(KEY_AUTO_BACKUP_ERROR, value).apply()

    // --- Interne ----------------------------------------------------------

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].toInt() xor b[i].toInt())
        return result == 0
    }

    private companion object {
        const val KEY_FIRST_NAME = "first_name"
        const val KEY_HEIGHT_CM = "height_cm"
        const val KEY_BIRTH_DATE = "birth_date"
        const val KEY_LOCK_ENABLED = "lock_enabled"
        const val KEY_BIOMETRIC = "biometric_enabled"
        const val KEY_SECURE_SCREEN = "secure_screen"
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_PIN_SALT = "pin_salt"
        const val KEY_REMINDER_ENABLED = "reminder_enabled"
        const val KEY_REMINDER_HOUR = "reminder_hour"
        const val KEY_REMINDER_MINUTE = "reminder_minute"
        const val KEY_SCHEDULED_REMINDER = "reminder_scheduled_for"
        const val KEY_SCHEDULED_BACKUP = "backup_scheduled_for"
        const val KEY_LAST_REMINDER = "reminder_last_at"
        const val KEY_AUTO_BACKUP = "auto_backup_enabled"
        const val KEY_AUTO_BACKUP_HOUR = "auto_backup_hour"
        const val KEY_AUTO_BACKUP_MINUTE = "auto_backup_minute"
        const val KEY_AUTO_BACKUP_FOLDER = "auto_backup_folder"
        const val KEY_AUTO_BACKUP_LAST = "auto_backup_last"
        const val KEY_AUTO_BACKUP_ERROR = "auto_backup_error"
        const val KEY_FIRST_RUN_RESTORE = "first_run_restore_checked"
        const val KEY_JOURNAL_RULED = "journal_ruled"
        const val KEY_JOURNAL_PAPER = "journal_paper"
        const val KEY_JOURNAL_LINES = "journal_lines"
        const val KEY_JOURNAL_SNAP = "journal_snap"
        const val KEY_DAY_CARD_ORDER = "day_card_order"
        const val KEY_DAY_CARDS_HIDDEN = "day_cards_hidden"
        const val KEY_DAY_CARDS_COLLAPSED = "day_cards_collapsed"

        /** Les cles ne contiennent que des lettres, la virgule est sans risque. */
        private const val SEPARATOR = ","
        const val ITERATIONS = 120_000
    }
}
