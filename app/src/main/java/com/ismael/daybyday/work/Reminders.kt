package com.ismael.daybyday.work

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ismael.daybyday.R
import com.ismael.daybyday.data.WeekReview
import com.ismael.daybyday.data.WeekReviewBuilder
import com.ismael.daybyday.dayByDayApp
import com.ismael.daybyday.ui.MainActivity
import java.time.LocalDate
import java.util.Locale

/**
 * Les deux notifications de l'application, ecrites **une seule fois**.
 *
 * Elles partent de deux endroits — l'alarme du soir, et le bouton d'essai des
 * Reglages — et c'est justement pour ca que le texte et la construction vivent
 * ici : le rappel d'essai doit etre exactement celui qu'on recevra, sinon il ne
 * prouve rien.
 */
object Reminders {

    /**
     * Le rappel du soir.
     *
     * [forced] est le bouton d'essai : il part meme si le rappel est coupe ou
     * si la journee est deja notee — c'est tout son interet, verifier que la
     * notification arrive jusqu'au telephone.
     */
    suspend fun sendEvening(context: Context, forced: Boolean) {
        val app = context.applicationContext.dayByDayApp
        if (!forced) {
            if (!app.prefs.reminderEnabled) return
            val today = app.repository.dayOnce(LocalDate.now())
            // Une journee deja coloree n'a pas besoin qu'on la reclame.
            if (today?.colorKey != null) return
        }

        val name = app.prefs.firstName.trim()
        val notification = NotificationCompat
            .Builder(context.applicationContext, ReminderWorker.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                if (name.isEmpty()) "Et cette journée ?" else "Alors $name, cette journée ?"
            )
            .setContentText(
                if (forced) {
                    "C'est un essai : si tu vois ça, les rappels fonctionnent."
                } else {
                    "Prends 30 secondes pour lui donner une couleur."
                }
            )
            // PRIORITY_HIGH est ce que lisent les Android d'avant les canaux ;
            // sur les recents, c'est l'importance du canal qui decide. Les deux
            // sont necessaires pour que le rappel s'affiche en bandeau.
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(openApp(context, null, REQUEST_EVENING))
            .build()

        runCatching {
            NotificationManagerCompat.from(context.applicationContext)
                .notify(ReminderWorker.NOTIFICATION_ID, notification)
        }
        // L'heure du dernier rappel parti : les Reglages peuvent alors separer
        // nettement « jamais programme » de « etouffe par le telephone ».
        app.prefs.lastReminderAt = System.currentTimeMillis()
    }

    /**
     * Le bilan du lundi matin, sur la semaine **ecoulee** : celle qui commence
     * n'a rien a raconter.
     *
     * Rien ne part si la semaine est vide. Une notification qui annonce qu'il
     * n'y a rien a annoncer, c'est la meilleure facon de se faire couper.
     */
    suspend fun sendWeekly(context: Context, forced: Boolean) {
        val app = context.applicationContext.dayByDayApp
        if (!forced && !app.prefs.weeklyReviewEnabled) return

        val monday = WeekReviewBuilder.mondayOf(LocalDate.now()).minusWeeks(1)
        val review = app.repository.weekReview(monday)
        if (!review.hasData && !forced) return

        val name = app.prefs.firstName.trim()
        val notification = NotificationCompat
            .Builder(context.applicationContext, WeeklyReviewWorker.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (name.isEmpty()) "Ta semaine" else "$name, ta semaine")
            .setContentText(summaryLine(review.summary.average, review.summary.filledDays))
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText(review)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openApp(context, MainActivity.OPEN_WEEK, REQUEST_WEEKLY))
            .build()

        runCatching {
            NotificationManagerCompat.from(context.applicationContext)
                .notify(WeeklyReviewWorker.NOTIFICATION_ID, notification)
        }
        app.prefs.lastWeeklyReviewAt = System.currentTimeMillis()
    }

    private fun openApp(context: Context, destination: String?, request: Int): PendingIntent {
        val intent = Intent(context.applicationContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (destination != null) intent.putExtra(MainActivity.EXTRA_OPEN, destination)
        return PendingIntent.getActivity(
            context.applicationContext,
            request,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun summaryLine(average: Double?, filledDays: Int): String {
        if (average == null) return "Aucune couleur posée la semaine dernière."
        val formatted = String.format(Locale.FRANCE, "%.1f", average)
        return "$formatted / 3 sur $filledDays journée(s) notée(s)."
    }

    private fun bigText(review: WeekReview): String {
        val lines = mutableListOf(summaryLine(review.summary.average, review.summary.filledDays))
        if (review.movedDays > 0) lines += "Bougé ${review.movedDays} jour(s)."
        if (review.wentOutDays > 0) lines += "Sorti ${review.wentOutDays} jour(s)."
        if (review.writtenDays > 0) lines += "Écrit ${review.writtenDays} jour(s)."
        return lines.joinToString(" ")
    }

    private const val REQUEST_EVENING = 1
    private const val REQUEST_WEEKLY = 2
}
