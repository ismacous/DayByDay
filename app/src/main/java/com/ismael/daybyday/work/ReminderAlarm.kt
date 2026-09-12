package com.ismael.daybyday.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ismael.daybyday.data.Prefs
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * Les rendez-vous de l'application avec une **heure**, et pourquoi ils ne
 * passent plus par WorkManager.
 *
 * Le rappel de 21 h n'arrivait jamais, et deux corrections successives n'ont
 * pas suffi. La cause est plus profonde que le code : WorkManager n'est pas
 * fait pour ca. Une tache periodique n'a pas d'heure, elle a une **periode** —
 * « une fois par jour » veut dire « une fois quelque part dans chaque tranche
 * de vingt-quatre heures », et Android la place ou ca l'arrange. Ajoutez
 * Samsung, qui endort les applications qu'on n'ouvre pas, et la tache peut ne
 * jamais tourner.
 *
 * Une alarme, elle, vise un **instant absolu**. Trois consequences, et ce sont
 * exactement les trois problemes qu'on avait :
 *
 * 1. **Reprogrammer ne repousse rien.** L'ancienne tache repartait d'un compte
 *    a rebours : ouvrir l'application a 20 h remettait le rappel a demain. Ici
 *    on recalcule « le prochain 21 h », qui reste ce soir 21 h.
 * 2. **L'heure est tenue.** `setExactAndAllowWhileIdle` reveille le telephone,
 *    meme en veille profonde.
 * 3. **Ca survit a tout** : redemarrage, mise a jour, changement d'heure. Le
 *    [ReminderReceiver] rearme a chaque fois.
 *
 * L'alarme est **rearmee par celui qui la recoit**, jamais par une repetition
 * automatique : une repetition qui derive d'une minute par jour finit a une
 * heure du matin au bout d'un mois.
 */
object ReminderAlarm {

    const val ACTION_EVENING = "com.ismael.daybyday.RAPPEL_DU_SOIR"
    const val ACTION_WEEKLY = "com.ismael.daybyday.BILAN_DE_LA_SEMAINE"
    const val ACTION_COACH = "com.ismael.daybyday.COUP_DE_POUCE"
    const val ACTION_COACH_LATE = "com.ismael.daybyday.COUP_DE_POUCE_SOIR"

    private const val REQUEST_EVENING = 2101
    private const val REQUEST_WEEKLY = 2102
    private const val REQUEST_COACH = 2103
    private const val REQUEST_COACH_LATE = 2104

    /** Ecart entre les deux passages du coup de pouce, en heures. */
    const val COACH_SECOND_PASS_HOURS = 4

    /** Au-dela, le second passage tomberait en pleine nuit : on n'en pose pas. */
    private const val COACH_LATEST_HOUR = 22

    /** Arme les deux rendez-vous d'apres les preferences. Sans effet de bord. */
    fun rearmAll(context: Context, prefs: Prefs) {
        armEvening(context, prefs)
        armWeekly(context, prefs)
        armCoach(context, prefs)
    }

    /**
     * Le coup de pouce, qui passe **deux fois** dans la journee.
     *
     * Ce n'est pas deux notifications : c'est deux occasions d'en envoyer une.
     * La plupart des jours, le premier passage n'a rien a dire et se tait ; le
     * second rattrape. C'est le quota du jour, tenu dans la memoire du coup de
     * pouce, qui limite le nombre reellement envoye — jamais plus de deux.
     */
    fun armCoach(context: Context, prefs: Prefs) {
        val manager = alarms(context) ?: return
        val pending = pendingIntent(context, ACTION_COACH, REQUEST_COACH)
        val latePending = pendingIntent(context, ACTION_COACH_LATE, REQUEST_COACH_LATE)
        if (!prefs.coachEnabled || !prefs.coachNotificationsEnabled) {
            manager.cancel(pending)
            manager.cancel(latePending)
            prefs.nextCoachAt = 0L
            return
        }

        val at = nextDaily(prefs.coachHour, prefs.coachMinute)
        setExact(manager, at, pending)
        prefs.nextCoachAt = at

        val lateHour = prefs.coachHour + COACH_SECOND_PASS_HOURS
        if (lateHour <= COACH_LATEST_HOUR) {
            setExact(manager, nextDaily(lateHour, prefs.coachMinute), latePending)
        } else {
            manager.cancel(latePending)
        }
    }

    fun armEvening(context: Context, prefs: Prefs) {
        val manager = alarms(context) ?: return
        val pending = pendingIntent(context, ACTION_EVENING, REQUEST_EVENING)
        if (!prefs.reminderEnabled) {
            manager.cancel(pending)
            prefs.nextReminderAt = 0L
            return
        }
        val at = nextDaily(prefs.reminderHour, prefs.reminderMinute)
        setExact(manager, at, pending)
        prefs.nextReminderAt = at
    }

    fun armWeekly(context: Context, prefs: Prefs) {
        val manager = alarms(context) ?: return
        val pending = pendingIntent(context, ACTION_WEEKLY, REQUEST_WEEKLY)
        if (!prefs.weeklyReviewEnabled) {
            manager.cancel(pending)
            prefs.nextWeeklyReviewAt = 0L
            return
        }
        val at = nextWeekly(prefs.weeklyReviewHour, prefs.weeklyReviewMinute)
        setExact(manager, at, pending)
        prefs.nextWeeklyReviewAt = at
    }

    /**
     * Le prochain passage a [hour]:[minute], en millisecondes depuis 1970.
     *
     * Aujourd'hui si l'heure n'est pas encore passee, demain sinon. C'est cette
     * regle — un instant absolu recalcule a chaque fois — qui fait qu'ouvrir
     * l'application ne repousse plus le rappel.
     */
    fun nextDaily(hour: Int, minute: Int, now: ZonedDateTime = ZonedDateTime.now()): Long {
        var target = now.withHour(hour.coerceIn(0, 23))
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return target.toInstant().toEpochMilli()
    }

    /** Le prochain lundi a [hour]:[minute]. */
    fun nextWeekly(hour: Int, minute: Int, now: ZonedDateTime = ZonedDateTime.now()): Long {
        var target = now.withHour(hour.coerceIn(0, 23))
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
        if (!target.isAfter(now)) target = target.plusWeeks(1)
        return target.toInstant().toEpochMilli()
    }

    /** L'heure d'une alarme, lisible. Rend `null` quand rien n'est arme. */
    fun timeOf(millis: Long): ZonedDateTime? =
        if (millis <= 0L) null else ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(millis),
            ZoneId.systemDefault(),
        )

    private fun alarms(context: Context): AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private fun pendingIntent(context: Context, action: String, request: Int): PendingIntent {
        val intent = Intent(context.applicationContext, ReminderReceiver::class.java)
            .setAction(action)
        return PendingIntent.getBroadcast(
            context.applicationContext,
            request,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Pose l'alarme a l'heure exacte, et retombe sur une alarme approximative
     * si le telephone refuse — sur certaines versions d'Android, l'exactitude
     * est une autorisation a part, et une application qui plante parce qu'elle
     * ne l'a pas serait pire qu'un rappel a cinq minutes pres.
     */
    private fun setExact(manager: AlarmManager, at: Long, pending: PendingIntent) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            manager.canScheduleExactAlarms()
        val posed = allowed && runCatching {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }.isSuccess
        if (!posed) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }
}
