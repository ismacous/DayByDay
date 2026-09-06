package com.ismael.daybyday.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Le rappel du soir, quand il est demande **tout de suite** : c'est le bouton
 * d'essai des Reglages.
 *
 * L'heure de tous les jours ne passe plus par ici : une tache periodique n'a
 * pas d'heure, elle a une periode, et Android la place ou ca l'arrange. Voir
 * [ReminderAlarm]. Ce worker ne sert donc plus qu'a envoyer le rappel a la
 * demande, et il partage le texte avec le vrai — sinon l'essai ne prouverait
 * rien.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Reminders.sendEvening(applicationContext, forced = inputData.getBoolean(KEY_FORCE, false))
        return Result.success()
    }

    companion object {
        /**
         * Le canal actuel. Le suffixe compte : l'importance d'un canal ne se
         * change plus une fois qu'il existe, donc passer le rappel en bandeau
         * a demande d'en creer un nouveau.
         */
        const val CHANNEL_ID = "rappel_quotidien_bandeau"

        /** L'ancien canal, en importance normale. Supprime au demarrage. */
        const val LEGACY_CHANNEL_ID = "rappel_quotidien"

        const val NOTIFICATION_ID = 1001

        /** Envoyer le rappel quoi qu'il arrive : c'est le rappel d'essai. */
        const val KEY_FORCE = "force"
    }
}
