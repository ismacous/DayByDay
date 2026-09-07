package com.ismael.daybyday.ui

import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import com.ismael.daybyday.data.PageLink
import java.time.LocalDate

/**
 * L'appui sur un lien vers une autre journee.
 *
 * Meme technique que le double appui sur un mot : on regarde passer les appuis
 * dans la passe [PointerEventPass.Initial], avant le champ de texte, et on ne
 * consomme **que** ceux qui tombent sur un lien. Tout le reste — poser le
 * curseur, selectionner, faire defiler — continue exactement comme avant.
 *
 * Ici, contrairement au double appui, on est bien oblige de convertir un point
 * touche en position dans le texte : c'est la seule facon de savoir sur quoi
 * le doigt s'est pose. Deux precautions, parce que
 * `getOffsetForPosition` repond **toujours** quelque chose, meme pour un doigt
 * pose a dix centimetres du texte :
 *
 * 1. la ligne du doigt doit etre celle du lien, verticalement ;
 * 2. le doigt doit etre entre le debut et la fin du lien, horizontalement.
 *
 * Sans ces deux verifications, appuyer dans le vide sous la derniere ligne
 * ouvrirait le lien qui s'y trouve — et l'appui serait consomme, donc le
 * curseur ne se poserait plus.
 */
fun Modifier.openPageLinkOnTap(
    layout: State<TextLayoutResult?>,
    text: () -> String,
    onOpen: (LocalDate) -> Unit,
): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val down = event.changes.firstOrNull { it.changedToDownIgnoreConsumed() } ?: continue

            val result = layout.value ?: continue
            val content = text()
            val position = down.position

            val offset = result.getOffsetForPosition(position)
            val link = PageLink.at(content, offset) ?: continue

            val line = result.getLineForOffset(link.range.first)
            if (position.y < result.getLineTop(line) || position.y > result.getLineBottom(line)) {
                continue
            }
            val left = result.getHorizontalPosition(link.range.first, usePrimaryDirection = true)
            val right = result.getHorizontalPosition(link.range.last + 1, usePrimaryDirection = true)
            if (position.x < minOf(left, right) || position.x > maxOf(left, right)) continue

            // Consomme jusqu'au relachement, sinon le champ pose le curseur et
            // ouvre le clavier au moment meme ou l'on change de page.
            down.consume()
            do {
                val next = awaitPointerEvent(PointerEventPass.Initial)
                next.changes.forEach { it.consume() }
            } while (next.changes.any { it.pressed })

            onOpen(link.date)
        }
    }
}
