package com.ismael.daybyday.health

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Lecture des pas depuis Health Connect, le standard Android ou Samsung Health
 * ecrit ses donnees. Tout se passe en local sur le telephone : l'application
 * n'a pas la permission Internet, elle ne peut rien envoyer nulle part.
 */
object HealthConnectSource {

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
    )

    /** Une nuit lue sur le telephone : coucher et lever en minutes depuis minuit. */
    data class Night(val startMinutes: Int, val endMinutes: Int)

    fun isAvailable(context: Context): Boolean =
        runCatching { HealthConnectClient.getSdkStatus(context) }
            .getOrNull() == HealthConnectClient.SDK_AVAILABLE

    /**
     * Ecran systeme ou l'autorisation des pas s'accorde et se retire. Depuis
     * Android 14 Health Connect fait partie des reglages du telephone ; avant,
     * c'est une application a part.
     */
    fun settingsIntent(): Intent {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            "android.health.connect.action.HEALTH_HOME_SETTINGS"
        } else {
            HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS
        }
        return Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun client(context: Context): HealthConnectClient? =
        runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()

    suspend fun hasPermission(context: Context): Boolean {
        if (!isAvailable(context)) return false
        val client = client(context) ?: return false
        return runCatching {
            client.permissionController.getGrantedPermissions().containsAll(permissions)
        }.getOrDefault(false)
    }

    /** Nombre de pas de la journee, ou null si la lecture n'est pas possible. */
    suspend fun stepsFor(context: Context, date: LocalDate): Int? {
        if (!hasPermission(context)) return null
        val client = client(context) ?: return null
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val result: AggregationResult = runCatching {
            client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )
        }.getOrNull() ?: return null
        return result[StepsRecord.COUNT_TOTAL]?.toInt()
    }

    /**
     * La nuit qui a mene a [date], si le telephone ou une montre l'a enregistree.
     *
     * On cherche entre midi la veille et midi le jour meme : une nuit commence
     * le soir precedent et se termine le matin, et cette fenetre l'attrape en
     * entier sans ramasser la sieste de l'apres-midi. Quand plusieurs sessions
     * sont enregistrees, la plus longue est la vraie nuit.
     */
    suspend fun nightFor(context: Context, date: LocalDate): Night? {
        if (!hasPermission(context)) return null
        val client = client(context) ?: return null
        val zone = ZoneId.systemDefault()
        val from = date.minusDays(1).atTime(12, 0).atZone(zone).toInstant()
        val to = date.atTime(12, 0).atZone(zone).toInstant()

        val sessions = runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(from, to),
                )
            ).records
        }.getOrNull().orEmpty()

        val night = sessions.maxByOrNull { it.endTime.toEpochMilli() - it.startTime.toEpochMilli() }
            ?: return null
        if (night.endTime <= night.startTime) return null

        return Night(
            startMinutes = minutesOfDay(night.startTime, zone),
            endMinutes = minutesOfDay(night.endTime, zone),
        )
    }

    private fun minutesOfDay(instant: java.time.Instant, zone: ZoneId): Int {
        val time: LocalDateTime = LocalDateTime.ofInstant(instant, zone)
        return time.hour * 60 + time.minute
    }
}
