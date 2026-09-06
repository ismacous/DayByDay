package com.ismael.daybyday.work

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ismael.daybyday.R
import com.ismael.daybyday.data.WeekReviewBuilder
import com.ismael.daybyday.dayByDayApp
import com.ismael.daybyday.ui.MainActivity
import java.time.LocalDate
import java.util.Locale

/**
 * Le rendez-vous du lundi matin : « voila ta semaine ».
 *
 * C'est le seul moment ou l'application prend la parole pour dire quelque
 * chose plutot que pour demander quelque chose. La notification ne repete pas
 * l'ecran : elle donne la couleur generale de la semaine et une raison
 * d'ouvrir.
 *
 * Rien n'est envoye si la semaine est vide. Une notification qui annonce qu'il
 * n'y a rien a annoncer, c'est la meilleure facon de se faire couper.
 */
class WeeklyReviewWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext.dayByDayApp
        val forced = inputData.getBoolean(KEY_FORCE, false)
        if (!forced && !app.prefs.weeklyReviewEnabled) return Result.success()

        // La semaine **ecoulee** : le lundi matin, celle qui commence n'a rien
        // a raconter.
        val monday = WeekReviewBuilder.mondayOf(LocalDate.now()).minusWeeks(1)
        val review = app.repository.weekReview(monday)
        if (!review.hasData && !forced) return Result.success()

        val name = app.prefs.firstName.trim()
        val title = if (name.isEmpty()) "Ta semaine" else "$name, ta semaine"

        val intent = Intent(applicationContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_WEEK)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(summaryLine(review.summary.average, review.summary.filledDays))
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText(review)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        }
        app.prefs.lastWeeklyReviewAt = System.currentTimeMillis()
        return Result.success()
    }

    private fun summaryLine(average: Double?, filledDays: Int): String {
        if (average == null) {
            return "Aucune couleur posée la semaine dernière."
        }
        val formatted = String.format(Locale.FRANCE, "%.1f", average)
        return "$formatted / 3 sur $filledDays journée(s) notée(s)."
    }

    private fun bigText(review: com.ismael.daybyday.data.WeekReview): String {
        val lines = mutableListOf(summaryLine(review.summary.average, review.summary.filledDays))
        if (review.movedDays > 0) lines += "Bougé ${review.movedDays} jour(s)."
        if (review.wentOutDays > 0) lines += "Sorti ${review.wentOutDays} jour(s)."
        if (review.writtenDays > 0) lines += "Écrit ${review.writtenDays} jour(s)."
        return lines.joinToString(" ")
    }

    companion object {
        const val CHANNEL_ID = "bilan_semaine"
        const val NOTIFICATION_ID = 1002
        const val REQUEST_CODE = 2

        /** Envoyer le bilan quoi qu'il arrive : c'est l'essai depuis les Réglages. */
        const val KEY_FORCE = "force"
    }
}
