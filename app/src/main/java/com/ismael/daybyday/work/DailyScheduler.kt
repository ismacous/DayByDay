package com.ismael.daybyday.work

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ismael.daybyday.data.Prefs
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Planifie les deux taches quotidiennes locales : le rappel du soir et la
 * sauvegarde automatique. Tout passe par WorkManager, donc ca survit aux
 * redemarrages du telephone sans permission supplementaire.
 */
object DailyScheduler {

    const val REMINDER_WORK = "daybyday-rappel-quotidien"
    const val BACKUP_WORK = "daybyday-sauvegarde-quotidienne"
    const val TEST_REMINDER_WORK = "daybyday-rappel-test"

    /** Millisecondes jusqu'a la prochaine occurrence de [hour]:[minute]. */
    fun initialDelayMillis(hour: Int, minute: Int, now: ZonedDateTime = ZonedDateTime.now()): Long {
        var target = now.withHour(hour.coerceIn(0, 23))
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return Duration.between(now, target).toMillis()
    }

    /**
     * Programme le rappel du soir.
     *
     * Le piege, et la raison du bug du rappel qui ne sonnait jamais : cette
     * fonction est appelee au demarrage de l'application, donc a chaque fois
     * qu'Ismael l'ouvre. Elle reprogrammait alors la tache **en repartant de
     * zero**, et le compte a rebours vers 21 h recommencait. Ouvrir
     * l'application a 20 h repoussait le rappel au lendemain 21 h ; l'ouvrir
     * tous les jours le repoussait indefiniment.
     *
     * On ne reprogramme donc que si l'heure ou l'activation ont change depuis la
     * derniere fois. Sinon on laisse la tache deja en place vivre sa vie
     * ([ExistingPeriodicWorkPolicy.KEEP] la garde si elle existe, et la recree si
     * elle a disparu — apres une mise a jour, par exemple).
     */
    fun scheduleReminder(context: Context, prefs: Prefs) {
        val manager = WorkManager.getInstance(context.applicationContext)
        if (!prefs.reminderEnabled) {
            manager.cancelUniqueWork(REMINDER_WORK)
            prefs.scheduledReminder = null
            return
        }
        val signature = "${prefs.reminderHour}:${prefs.reminderMinute}"
        val changed = prefs.scheduledReminder != signature
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(
                initialDelayMillis(prefs.reminderHour, prefs.reminderMinute),
                TimeUnit.MILLISECONDS,
            )
            .build()
        manager.enqueueUniquePeriodicWork(
            REMINDER_WORK,
            if (changed) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
        prefs.scheduledReminder = signature
    }

    /**
     * Envoie le rappel tout de suite, pour verifier qu'il arrive bien. C'est le
     * seul moyen de distinguer « la notification est mal programmee » de « le
     * telephone l'a bloquee », et Samsung coupe volontiers les taches de fond.
     */
    fun sendTestReminder(context: Context) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(Data.Builder().putBoolean(ReminderWorker.KEY_FORCE, true).build())
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(TEST_REMINDER_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    fun scheduleAutoBackup(context: Context, prefs: Prefs) {
        val manager = WorkManager.getInstance(context.applicationContext)
        if (!prefs.autoBackupEnabled || prefs.autoBackupFolder == null) {
            manager.cancelUniqueWork(BACKUP_WORK)
            prefs.scheduledBackup = null
            return
        }
        // Meme regle que pour le rappel : ne pas remettre le compteur a zero a
        // chaque ouverture, sinon la sauvegarde de 23 h n'arrive jamais non plus.
        val signature = "${prefs.autoBackupHour}:${prefs.autoBackupMinute}:${prefs.autoBackupFolder}"
        val changed = prefs.scheduledBackup != signature
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(
                initialDelayMillis(prefs.autoBackupHour, prefs.autoBackupMinute),
                TimeUnit.MILLISECONDS,
            )
            .build()
        manager.enqueueUniquePeriodicWork(
            BACKUP_WORK,
            if (changed) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
        prefs.scheduledBackup = signature
    }

    fun rescheduleAll(context: Context, prefs: Prefs) {
        scheduleReminder(context, prefs)
        scheduleAutoBackup(context, prefs)
    }
}
