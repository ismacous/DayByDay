package com.ismael.daybyday.ui

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import com.ismael.daybyday.data.Waveform
import java.io.File

/**
 * L'enregistrement et la lecture des vocaux.
 *
 * Deux choix qui expliquent le reste :
 *
 * 1. On enregistre **directement dans le fichier final**, pas dans un fichier
 *    temporaire recopie a la fin. Un vocal de dix minutes recopie a l'arrivee,
 *    c'est dix minutes de risque pour rien, et une copie qui echoue perd tout
 *    alors que l'enregistrement, lui, avait marche.
 * 2. La duree est **mesuree pendant** l'enregistrement, pas relue dans le
 *    fichier ensuite. La relire demanderait d'ouvrir chaque vocal juste pour
 *    afficher une liste.
 *
 * Le format est de l'AAC dans un conteneur MPEG-4 : c'est ce qu'Android sait
 * enregistrer et relire depuis toujours, sans bibliotheque a ajouter — donc
 * sans risque pour la regle « aucune permission Internet », qu'une dependance
 * pourrait rapporter dans son propre manifeste.
 */
class VoiceRecorder {

    private var recorder: MediaRecorder? = null
    private var startedAt = 0L

    /**
     * Les mesures du micro pendant l'enregistrement, pour dessiner la
     * silhouette du son.
     *
     * On les prend **au vol** : `getMaxAmplitude` rend le plus fort depuis le
     * dernier appel, donc appeler regulierement donne l'enveloppe du son sans
     * rien decoder. Les relire ensuite dans le fichier demanderait de le
     * decoder en entier — plusieurs secondes pour un vocal de dix minutes.
     */
    private val samples = mutableListOf<Int>()

    val isRecording: Boolean get() = recorder != null

    /**
     * Demarre l'enregistrement vers [target].
     *
     * On **demande d'abord si le micro est libre**, au lieu de se contenter du
     * resultat de `MediaRecorder`. Pendant un appel, celui-ci demarre sans
     * broncher et enregistre du silence : le vocal existe, il dure dix
     * secondes, et il ne contient rien. Un enregistrement vide qui a l'air
     * reussi est pire qu'un refus.
     */
    fun start(context: Context, target: File): RecordStart {
        stop()
        samples.clear()

        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audio != null) {
            // Hors du mode normal, le telephone est en communication : le micro
            // appartient a l'appel.
            if (audio.mode != AudioManager.MODE_NORMAL) return RecordStart.BUSY
            val takenByAnother = runCatching {
                audio.activeRecordingConfigurations.isNotEmpty()
            }.getOrDefault(false)
            if (takenByAnother) return RecordStart.BUSY
        }

