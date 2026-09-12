package com.ismael.daybyday.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ismael.daybyday.dayByDayApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Ce qui recoit les alarmes, et ce qui les rearme.
 *
 * Il fait deux choses a chaque reveil, et la seconde compte autant que la
 * premiere : il envoie la notification, **puis il rearme la suivante**. Une
 * alarme ne se repete pas toute seule ici — c'est volontaire. Une repetition
 * automatique derive (elle ajoute vingt-quatre heures a l'heure ou elle a
 * reellement tourne, pas a l'heure prevue), et au bout d'un mois le rappel du
 * soir arrive au milieu de la nuit. Recalculer « le prochain 21 h » a chaque
 * fois ne derive jamais.
 *
 * Il ecoute aussi le demarrage du telephone, la mise a jour de l'application et
 * les changements d'heure : dans les trois cas Android efface les alarmes, et
 * sans ca le rappel disparaitrait silencieusement.
 *
 * La notification part **d'ici**, et non d'une tache confiee a WorkManager :
 * l'alarme a deja reveille le processus, autant s'en servir. Un intermediaire
 * de plus, c'est un endroit de plus ou Samsung peut couper.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val action = intent.action

        val isAlarm = action == ReminderAlarm.ACTION_EVENING ||
            action == ReminderAlarm.ACTION_WEEKLY ||
            action == ReminderAlarm.ACTION_COACH ||
            action == ReminderAlarm.ACTION_COACH_LATE

        if (isAlarm) {
            // `goAsync` demande a Android quelques secondes de plus : le temps
            // de lire la journee en base avant de decider quoi afficher.
            val pending = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    when (action) {
                        ReminderAlarm.ACTION_EVENING -> Reminders.sendEvening(app, forced = false)
                        ReminderAlarm.ACTION_WEEKLY -> Reminders.sendWeekly(app, forced = false)
                        else -> Reminders.sendCoach(app, forced = false)
                    }
                } finally {
                    runCatching { ReminderAlarm.rearmAll(app, app.dayByDayApp.prefs) }
                    pending.finish()
                }
            }
            return
        }

        // Demarrage, mise a jour, changement d'heure : Android a efface les
        // alarmes, on les repose.
        runCatching { ReminderAlarm.rearmAll(app, app.dayByDayApp.prefs) }
    }
}
