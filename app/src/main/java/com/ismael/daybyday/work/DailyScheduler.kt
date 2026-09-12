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
 * Ce qui se declenche tout seul, et par quel moyen.
 *
 * Deux mecaniques, et le choix entre les deux tient a une seule question :
 * **est-ce que l'heure compte ?**
 *
 * - Le rappel du soir et le bilan du lundi ont un rendez-vous avec quelqu'un.
 *   Ils passent par une **alarme** ([ReminderAlarm]), qui vise un instant.
 * - La sauvegarde automatique n'a rendez-vous avec personne : elle doit juste
 *   avoir eu lieu. Elle reste une tache periodique WorkManager, ou seule
 *   compte la regle apprise a ses depens — ne pas remettre le compte a rebours
 *   a zero a chaque ouverture de l'application, sinon elle n'arrive jamais.
 */
object DailyScheduler {

    const val REMINDER_WORK = "daybyday-rappel-quotidien"
    const val BACKUP_WORK = "daybyday-sauvegarde-quotidienne"
    const val TEST_REMINDER_WORK = "daybyday-rappel-test"
    const val WEEKLY_WORK = "daybyday-bilan-semaine"
    const val TEST_WEEKLY_WORK = "daybyday-bilan-test"
    const val TEST_COACH_WORK = "daybyday-coup-de-pouce-test"

    /**
     * Millisecondes jusqu'a la prochaine occurrence de [hour]:[minute].
     *
     * Ne sert plus qu'a la sauvegarde automatique, la seule des trois taches
     * dont l'heure n'a pas d'importance : personne ne regarde une sauvegarde
     * partir.
     */
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
     * Il ne passe plus par WorkManager, et c'est la troisieme et derniere
     * correction de ce bug. Les deux precedentes traitaient des symptomes :
     * la premiere version reprogrammait la tache a chaque ouverture, donc le
     * compte a rebours vers 21 h repartait de zero et le rappel n'arrivait
     * jamais ; la deuxieme ne reprogrammait plus que si l'heure changeait, et
     * le rappel arrivait... quand Android le decidait.
     *
     * La cause etait plus profonde : **une tache periodique n'a pas d'heure.**
     * « Une fois par jour » veut dire « une fois quelque part dans chaque
     * tranche de vingt-quatre heures », et le systeme la place ou ca l'arrange
     * — voire jamais, sur un Samsung qui endort les applications.
     *
     * C'est donc une alarme ([ReminderAlarm]), qui vise un instant absolu. On
     * annule au passage l'ancienne tache periodique : sans ca, les telephones
     * deja a jour recevraient les deux.
     */
    fun scheduleReminder(context: Context, prefs: Prefs) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(REMINDER_WORK)
        prefs.scheduledReminder = null
        ReminderAlarm.armEvening(context, prefs)
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

    /** Le bilan du lundi matin. Meme mecanique que le rappel du soir. */
    fun scheduleWeeklyReview(context: Context, prefs: Prefs) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WEEKLY_WORK)
        prefs.scheduledWeeklyReview = null
        ReminderAlarm.armWeekly(context, prefs)
    }

    /** Envoie le bilan de la semaine tout de suite, pour voir ce qu'il donne. */
    fun sendTestWeeklyReview(context: Context) {
        val request = OneTimeWorkRequestBuilder<WeeklyReviewWorker>()
            .setInputData(Data.Builder().putBoolean(WeeklyReviewWorker.KEY_FORCE, true).build())
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(TEST_WEEKLY_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    /** Le coup de pouce. Meme mecanique que le rappel du soir : une alarme. */
    fun scheduleCoach(context: Context, prefs: Prefs) {
        ReminderAlarm.armCoach(context, prefs)
    }

    /** Envoie le coup de pouce tout de suite, pour voir ce que ca donne. */
    fun sendTestCoach(context: Context) {
        val request = OneTimeWorkRequestBuilder<CoachWorker>()
            .setInputData(Data.Builder().putBoolean(CoachWorker.KEY_FORCE, true).build())
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(TEST_COACH_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    fun rescheduleAll(context: Context, prefs: Prefs) {
        scheduleReminder(context, prefs)
        scheduleAutoBackup(context, prefs)
        scheduleWeeklyReview(context, prefs)
        scheduleCoach(context, prefs)
    }
}