        target.parentFile?.mkdirs()
        val instance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        return runCatching {
            instance.setAudioSource(MediaRecorder.AudioSource.MIC)
            instance.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            instance.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            // De la voix, pas de la musique : plus haut ne s'entend pas et
            // remplit le telephone.
            instance.setAudioEncodingBitRate(64_000)
            instance.setAudioSamplingRate(44_100)
            instance.setOutputFile(target.absolutePath)
            instance.prepare()
            instance.start()
            recorder = instance
            startedAt = SystemClock.elapsedRealtime()
            RecordStart.OK
        }.getOrElse {
            runCatching { instance.release() }
            target.delete()
            RecordStart.FAILED
        }
    }

    /**
     * Arrete l'enregistrement et rend sa duree, ou null s'il n'y avait rien a
     * arreter ou si le fichier est inutilisable.
     *
     * Un enregistrement de moins d'une demi-seconde est jete : c'est un appui
     * rate, pas un vocal, et une liste qui se remplit de silences d'un dixieme
     * de seconde devient vite pire que pas de liste du tout.
     */
    fun stop(): Long? {
        val instance = recorder ?: return null
        recorder = null
        val duration = SystemClock.elapsedRealtime() - startedAt
        // `stop` echoue si rien n'a encore ete ecrit : le fichier existe alors
        // mais il est vide, et c'est a l'appelant de le supprimer.
        val ok = runCatching { instance.stop() }.isSuccess
        runCatching { instance.release() }
        return if (ok && duration >= MINIMUM_MS) duration else null
    }

    /** Abandonne l'enregistrement en cours, sans rien garder. */
    fun cancel() {
        val instance = recorder ?: return
        recorder = null
        runCatching { instance.stop() }
        runCatching { instance.release() }
    }

    /**
     * La duree ecoulee depuis le debut, **et** une mesure du micro au passage.
     *
     * Les deux ensemble parce qu'ils ont le meme rythme : l'ecran a besoin du
     * chiffre qui defile, la forme d'onde a besoin d'un point regulier. Deux
     * horloges pour ca en feraient une de trop.
     */
    fun tick(): Long {
        val instance = recorder ?: return 0L
        runCatching { samples += instance.maxAmplitude }
        return SystemClock.elapsedRealtime() - startedAt
    }

    /** La silhouette du son qui vient d'etre enregistre. */
    fun waveform(): String = Waveform.encode(samples)

    /**
     * Le silence qui ne compte pas.
     *
     * Meme micro libre, un vocal peut ne rien contenir : on l'a lance dans un
     * endroit muet, ou le systeme a coupe la source. La plus forte mesure du
     * micro le dit, et c'est gratuit — on les a deja prises pour la forme
     * d'onde.
     */
    fun heardSomething(): Boolean = samples.any { it > SILENCE }

    private companion object {
        const val MINIMUM_MS = 500L

        /**
         * En dessous, c'est du bruit de fond. Un micro ouvert dans une piece
         * calme rend toujours quelques centaines ; zero absolu n'arrive que
         * quand la source est coupee.
         */
        const val SILENCE = 400
    }
}

/** Ce qui s'est passe quand on a demande a enregistrer. */
enum class RecordStart {
    OK,

    /** Le micro est pris : un appel en cours, ou une autre application. */
    BUSY,

    /** Le telephone a refuse pour une autre raison. */
    FAILED,
}

/**
 * Le lecteur des vocaux.
 *
 * Un seul a la fois, volontairement : deux vocaux qui se parlent dessus ne
 * servent a personne, et c'est aussi ce qui evite d'avoir a suivre plusieurs
 * lectures en cours a l'ecran.
 */
class VoicePlayer {

    private var player: MediaPlayer? = null
    private var playingPath: String? = null

    /** Le chemin du vocal en cours de lecture, ou null. */
    val playing: String? get() = playingPath

    /**
     * Joue [file], ou arrete la lecture si c'est deja lui qui joue.
     *
     * [onFinished] est appele a la fin comme a l'arret, pour que l'ecran n'ait
     * qu'un seul chemin de retour a suivre.
     */
    fun toggle(file: File, key: String, onFinished: () -> Unit) {
        if (playingPath == key) {
            stop()
            onFinished()
            return
        }
        stop()
        if (!file.exists()) return
        runCatching {
            val instance = MediaPlayer()
            instance.setDataSource(file.absolutePath)
            instance.setOnCompletionListener {
                stop()
                onFinished()
            }
            instance.prepare()
            instance.start()
            player = instance
            playingPath = key
        }.onFailure { stop() }
    }

    /**
     * Ou en est la lecture, entre 0 et 1.
     *
     * Demande a l'ecran plutot que renvoye par un rappel : `MediaPlayer` ne
     * previent de rien pendant qu'il joue, et une horloge qui ne tourne que
     * pendant la lecture coute moins qu'un etat de plus a tenir a jour.
     */
    fun progress(): Float {
        val instance = player ?: return 0f
        return runCatching {
            val total = instance.duration
            if (total <= 0) 0f else (instance.currentPosition.toFloat() / total).coerceIn(0f, 1f)
        }.getOrDefault(0f)
    }

    fun stop() {
        val instance = player ?: run { playingPath = null; return }
        player = null
        playingPath = null
        runCatching { instance.stop() }
        runCatching { instance.release() }
    }
}

/** « 1:07 », « 0:04 » : la duree telle qu'on la lit sur un message vocal. */
fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
