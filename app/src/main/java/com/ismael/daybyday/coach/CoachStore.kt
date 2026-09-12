package com.ismael.daybyday.coach

import android.content.Context

/**
 * La memoire du coup de pouce, rangee a part des autres preferences pour
 * qu'un « oublier ce qui a deja ete dit » ne touche a rien d'autre.
 *
 * Tout reste sur le telephone, comme le reste de l'application.
 */
class CoachStore(context: Context) : CoachMemory {

    private val prefs = context.applicationContext
        .getSharedPreferences("daybyday_coach", Context.MODE_PRIVATE)

    override fun lastShown(slug: String): Long? {
        val value = prefs.getLong(shownKey(slug), NEVER)
        return if (value == NEVER) null else value
    }

    override fun variant(slug: String): Int = prefs.getInt(variantKey(slug), 0)

    override fun remember(slug: String, epochDay: Long) {
        val editor = prefs.edit().putLong(shownKey(slug), epochDay)
        // La phrase ne tourne qu'une fois par jour : sinon le texte changerait
        // a chaque fois que l'ecran est recompose.
        if (lastShown(slug) != epochDay) editor.putInt(variantKey(slug), variant(slug) + 1)
        editor.apply()
    }

    override fun popupsShown(epochDay: Long): Int = countFor(KEY_POPUP_DAY, KEY_POPUP_COUNT, epochDay)

    override fun recordPopup(epochDay: Long) {
        bump(KEY_POPUP_DAY, KEY_POPUP_COUNT, epochDay)
    }

    override fun notificationsSent(epochDay: Long): Int =
        countFor(KEY_NOTIFICATION_DAY, KEY_NOTIFICATION_COUNT, epochDay)

    override fun recordNotification(epochDay: Long) {
        bump(KEY_NOTIFICATION_DAY, KEY_NOTIFICATION_COUNT, epochDay)
    }

    /** Remet tout a zero : tous les messages redeviennent disponibles. */
    fun forgetEverything() {
        prefs.edit().clear().apply()
    }

    /**
     * Un compteur qui se remet a zero en changeant de jour : on garde le jour
     * a cote du nombre plutot qu'une cle par date, sinon les preferences
     * grossiraient d'une ligne par journee pour toujours.
     */
    private fun countFor(dayKey: String, countKey: String, epochDay: Long): Int =
        if (prefs.getLong(dayKey, NEVER) == epochDay) prefs.getInt(countKey, 0) else 0

    private fun bump(dayKey: String, countKey: String, epochDay: Long) {
        prefs.edit()
            .putLong(dayKey, epochDay)
            .putInt(countKey, countFor(dayKey, countKey, epochDay) + 1)
            .apply()
    }

    private fun shownKey(slug: String) = "vu_$slug"

    private fun variantKey(slug: String) = "variante_$slug"

    private companion object {
        const val NEVER = Long.MIN_VALUE
        const val KEY_POPUP_DAY = "popup_jour"
        const val KEY_POPUP_COUNT = "popup_nombre"
        const val KEY_NOTIFICATION_DAY = "notif_jour"
        const val KEY_NOTIFICATION_COUNT = "notif_nombre"
    }
}
