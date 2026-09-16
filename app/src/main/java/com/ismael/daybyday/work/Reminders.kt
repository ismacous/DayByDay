package com.ismael.daybyday.work

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ismael.daybyday.R
import com.ismael.daybyday.coach.CoachEngine
import com.ismael.daybyday.coach.CoachSnapshot
import com.ismael.daybyday.coach.Nudge
import com.ismael.daybyday.coach.NudgeSurface
import com.ismael.daybyday.coach.TemporaryCoachMemory
import com.ismael.daybyday.data.WeekReviewBuilder
import com.ismael.daybyday.data.WeekWords
import com.ismael.daybyday.dayByDayApp
import com.ismael.daybyday.ui.MainActivity
import java.time.LocalDate
import java.time.LocalTime

/**
 * Les notifications de l'application, ecrites **une seule fois**.
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

        // Le texte se choisit dans [WeekWords], pas ici : c'est du Kotlin pur,
        // donc c'est testable, et c'est le seul moment ou l'application prend
        // la parole sans qu'on lui ait rien demande. La formulation tourne avec
        // le numero de semaine, pour que deux lundis de suite ne se ressemblent
        // pas mot pour mot.
        val words = WeekWords.of(
            review = review,
            firstName = app.prefs.firstName,
            variant = review.weekNumber,
        )
        val notification = NotificationCompat
            .Builder(context.applicationContext, WeeklyReviewWorker.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(words.title)
            // La ligne visible sans derouler est la phrase qui accueille, et
            // surtout pas la moyenne : « 2,3 / 3 sur 7 journee(s) notee(s) »
            // etait la premiere chose qu'Ismael lisait le lundi matin.
            .setContentText(words.short)
            .setStyle(NotificationCompat.BigTextStyle().bigText(words.long))
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

    /**
     * Le coup de pouce.
     *
     * Il ne sonne que s'il a vraiment quelque chose a dire : la regle de
     * politesse (« rien de special aujourd'hui ») reste dans l'application et
     * ne reveille jamais le telephone. Le quota du jour — une notification, ou
     * deux au maximum — est tenu par la memoire du coup de pouce, pas par le
     * nombre d'alarmes posees.
     *
     * [forced] est le bouton d'essai des Reglages : il ignore le quota et le
     * delai d'attente, pour montrer ce que ca donne tout de suite.
     */
    suspend fun sendCoach(context: Context, forced: Boolean) {
        val app = context.applicationContext.dayByDayApp
        val prefs = app.prefs
        if (!forced && (!prefs.coachEnabled || !prefs.coachNotificationsEnabled)) return

        val today = LocalDate.now()
        val oldest = today.toEpochDay() - COACH_HISTORY_DAYS
        val snapshot = CoachSnapshot.build(
            today = today,
            hourOfDay = LocalTime.now().hour,
            firstName = prefs.firstName,
            birthDate = prefs.birthDate,
            entries = app.repository.allDays().filter { it.epochDay >= oldest },
            tags = app.repository.allTags(),
            links = app.repository.allDayTags(),
            money = app.repository.allMoney().filter { it.epochDay >= oldest },
            treatments = app.repository.allTreatments(),
            doses = app.repository.allDoses().filter { it.epochDay >= oldest },
            hiddenCards = prefs.hiddenDayCards,
        )

        val memory = if (forced) TemporaryCoachMemory() else app.coach
        val nudge = CoachEngine.choose(
            snapshot = snapshot,
            memory = memory,
            surface = NudgeSurface.NOTIFICATION,
            maxNotifications = if (forced) Int.MAX_VALUE else prefs.coachNotificationsPerDay,
        )
        // Un essai ne doit jamais rester muet : sinon on ne sait pas si la
        // notification est bloquee ou s'il n'y avait simplement rien a dire.
        if (nudge == null && !forced) return
        val title = nudge?.title ?: "Un petit mot"
        val text = nudge?.text
            ?: "C'est un essai : si tu vois ça, le coup de pouce fonctionne."

        val notification = NotificationCompat
            .Builder(context.applicationContext, CoachWorker.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openApp(context, null, REQUEST_COACH))
            .build()

        val posted = runCatching {
            NotificationManagerCompat.from(context.applicationContext)
                .notify(CoachWorker.NOTIFICATION_ID, notification)
        }.isSuccess

        // Le quota n'est consomme que si la notification est reellement partie.
        if (posted && !forced && nudge != null) {
            CoachEngine.markShown(app.coach, nudge, NudgeSurface.NOTIFICATION, today.toEpochDay())
        }
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

    private const val REQUEST_EVENING = 1
    private const val REQUEST_WEEKLY = 2
    private const val REQUEST_COACH = 3

    /** Fenetre d'historique lue par le coup de pouce : un peu plus d'un an. */
    private const val COACH_HISTORY_DAYS = 400L
}
