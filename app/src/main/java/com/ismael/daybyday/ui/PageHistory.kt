package com.ismael.daybyday.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** L'etat complet d'une page a un instant : de quoi y revenir. */
data class PageSnapshot(
    val title: String,
    val blocks: List<PageBlock>,
)

/**
 * Annuler et refaire.
 *
 * **Ce qui est enregistre est la page entiere**, pas la difference. Une page
 * fait quelques kilo-octets et l'historique en garde soixante : c'est
 * negligeable, et ca evite toute une classe de bugs — un historique de
 * differences doit savoir defaire chaque operation a l'envers, et il suffit
 * d'en oublier une (deplacer un bloc, changer la couleur d'une citation) pour
 * que « annuler » abime la page au lieu de la reparer.
 *
 * **Tout ce qu'on tape n'est pas un pas.** Sinon annuler reculerait d'une
 * lettre, et il faudrait appuyer trente fois pour defaire une phrase. Un pas
 * est donc pose quand la **structure** change — un bloc ajoute, deplace,
 * efface — ou apres un silence : on ecrit, on s'arrete, ce qu'on vient
 * d'ecrire devient un pas.
 */
class PageHistory {

    private val past = ArrayDeque<PageSnapshot>()
    private val future = ArrayDeque<PageSnapshot>()

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    private var lastPushAt = 0L

    /** A l'ouverture d'une page : rien a annuler, on repart de zero. */
    fun reset() {
        past.clear()
        future.clear()
        lastPushAt = 0L
        refresh()
    }

    /**
     * Retient l'etat **avant** un changement.
     *
     * A appeler juste avant de modifier la page. [structural] force le pas :
     * poser un vocal ou deplacer une citation doit toujours pouvoir se
     * defaire, meme si on vient d'ecrire une lettre une demi-seconde plus tot.
     */
    fun record(before: PageSnapshot, structural: Boolean, now: Long = System.currentTimeMillis()) {
        val worthIt = structural ||
            now - lastPushAt > QUIET_MS ||
            past.lastOrNull()?.blocks?.size != before.blocks.size
        if (!worthIt) return

        if (past.lastOrNull() == before) return
        past.addLast(before)
        while (past.size > DEPTH) past.removeFirst()
        future.clear()
        lastPushAt = now
        refresh()
    }

    /** Revient d'un pas. Rend l'etat a poser, ou `null` s'il n'y a rien. */
    fun undo(current: PageSnapshot): PageSnapshot? {
        val previous = past.removeLastOrNull() ?: return null
        future.addLast(current)
        // Le prochain pas ne doit pas etre avale par le silence : on vient de
        // faire quelque chose de deliberé.
        lastPushAt = 0L
        refresh()
        return previous
    }

    /** Refait le pas qu'on vient d'annuler. */
    fun redo(current: PageSnapshot): PageSnapshot? {
        val next = future.removeLastOrNull() ?: return null
        past.addLast(current)
        lastPushAt = 0L
        refresh()
        return next
    }

    private fun refresh() {
        canUndo = past.isNotEmpty()
        canRedo = future.isNotEmpty()
    }

    private companion object {
        /** Combien d'etats on garde. Au-dela, personne ne remonte. */
        const val DEPTH = 60

        /** Le silence apres lequel ce qu'on vient d'ecrire devient un pas. */
        const val QUIET_MS = 900L
    }
}
