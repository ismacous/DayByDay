package com.ismael.daybyday.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Le coup de pouce, quand il est demande **tout de suite** : c'est le bouton
 * d'essai des Reglages.
 *
 * L'heure de tous les jours ne passe pas par ici mais par une alarme (voir
 * [ReminderAlarm]), pour la meme raison que le rappel du soir : une tache
 * periodique n'a pas d'heure, elle a une periode. Ce worker partage le texte
 * avec le vrai — sinon l'essai ne prouverait rien.
 */
class CoachWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Reminders.sendCoach(applicationContext, forced = inputData.getBoolean(KEY_FORCE, false))
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "coup_de_pouce"
        const val NOTIFICATION_ID = 1003

        /** Envoyer le message quoi qu'il arrive : c'est l'essai. */
        const val KEY_FORCE = "force"
    }
}
