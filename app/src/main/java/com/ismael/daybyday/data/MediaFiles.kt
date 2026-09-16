package com.ismael.daybyday.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStream
import java.time.LocalDate
import java.util.UUID

/**
 * Gestion des fichiers photos / videos. Tout est copie dans le stockage
 * interne prive de l'application (/data/data/<package>/files/media), donc
 * invisible pour la galerie et pour les autres applications.
 */
class MediaFiles(private val context: Context) {

    val root: File get() = File(context.filesDir, "media").apply { if (!exists()) mkdirs() }

    fun file(relativePath: String): File = File(root, relativePath)

    /** Copie le contenu de [uri] dans le stockage prive et renvoie le media cree. */
    fun importFrom(uri: Uri, epochDay: Long): MediaItem? {
        val mime = context.contentResolver.getType(uri).orEmpty()
        val kind = if (mime.startsWith("video")) MediaKind.VIDEO else MediaKind.PHOTO
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            ?: if (kind == MediaKind.VIDEO) "mp4" else "jpg"

        val date = LocalDate.ofEpochDay(epochDay)
        val relativeDir = "%04d/%02d".format(date.year, date.monthValue)
        val relativePath = "$relativeDir/${UUID.randomUUID()}.$extension"
        val target = File(root, relativePath)
        target.parentFile?.mkdirs()

        val stream: InputStream = runCatching { context.contentResolver.openInputStream(uri) }
            .getOrNull() ?: return null
        stream.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        if (target.length() == 0L) {
            target.delete()
            return null
        }
        return MediaItem(
            epochDay = epochDay,
            relativePath = relativePath,
            kindKey = if (kind == MediaKind.VIDEO) 1 else 0,
        )
    }

    /**
     * Prepare la place d'une photo que l'appareil photo va prendre, et rend son
     * chemin relatif.
     *
     * Le cliche est ecrit **directement** au bon endroit, pas dans un fichier
     * temporaire recopie ensuite. C'est la meme raison que pour les vocaux, et
     * une de plus qui compte ici : une photo deposee ailleurs avant d'etre
     * recopiee, c'est une photo qui existe un instant hors du dossier prive de
     * l'application.
     */
    fun newPhotoPath(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        val relativePath = "%04d/%02d/%s.jpg".format(date.year, date.monthValue, UUID.randomUUID())
        val target = File(root, relativePath)
        target.parentFile?.mkdirs()
        // Le fichier doit exister avant d'etre partage : FileProvider refuse
        // une adresse qui ne mene nulle part.
        if (!target.exists()) target.createNewFile()
        return relativePath
    }

    /**
     * L'adresse a donner a l'appareil photo pour qu'il ecrive dans nos
     * fichiers. Elle ne vaut que pour ce fichier-la, et le temps de la prise.
     */
    fun shareUri(relativePath: String): Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.photos",
        file(relativePath),
    )

    /**
     * Reprend le cliche qui vient d'etre pris, ou `null` si l'appareil photo
     * n'a rien laisse — annulation, ou echec silencieux. Le fichier vide qu'on
     * avait prepare est alors efface : sinon chaque hesitation laisserait un
     * zero-octet derriere elle.
     */
    fun adoptPhoto(relativePath: String, epochDay: Long): MediaItem? {
        val target = file(relativePath)
        if (!target.exists() || target.length() == 0L) {
            delete(relativePath)
            return null
        }
        return MediaItem(
            epochDay = epochDay,
            relativePath = relativePath,
            kindKey = 0,
        )
    }

    /**
     * Prepare un fichier vide pour un enregistrement vocal, et rend son chemin
     * relatif.
     *
     * Le vocal est enregistre **directement** au bon endroit, pas dans un
     * fichier temporaire recopie ensuite : un enregistrement de dix minutes
     * recopie a la fin, c'est dix minutes de risque pour rien.
     */
    fun newVoicePath(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        val relativePath = "%04d/%02d/%s.m4a".format(date.year, date.monthValue, UUID.randomUUID())
        File(root, relativePath).parentFile?.mkdirs()
        return relativePath
    }

    /** Ecrit un fichier venant d'une sauvegarde. */
    fun writeFrom(relativePath: String, input: InputStream) {
        val target = File(root, relativePath)
        target.parentFile?.mkdirs()
        target.outputStream().use { output -> input.copyTo(output) }
    }

    fun delete(relativePath: String) {
        val target = File(root, relativePath)
        if (target.exists()) target.delete()
        // Nettoie les dossiers vides laisses derriere.
        var parent = target.parentFile
        while (parent != null && parent != root && parent.isDirectory && parent.list()?.isEmpty() == true) {
            parent.delete()
            parent = parent.parentFile
        }
    }

    fun deleteAll() {
        root.deleteRecursively()
        root.mkdirs()
    }

    fun totalBytes(): Long = root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
