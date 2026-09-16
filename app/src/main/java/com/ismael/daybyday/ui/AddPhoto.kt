package com.ismael.daybyday.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismael.daybyday.dayByDayApp
import java.time.LocalDate

/**
 * « Ajouter une photo », des deux facons.
 *
 * L'application ne savait que piocher dans la galerie, ce qui obligeait a
 * sortir, prendre la photo avec l'appareil du telephone, revenir, puis la
 * retrouver dans la pellicule. Trois ecrans pour une chose qu'on voulait faire
 * sur le moment — et le moment, entre-temps, est passe.
 *
 * Le cliche pris depuis l'application est ecrit **directement** dans son
 * dossier prive, via une adresse valable pour ce seul fichier et pour la duree
 * de la prise de vue. Il ne passe donc jamais par la pellicule du telephone :
 * une photo du journal reste dans le journal.
 *
 * Aucune permission n'est ajoutee au passage. Declarer `CAMERA` obligerait a
 * la demander, alors que l'application ne se sert jamais de l'appareil
 * elle-meme : elle demande a celui du telephone de faire une photo et de la
 * deposer a l'adresse donnee. C'est le systeme qui gere le reste.
 */
class AddPhoto internal constructor(
    private val onChoose: () -> Unit,
) {
    /** Ouvre le choix : appareil photo, ou galerie. */
    fun ask() = onChoose()
}

/**
 * Prepare les deux chemins et le petit choix qui les separe.
 *
 * [onPicked] recoit ce qui a ete choisi dans la galerie — un ou plusieurs
 * fichiers a recopier. [onCaptured] recoit le chemin **deja ecrit** du cliche
 * qui vient d'etre pris : il n'y a rien a recopier, seulement a le rattacher.
 *
 * [dayOf] est relu au moment du geste et non capture a la creation : sur
 * l'ecran d'une journee, on peut changer de jour entre deux photos, et la
 * photo doit suivre le jour qu'on regarde.
 */
@Composable
fun rememberAddPhoto(
    dayOf: () -> LocalDate,
    onPicked: (List<Uri>) -> Unit,
    onCaptured: (String) -> Unit,
): AddPhoto {
    val context = LocalContext.current
    var asking by rememberSaveable { mutableStateOf(false) }

    // Le chemin prepare pour le cliche en cours.
    //
    // `rememberSaveable` et pas `remember` : pendant qu'on cadre la photo,
    // l'appareil photo est au premier plan et Android a tout a fait le droit
    // de detruire l'ecran qui l'a lance. Au retour, un simple `remember`
    // aurait tout oublie — la photo serait bien prise, ecrite au bon endroit,
    // et ne serait rattachee a aucune journee.
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }

    val pickFromGallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(30)
    ) { uris -> if (uris.isNotEmpty()) onPicked(uris) }

    val takePhoto = rememberLauncherForActivityResult(WritablePicture()) { taken ->
        val path = pendingPhoto
        pendingPhoto = null
        // `taken` vaut false sur une annulation, mais certains appareils photo
        // repondent false en ayant tout de meme ecrit. On regarde donc le
        // fichier plutot que la reponse : c'est lui qui dit la verite.
        if (path != null) onCaptured(path)
    }

    val cameraExists = remember(context) { cameraAvailable(context) }

    if (asking) {
        PhotoSourceDialog(
            onDismiss = { asking = false },
            onCamera = {
                asking = false
                val files = context.dayByDayApp.repository.media
                val path = files.newPhotoPath(dayOf().toEpochDay())
                pendingPhoto = path
                runCatching { takePhoto.launch(files.shareUri(path)) }.onFailure {
                    // Pas d'appareil photo, ou refus du systeme : on ne laisse
                    // pas le fichier vide derriere nous.
                    pendingPhoto = null
                    files.delete(path)
                }
            },
            onGallery = {
                asking = false
                pickFromGallery.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            },
            hasCamera = cameraExists,
        )
    }

    return remember { AddPhoto { asking = true } }
}

/**
 * La prise de vue, avec le droit d'ecrire.
 *
 * Le contrat fourni par Android pose l'adresse de sortie mais ne donne pas
 * l'autorisation d'y ecrire : l'appareil photo ouvre alors le fichier, echoue
 * en silence, et rend une photo vide. Les deux drapeaux la donnent, pour ce
 * seul fichier et le temps de la prise.
 */
private class WritablePicture : ActivityResultContracts.TakePicture() {
    override fun createIntent(context: Context, input: Uri): Intent =
        super.createIntent(context, input).addFlags(
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
}

/** Y a-t-il seulement un appareil photo a qui parler ? */
private fun cameraAvailable(context: Context): Boolean =
    Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        .resolveActivity(context.packageManager) != null

/**
 * Le choix, en deux lignes.
 *
 * Deux boutons cote a cote dans la carte auraient pris de la place sur chaque
 * carte et dans la barre du journal, pour un geste qu'on fait rarement. Un
 * seul bouton, et la question posee au moment ou elle se pose.
 */
@Composable
private fun PhotoSourceDialog(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    hasCamera: Boolean,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter une photo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (hasCamera) {
                    SourceRow("📷", "Prendre une photo", "Elle reste dans l'appli.", onCamera)
                }
                SourceRow("🖼️", "Choisir dans la galerie", "Photos et vidéos.", onGallery)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
private fun SourceRow(
    emoji: String,
    label: String,
    hint: String,
    onClick: () -> Unit,
) {
    val tint = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.08f))
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 17.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
