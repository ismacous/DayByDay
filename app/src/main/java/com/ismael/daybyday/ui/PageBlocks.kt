package com.ismael.daybyday.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.ismael.daybyday.data.BlockKind
import com.ismael.daybyday.data.JournalBlock
import com.ismael.daybyday.data.RichText
import com.ismael.daybyday.data.StyleFamily
import com.ismael.daybyday.data.TextSpan
import com.ismael.daybyday.data.TextStyleKind
import java.util.concurrent.atomic.AtomicLong

/**
 * Un bloc de page tel que l'ecran le manipule.
 *
 * Ce n'est pas [JournalBlock] : celui-la est ce qu'on enregistre, celui-ci est
 * ce qu'on edite. La difference qui compte est la [key] — une identite qui vit
 * le temps de l'ecran, la meme avant et apres un enregistrement. Les
 * identifiants de la base ne peuvent pas servir a ca : un bloc qu'on vient de
 * creer n'en a pas encore, et deux blocs sans identifiant seraient le meme aux
 * yeux de Compose, qui melangerait leurs champs de texte au premier
 * deplacement.
 */
data class PageBlock(
    val key: Long,
    val kind: BlockKind,
    val value: TextFieldValue = TextFieldValue(""),
    val spans: List<TextSpan> = emptyList(),
    /** Bloc vocal : la ligne de `voice_notes` qu'il montre. */
    val voiceId: Long? = null,
    /** Citation : la couleur de son trait, et son fond. */
    val bar: TextStyleKind? = null,
    val fill: TextStyleKind? = null,
    /** Trait : lequel. */
    val rule: TextStyleKind? = null,
) {
    val isText: Boolean get() = kind == BlockKind.TEXT
    val text: String get() = value.text
}

object PageBlocks {

    private val keys = AtomicLong(1)

    fun newKey(): Long = keys.getAndIncrement()

    fun text(text: String = "", spans: List<TextSpan> = emptyList()): PageBlock = PageBlock(
        key = newKey(),
        kind = BlockKind.TEXT,
        value = TextFieldValue(text),
        spans = spans,
    )

    fun voice(voiceId: Long): PageBlock =
        PageBlock(key = newKey(), kind = BlockKind.VOICE, voiceId = voiceId)

    fun rule(style: TextStyleKind): PageBlock =
        PageBlock(key = newKey(), kind = BlockKind.RULE, rule = style)

    fun quote(text: String = ""): PageBlock = PageBlock(
        key = newKey(),
        kind = BlockKind.QUOTE,
        value = TextFieldValue(text, TextRange(0, text.length)),
    )

    /** Ce qui sort de la base devient editable. */
    fun from(stored: List<JournalBlock>): List<PageBlock> = stored.map { block ->
        PageBlock(
            key = newKey(),
            kind = block.kind,
            value = TextFieldValue(block.text),
            spans = RichText.decode(block.spans, block.text.length),
            voiceId = block.voiceId,
            bar = style(block.barCode, StyleFamily.QUOTE_BAR),
            fill = style(block.fillCode, StyleFamily.QUOTE_FILL),
            rule = TextStyleKind.fromCode(block.ruleCode)?.takeIf { it.isRule },
        )
    }

    /**
     * Et ce qui est edite retourne en base.
     *
     * Un bloc vocal sans son est ecarte : c'est celui de l'enregistrement en
     * cours, une place tenue au chaud le temps qu'on parle. L'enregistrer
     * laisserait une barre de lecture vide dans la page si l'application etait
     * fermee au milieu d'une phrase.
     */
    fun toStored(blocks: List<PageBlock>, epochDay: Long): List<JournalBlock> =
        blocks
            .filterNot { it.kind == BlockKind.VOICE && it.voiceId == null }
            .mapIndexed { index, block ->
            JournalBlock(
                epochDay = epochDay,
                position = index,
                kindCode = block.kind.code,
                text = if (block.kind == BlockKind.RULE || block.kind == BlockKind.VOICE) {
                    ""
                } else {
                    block.text
                },
                spans = RichText.encode(block.spans),
                voiceId = block.voiceId,
                barCode = block.bar?.code.orEmpty(),
                fillCode = block.fill?.code.orEmpty(),
                ruleCode = block.rule?.code.orEmpty(),
            )
        }

    private fun style(code: String, family: StyleFamily): TextStyleKind? =
        TextStyleKind.fromCode(code)?.takeIf { it.family == family }

