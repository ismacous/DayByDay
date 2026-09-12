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

    override fun isDismissed(surface: NudgeSurface, epochDay: Long): Boolean =
        prefs.getLong(dismissKey(surface), NEVER) == epochDay

    override fun dismiss(surface: NudgeSurface, epochDay: Long) {
        prefs.edit().putLong(dismissKey(surface), epochDay).apply()
    }

    override fun notificationsSent(epochDay: Long): Int =
        if (prefs.getLong(KEY_NOTIFICATION_DAY, NEVER) == epochDay) {
            prefs.getInt(KEY_NOTIFICATION_COUNT, 0)
        } else {
            0
        }

    override fun recordNotification(epochDay: Long) {
        prefs.edit()
            .putLong(KEY_NOTIFICATION_DAY, epochDay)
            .putInt(KEY_NOTIFICATION_COUNT, notificationsSent(epochDay) + 1)
            .apply()
    }

    /** Remet tout a zero : tous les messages redeviennent disponibles. */
    fun forgetEverything() {
        prefs.edit().clear().apply()
    }

    private fun shownKey(slug: String) = "vu_$slug"

    private fun variantKey(slug: String) = "variante_$slug"

    private fun dismissKey(surface: NudgeSurface) = "ferme_${surface.name}"

    private companion object {
        const val NEVER = Long.MIN_VALUE
        const val KEY_NOTIFICATION_DAY = "notif_jour"
        const val KEY_NOTIFICATION_COUNT = "notif_nombre"
    }
}
