package com.ismael.daybyday.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.ismael.daybyday.data.RichText

/**
 * Le double appui qui selectionne un mot.
 *
 * Compose ne le declenche pas dans ces champs — ni sur le titre, ni sur le
 * texte, donc ce n'est pas la mise en forme qui gene. On le refait ici, et de
 * la facon la plus simple possible :
 *
 * - on regarde passer les appuis dans la passe [PointerEventPass.Initial],
 *   c'est-a-dire avant le champ, mais sans rien consommer tant qu'il ne s'agit
 *   pas d'un double appui : l'appui simple, le glisser et l'appui long
 *   continuent de fonctionner exactement comme avant ;
 * - le mot est cherche a partir du curseur, pas du point touche. Le premier
 *   appui a deja pose le curseur au bon endroit, et partir de lui evite de
 *   convertir des coordonnees d'ecran en position dans un texte qui defile —
 *   la source d'erreur classique de ce genre de code ;
 * - le deuxieme appui est alors consomme jusqu'au relachement du doigt, sinon
 *   le champ replacerait le curseur et effacerait la selection a peine posee.
 */
fun Modifier.selectWordOnDoubleTap(
    value: () -> TextFieldValue,
    onSelect: (TextRange) -> Unit,
): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        var previousTapAt = 0L
        var previousTapPosition = Offset.Zero
        val timeout = viewConfiguration.doubleTapTimeoutMillis
        // Deux fois la tolerance de glissement : un doigt ne retombe jamais
        // exactement au meme pixel, et viser un mot n'est pas viser un point.
        val reach = viewConfiguration.touchSlop * 2f

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val down = event.changes.firstOrNull { it.changedToDownIgnoreConsumed() } ?: continue

            val quick = down.uptimeMillis - previousTapAt <= timeout
            val close = (down.position - previousTapPosition).getDistance() <= reach
            if (!quick || !close) {
                previousTapAt = down.uptimeMillis
                previousTapPosition = down.position
                continue
            }
            // Un troisieme appui ne doit pas etre lu comme un nouveau double.
            previousTapAt = 0L

            val current = value()
            val word = RichText.wordAt(current.text, current.selection.start) ?: continue
            onSelect(TextRange(word.first, word.last + 1))

            down.consume()
            do {
                val next = awaitPointerEvent(PointerEventPass.Initial)
                next.changes.forEach { it.consume() }
            } while (next.changes.any { it.pressed })
        }
    }
}
