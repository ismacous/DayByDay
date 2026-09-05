package com.ismael.daybyday.health

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.time.LocalDate
import java.time.ZoneId

/**
 * Temps reel passe sur le telephone, lu depuis les evenements d'usage
 * d'Android. On additionne des intervalles qui ne se chevauchent pas : quand
 * plusieurs applications se relaient, la periode ne compte qu'une seule fois.
 *
 * Additionner le "temps au premier plan" de chaque application, comme le fait
 * queryAndAggregateUsageStats, donne des totaux absurdes (16 h dans une
 * journee) parce que les periodes des applications se recouvrent.
 */
object ScreenTimeSource {

    /** Un evenement d'usage reduit a ce dont le calcul a besoin. */
    data class Moment(val type: Int, val timestamp: Long, val activity: String)

    fun hasPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun settingsIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Meme ecran, mais ouvert directement sur DayByDay quand le telephone le
     * permet : sur la liste complete il faut sinon chercher l'application a la
     * main parmi des dizaines d'autres.
     */
    fun settingsIntent(context: Context): Intent =
        settingsIntent().setData(Uri.fromParts("package", context.packageName, null))

    /** Minutes d'utilisation reelle du telephone ce jour-la, ou null sans autorisation. */
    fun minutesFor(context: Context, date: LocalDate): Int? {
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null

        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = minOf(
            date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            System.currentTimeMillis(),
        )
        if (dayEnd <= dayStart) return null

        val events = runCatching { manager.queryEvents(dayStart, dayEnd) }.getOrNull() ?: return null

        val moments = mutableListOf<Moment>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            moments += Moment(
                type = event.eventType,
                timestamp = event.timeStamp,
                activity = "${event.packageName}/${event.className}",
            )
        }

        val minutes = (foregroundMillis(moments, dayStart, dayEnd) / 60_000L).toInt()
        return if (minutes <= 0) null else minutes
    }

    /**
     * Duree pendant laquelle le telephone a reellement ete utilise entre
     * [from] et [to].
     *
     * Le point delicat : quand on passe de l'application A a l'application B,
     * Android envoie PAUSED(A), RESUMED(B), puis STOPPED(A) — dans cet ordre.
     * Fermer l'intervalle sur n'importe quel PAUSED/STOPPED faisait donc
     * refermer aussitot celui de B, et tout le temps passe ensuite sur B etait
     * perdu (une nuit entiere comptee comme une heure). On ne ferme que si
     * l'evenement concerne bien l'activite actuellement au premier plan.
     */
    internal fun foregroundMillis(moments: List<Moment>, from: Long, to: Long): Long {
        var total = 0L
        var openedAt = 0L
        var foreground: String? = null

        fun close(at: Long) {
            if (openedAt != 0L) {
                total += (at - openedAt).coerceAtLeast(0L)
                openedAt = 0L
            }
            foreground = null
        }

        moments.sortedBy { it.timestamp }.forEach { moment ->
            when (moment.type) {
                // Une application passe au premier plan : le telephone est utilise.
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    foreground = moment.activity
                    if (openedAt == 0L) openedAt = moment.timestamp
                }

                // Retour en arriere-plan : seule l'activite visible ferme la periode.
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED,
                -> if (moment.activity == foreground) close(moment.timestamp)

                // Ecran eteint ou verrouille : la periode se ferme quoi qu'il arrive.
                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.KEYGUARD_SHOWN,
                -> close(moment.timestamp)
            }
        }

        // Session encore ouverte a la fin de la periode observee.
        if (openedAt != 0L) total += (to - openedAt).coerceAtLeast(0L)

        // Filet de securite : jamais plus que le temps ecoule dans la journee.
        return minOf(total, (to - from).coerceAtLeast(0L))
    }
}