    /**
     * Coupe un bloc de texte au curseur pour glisser quelque chose entre les
     * deux moities.
     *
     * C'est **la** operation qui fait qu'un vocal ou une citation peut se poser
     * au milieu d'un paragraphe : avant, la page etait un seul champ, donc il
     * n'existait aucun endroit ou mettre quoi que ce soit, et tout finissait a
     * la fin. Couper au curseur ne coupe rien quand le curseur est deja au
     * debut ou a la fin : on ne fabrique pas un bloc vide pour rien.
     */
    fun insertAt(
        blocks: List<PageBlock>,
        focused: Long?,
        inserted: PageBlock,
    ): List<PageBlock> {
        val index = blocks.indexOfFirst { it.key == focused }
        if (index < 0) return blocks + inserted

        val block = blocks[index]
        if (!block.isText) {
            return blocks.take(index + 1) + inserted + blocks.drop(index + 1)
        }

        val caret = block.value.selection.end.coerceIn(0, block.text.length)
        val before = blocks.take(index)
        val after = blocks.drop(index + 1)

        return when (caret) {
            0 -> before + inserted + block + after
            block.text.length -> before + block + inserted + after
            else -> {
                val head = block.copy(
                    value = TextFieldValue(
                        block.text.substring(0, caret),
                        TextRange(caret),
                    ),
                    spans = clip(block.spans, 0, caret),
                )
                val tail = PageBlock(
                    key = newKey(),
                    kind = BlockKind.TEXT,
                    value = TextFieldValue(block.text.substring(caret)),
                    spans = clip(block.spans, caret, block.text.length).map {
                        TextSpan(it.start - caret, it.end - caret, it.style)
                    },
                )
                before + head + inserted + tail + after
            }
        }
    }

    /**
     * Fait d'une selection une citation.
     *
     * Avec du texte choisi, c'est **lui** qui part dans la citation et le
     * paragraphe se recoud autour : c'est ce qu'on attend en selectionnant une
     * phrase avant d'appuyer sur « citation ». Sans selection, une citation
     * vide se pose au curseur, prête a ecrire — un bloc vide vaut mieux qu'un
     * exemple a effacer.
     *
     * Rend la page et la **cle du bloc a qui donner le clavier** : sans elle,
     * on poserait une citation vide sans curseur dedans, et il faudrait viser
     * un trait de quatre points de large pour commencer a ecrire.
     */
    fun toQuote(blocks: List<PageBlock>, focused: Long?): Pair<List<PageBlock>, Long?> {
        val index = blocks.indexOfFirst { it.key == focused }
        val block = blocks.getOrNull(index)

        if (block == null || !block.isText) {
            val empty = quote("")
            return insertAt(blocks, focused, empty) to empty.key
        }

        val selection = block.value.selection
        val from = minOf(selection.start, selection.end).coerceIn(0, block.text.length)
        val to = maxOf(selection.start, selection.end).coerceIn(0, block.text.length)

        if (to <= from) {
            val empty = quote("")
            return insertAt(blocks, focused, empty) to empty.key
        }

        val quoted = block.text.substring(from, to)
        val head = block.copy(
            value = TextFieldValue(block.text.substring(0, from)),
            spans = clip(block.spans, 0, from),
        )
        val middle = PageBlock(
            key = newKey(),
            kind = BlockKind.QUOTE,
            value = TextFieldValue(quoted, TextRange(quoted.length)),
            spans = clip(block.spans, from, to).map {
                TextSpan(it.start - from, it.end - from, it.style)
            },
        )
        val tail = PageBlock(
            key = newKey(),
            kind = BlockKind.TEXT,
            value = TextFieldValue(block.text.substring(to)),
            spans = clip(block.spans, to, block.text.length).map {
                TextSpan(it.start - to, it.end - to, it.style)
            },
        )

        val out = blocks.take(index) + listOf(head, middle, tail) + blocks.drop(index + 1)
        // `tidy` jette les moities vides — une citation prise au debut du
        // paragraphe ne doit pas laisser un bloc vide devant elle.
        return tidy(out) to middle.key
    }

    private fun clip(spans: List<TextSpan>, from: Int, to: Int): List<TextSpan> =
        spans.mapNotNull { span ->
            val s = maxOf(span.start, from)
            val e = minOf(span.end, to)
            if (e > s) TextSpan(s, e, span.style) else null
        }

    /**
     * Range la page apres un deplacement ou une suppression.
     *
     * Deux blocs de texte qui se touchent n'ont aucune raison d'exister : rien
     * ne les separe, donc rien ne les distingue. Ils sont recolles, et c'est ce
     * qui fait qu'effacer une citation refait un seul paragraphe au lieu de
     * laisser une couture invisible au milieu du texte. Un bloc de texte vide
     * s'en va aussi — sauf s'il ne reste que lui, il faut bien un endroit ou
     * ecrire.
     */
    fun tidy(blocks: List<PageBlock>): List<PageBlock> {
        val out = mutableListOf<PageBlock>()
        blocks.forEach { block ->
            val previous = out.lastOrNull()
            if (block.isText && previous != null && previous.isText) {
                val joined = previous.text + "\n" + block.text
                val shift = previous.text.length + 1
                out[out.lastIndex] = previous.copy(
                    value = TextFieldValue(joined, TextRange(shift.coerceAtMost(joined.length))),
                    spans = RichText.merge(
                        previous.spans + block.spans.map {
                            TextSpan(it.start + shift, it.end + shift, it.style)
                        }
                    ),
                )
            } else {
                out += block
            }
        }

        val kept = out.filterNot { it.isText && it.text.isEmpty() && out.size > 1 }
        return kept.ifEmpty { listOf(text()) }
    }
}
