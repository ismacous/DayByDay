package com.ismael.daybyday

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.ismael.daybyday.coach.CoachStore
import com.ismael.daybyday.data.AppDatabase
import com.ismael.daybyday.data.DayRepository
import com.ismael.daybyday.data.Prefs
import com.ismael.daybyday.data.TagCatalog
import com.ismael.daybyday.work.CoachWorker
import com.ismael.daybyday.work.DailyScheduler
import com.ismael.daybyday.work.ReminderWorker
import com.ismael.daybyday.work.WeeklyReviewWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DayByDayApp : Application() {

    val repository: DayRepository by lazy { DayRepository(this) }
    val prefs: Prefs by lazy { Prefs(this) }
    val lock: LockController by lazy { LockController(prefs) }

    /** Ce que le coup de pouce a deja dit, pour ne pas se repeter. */
    val coach: CoachStore by lazy { CoachStore(this) }

    /**
     * Le bonjour du demarrage a-t-il deja ete joue ?
     *
     * Porte par l'application et non par l'ecran : une rotation du telephone ou
     * un retour a l'accueil recreent l'activite, et l'animation repartirait a
     * chaque fois. Une animation d'accueil se voit une fois par ouverture, pas
     * a chaque aller-retour.
     */
    var helloPlayed: Boolean = false

    /** Portee de coroutine liee au process, pour les sauvegardes de fin d'ecran. */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createReminderChannel()
        createWeeklyChannel()
        createCoachChannel()
        DailyScheduler.rescheduleAll(this, prefs)
        appScope.launch {
            runCatching { TagCatalog.sync(AppDatabase.get(this@DayByDayApp).dayDao()) }
        }
    }

    /**
     * Le canal du rappel du soir.
     *
     * Il est en importance **haute**, pour que le rappel s'affiche en bandeau
     * par-dessus l'ecran, comme un SMS, au lieu d'attendre sagement dans le
     * volet. Attention : l'importance d'un canal ne se change plus une fois
     * qu'il existe — Android la confie a l'utilisateur et ignore toute
     * modification ensuite. Passer de « normale » a « haute » demande donc un
     * **nouveau** canal, et de supprimer l'ancien pour ne pas laisser deux
     * lignes « Rappel quotidien » dans les reglages du telephone.
     */
    private fun createReminderChannel() {
        val manager = NotificationManagerCompat.from(this)
        manager.deleteNotificationChannel(ReminderWorker.LEGACY_CHANNEL_ID)
        val channel = NotificationChannelCompat
            .Builder(ReminderWorker.CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName("Rappel quotidien")
            .setDescription("Le petit rappel du soir pour noter ta journée.")
            .setVibrationEnabled(true)
            .build()
        manager.createNotificationChannel(channel)
    }

    /**
     * Le canal du coup de pouce. Importance normale, et canal a part : on doit
     * pouvoir le couper sans perdre le rappel du soir, qui lui est un bandeau.
     */
    private fun createCoachChannel() {
        val channel = NotificationChannelCompat
            .Builder(CoachWorker.CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName("Coup de pouce")
            .setDescription("Les petits messages de soutien, une ou deux fois par jour au maximum.")
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    /**
     * Le canal du bilan du lundi. Importance normale, volontairement : ce n'est
     * pas quelque chose a faire, c'est quelque chose a lire. Un bandeau
     * par-dessus l'ecran pour une lecture, ca s'appelle deranger.
     */
    private fun createWeeklyChannel() {
        val channel = NotificationChannelCompat
            .Builder(WeeklyReviewWorker.CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName("Bilan de la semaine")
            .setDescription("Le récapitulatif du lundi matin.")
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }
}

/** Etat de verrouillage de l'application, partage entre l'activite et l'UI. */
class LockController(private val prefs: Prefs) {

    var isLocked by mutableStateOf(prefs.lockEnabled && prefs.hasPin)
        private set

    private var backgroundedAt: Long = 0L

    fun unlock() {
        isLocked = false
    }

    fun onEnterBackground() {
        backgroundedAt = System.currentTimeMillis()
    }

    fun onEnterForeground() {
        if (!prefs.lockEnabled || !prefs.hasPin) {
            isLocked = false
            return
        }
        if (backgroundedAt != 0L && System.currentTimeMillis() - backgroundedAt > GRACE_MS) {
            isLocked = true
        }
    }

    /** Appele quand le verrouillage vient d'etre active ou desactive. */
    fun refresh() {
        if (!prefs.lockEnabled || !prefs.hasPin) isLocked = false
    }

    private companion object {
        const val GRACE_MS = 15_000L
    }
}

val Context.dayByDayApp: DayByDayApp
    get() = applicationContext as DayByDayApp
