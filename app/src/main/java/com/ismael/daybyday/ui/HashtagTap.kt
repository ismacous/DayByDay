package com.ismael.daybyday.ui

import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import com.ismael.daybyday.data.Hashtag

/**
 * Appuyer sur un mot-cle ouvre les journees qui le portent.
 *
 * C'etait le chainon manquant. Ecrire `#mood` en faisait bien une etiquette,
 * colorée et retrouvable dans la recherche — mais il fallait *savoir* que la
 * recherche avait un filtre « Mots-clés », l'ouvrir, et y retrouver son mot
 * parmi les autres. Personne ne fait ça. Le mot est déjà sous le doigt : c'est
 * lui qu'on touche.
 *
 * Mêmes précautions que pour un lien entre pages, et pour la même raison :
 * `getOffsetForPosition` répond toujours quelque chose, même pour un doigt posé
 * loin sous le texte. Sans la vérification en **ligne** et en **colonne**,
 * appuyer dans le vide ouvrirait la recherche, et le curseur ne se poserait
 * plus jamais au bas de la page.
 */
fun Modifier.openHashtagOnTap(
    layout: State<TextLayoutResult?>,
    text: () -> String,
    onOpen: (String) -> Unit,
): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val down = event.changes.firstOrNull { it.changedToDownIgnoreConsumed() } ?: continue

            val result = layout.value ?: continue
            val content = text()
            val position = down.position
            if (content.isEmpty()) continue

            val offset = result.getOffsetForPosition(position)
            val range = Hashtag.rangesIn(content).firstOrNull { offset in it.first..it.last + 1 }
                ?: continue

            val line = result.getLineForOffset(range.first)
            if (position.y < result.getLineTop(line) || position.y > result.getLineBottom(line)) {
                continue
            }
            val left = result.getHorizontalPosition(range.first, usePrimaryDirection = true)
            val right = result.getHorizontalPosition(range.last + 1, usePrimaryDirection = true)
            if (position.x < minOf(left, right) || position.x > maxOf(left, right)) continue

            // Consomme jusqu'au relachement, sinon le champ pose le curseur et
            // ouvre le clavier au moment meme ou l'on change d'ecran.
            down.consume()
            do {
                val next = awaitPointerEvent(PointerEventPass.Initial)
                next.changes.forEach { it.consume() }
            } while (next.changes.any { it.pressed })

            // Sans le `#` : c'est le mot qui sert de cle, comme dans la
            // recherche.
            onOpen(content.substring(range.first + 1, range.last + 1))
        }
    }
}
