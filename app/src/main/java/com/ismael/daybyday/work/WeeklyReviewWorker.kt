package com.ismael.daybyday.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Le bilan de la semaine, quand il est demande **tout de suite** : c'est le
 * bouton d'essai des Reglages. Le rendez-vous du lundi matin, lui, passe par
 * une alarme — voir [ReminderAlarm].
 *
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
        Reminders.sendWeekly(applicationContext, forced = inputData.getBoolean(KEY_FORCE, false))
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "bilan_semaine"
        const val NOTIFICATION_ID = 1002
        const val REQUEST_CODE = 2

        /** Envoyer le bilan quoi qu'il arrive : c'est l'essai depuis les Réglages. */
        const val KEY_FORCE = "force"
    }
}
