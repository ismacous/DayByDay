package com.ismael.daybyday.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismael.daybyday.data.TextSpan
import com.ismael.daybyday.data.TextStyleKind
import java.time.LocalDate

/**
 * L'allure de la page, passee de bloc en bloc.
 *
 * Un paquet plutot que huit parametres repetes partout : ces valeurs vont
 * toujours ensemble, et en oublier une dans un bloc donnerait un paragraphe
 * ecrit dans une autre police que ses voisins.
 */
data class PageStyle(
    val ink: Color,
    val paper: Color,
    val accent: Color,
    val baseFont: FontFamily?,
    val textSize: Int,
    /** L'ecart entre deux lignes du lignage, en unites de texte. */
    val rhythm: TextUnit,
)

/**
 * Le champ de texte d'un bloc.
 *
 * C'est le meme pour un paragraphe et pour une citation : seule la place
 * autour change. Il ne decide de rien — ni du curseur, ni des styles, ni du
 * defilement : il rend ce qu'on lui donne et previent quand quelque chose
 * bouge. Toute la logique reste a un seul endroit, l'ecran.
 */
@Composable
fun BlockTextField(
    value: TextFieldValue,
    spans: List<TextSpan>,
    style: PageStyle,
    layout: State<TextLayoutResult?>,
    placeholder: String?,
    heldSelection: TextRange?,
    onValueChange: (TextFieldValue) -> Unit,
    onLayout: (TextLayoutResult) -> Unit,
    onFocus: (Boolean) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = style.ink,
        fontFamily = style.baseFont,
        fontSize = style.textSize.sp,
        // La hauteur de ligne vient d'une mesure en points : agrandir les
        // caracteres dans Android decalerait sinon le texte de ses lignes.
        lineHeight = style.rhythm,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Bottom,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        cursorBrush = SolidColor(style.accent),
        onTextLayout = onLayout,
        visualTransformation = run {
            // Sans focus, le champ ne peint plus la selection : on la dessine
            // nous-memes, sinon on choisit une couleur a l'aveugle. Gardee
            // d'une recomposition a l'autre : la refabriquer a chaque frappe
            // ferait remettre en forme tout le bloc pour rien.
            val tint = style.accent.copy(alpha = 0.28f)
            remember(spans, heldSelection, tint, style.rhythm) {
                SpanTransformation(spans, heldSelection, tint, style.rhythm)
            }
        },
        decorationBox = { field ->
            Box {
                if (value.text.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        style = textStyle.copy(color = style.ink.copy(alpha = 0.45f)),
                    )
                }
                field()
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .openPageLinkOnTap(layout, { value.text }, onOpenDay)
            .hashtagChips(layout, style.textSize.toFloat())
            .onFocusChanged { onFocus(it.isFocused) }
            .selectWordOnDoubleTap({ value }) { onValueChange(value.copy(selection = it)) },
    )
}

/**
 * Une citation.
 *
 * Le trait n'est plus dessine a partir de la mise en page du texte : c'est une
 * **forme, a cote du texte**. Il fait donc toute la hauteur de la citation sans
 * qu'on ait a la calculer, il a sa propre couleur sans rien devoir a l'encre du
 * texte, et il y a enfin quelque chose a attraper pour deplacer la citation —
 * c'est lui.
 *
 * **Le trait est pose en `matchParentSize`, pas comme une colonne du `Row`.**
 * Premiere version : un `Box` a gauche, en `fillMaxHeight()`. Il ne s'affichait
 * pas du tout. `fillMaxHeight` ne remplit que si la hauteur maximale est
 * **connue** — or la citation vit dans une page qui defile, donc sa hauteur
 * maximale est infinie, et le trait se retrouvait haut de zero. En posant le
 * trait par-dessus une boite qui epouse la taille deja calculee du texte, la
 * contrainte est finie et le trait a enfin une hauteur. C'est le meme piege
 * partout : `fillMaxHeight` dans un conteneur qui defile ne remplit rien.
 */
@Composable
fun QuoteBlockView(
    value: TextFieldValue,
    spans: List<TextSpan>,
    bar: TextStyleKind?,
    fill: TextStyleKind?,
    style: PageStyle,
    layout: State<TextLayoutResult?>,
    heldSelection: TextRange?,
    onValueChange: (TextFieldValue) -> Unit,
    onLayout: (TextLayoutResult) -> Unit,
    onFocus: (Boolean) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    dragModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val color = bar?.let { Color(it.argb) } ?: style.ink
    val background = when (fill) {
        TextStyleKind.QUOTE_FILL_SOFT -> color.copy(alpha = 0.08f)
        TextStyleKind.QUOTE_FILL_FULL -> color.copy(alpha = 0.18f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(background),
    ) {
        // C'est le texte qui donne sa hauteur a la citation.
        BlockTextField(
            value = value,
            spans = spans,
            style = style,
            layout = layout,
            placeholder = "Ta citation",
            heldSelection = heldSelection,
            onValueChange = onValueChange,
            onLayout = onLayout,
            onFocus = onFocus,
            onOpenDay = onOpenDay,
            modifier = Modifier.padding(start = QUOTE_GRIP, end = 10.dp),
        )

        // Et le trait se pose dessus, sans participer a la mesure.
        Box(
            modifier = Modifier.matchParentSize(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .width(QUOTE_GRIP)
                    .fillMaxHeight()
                    .then(dragModifier)
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .width(QUOTE_BAR_WIDTH)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(QUOTE_BAR_WIDTH / 2))
                        .background(color),
                )
            }
        }
    }
}

/**
 * Un trait de separation.
 *
 * Il occupe exactement une ligne du lignage : le rythme de la page reprend
 * dessous sans decalage, comme si le trait etait une ligne de texte vide.
 */
@Composable
fun RuleBlockView(
    rule: TextStyleKind?,
    style: PageStyle,
    lineHeight: Dp,
    dragModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val thickness = ruleThickness(rule ?: TextStyleKind.RULE_THIN)
    val fraction = ruleWidth(rule ?: TextStyleKind.RULE_THIN)
    val color = style.ink.copy(alpha = 0.45f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(lineHeight)
            .then(dragModifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(lineHeight)) {
            val width = size.width * fraction
            val stroke = thickness.toPx()
            drawRoundRect(
                color = color,
                topLeft = Offset((size.width - width) / 2f, (size.height - stroke) / 2f),
                size = Size(width, stroke),
                cornerRadius = CornerRadius(stroke / 2f),
            )
        }
    }
}

/**
 * Le choix de la couleur du trait et du fond d'une citation.
 *
 * Pose **sous la citation qu'on est en train d'ecrire**, pas dans un panneau.
 * C'est tout l'objet du changement : les memes reglages existaient deja, mais
 * ranges au fond d'un panneau qui ne s'ouvrait que si le curseur etait au bon
 * endroit — donc introuvables. Ici, ils apparaissent a cote de ce qu'ils
 * changent, et disparaissent des qu'on ecrit ailleurs.
 */
@Composable
fun QuotePalette(
    bar: TextStyleKind?,
    fill: TextStyleKind?,
    style: PageStyle,
    onBar: (TextStyleKind?) -> Unit,
    onFill: (TextStyleKind?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = QUOTE_GRIP, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        TextStyleKind.quoteBars.forEach { option ->
            ColorDot(
                color = Color(option.argb),
                chosen = option == bar,
                ink = style.ink,
                label = "Trait ${option.label.lowercase()}",
                onClick = { onBar(if (option == bar) null else option) },
            )
        }

        Spacer(Modifier.width(3.dp))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(18.dp)
                .background(style.ink.copy(alpha = 0.18f)),
        )
        Spacer(Modifier.width(3.dp))

        val tint = bar?.let { Color(it.argb) } ?: style.ink
        FillDot(color = Color.Transparent, ink = style.ink, chosen = fill == null,
            label = "Sans fond", onClick = { onFill(null) })
        FillDot(color = tint.copy(alpha = 0.10f), ink = style.ink,
            chosen = fill == TextStyleKind.QUOTE_FILL_SOFT,
            label = "Fond léger", onClick = { onFill(TextStyleKind.QUOTE_FILL_SOFT) })
        FillDot(color = tint.copy(alpha = 0.24f), ink = style.ink,
            chosen = fill == TextStyleKind.QUOTE_FILL_FULL,
            label = "Fond plein", onClick = { onFill(TextStyleKind.QUOTE_FILL_FULL) })
    }
}

@Composable
private fun ColorDot(
    color: Color,
    chosen: Boolean,
    ink: Color,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (chosen) ink.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(if (chosen) 16.dp else 18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(color),
        )
    }
}

@Composable
private fun FillDot(
    color: Color,
    ink: Color,
    chosen: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Transparent),
        )
        if (chosen) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ink.copy(alpha = 0.55f)),
            )
        }
    }
}

/**
 * La poignee d'un bloc, dans la marge de gauche.
 *
 * Elle est **toujours la**, tres pale, et se marque sur le bloc ou l'on est.
 * C'est ce qui rend le systeme lisible : sans elle, une page en blocs
 * ressemble trait pour trait a une page qui n'en a pas, et rien ne dit qu'il y
 * a quelque chose a attraper. Une poignee qu'on ne voit pas est une poignee qui
 * n'existe pas.
 *
 * Elle est haute d'une ligne et calee en haut du bloc : elle designe le debut
 * du bloc, pas son milieu — un paragraphe de dix lignes aurait sinon sa
 * poignee perdue au milieu du texte.
 */
@Composable
fun BlockGutter(
    current: Boolean,
    ink: Color,
    lineHeight: Dp,
    dragModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val tint = ink.copy(alpha = if (current) 0.55f else 0.14f)

    Box(
        modifier = modifier
            .width(GUTTER_WIDTH)
            .height(lineHeight)
            .then(dragModifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(GRIP_WIDTH, GRIP_HEIGHT)) {
            // Six points : le signe universel de « ca se deplace ». Dessine
            // plutot qu'importe — six cercles ne valent pas le jeu complet des
            // icones Material.
            val radius = size.width / 6f
            val columnGap = size.width - radius * 2f
            val rowGap = (size.height - radius * 2f) / 2f
            repeat(3) { row ->
                repeat(2) { column ->
                    drawCircle(
                        color = tint,
                        radius = radius,
                        center = Offset(radius + column * columnGap, radius + row * rowGap),
                    )
                }
            }
        }
    }
}

/** La largeur de la marge qui porte les poignees. */
val GUTTER_WIDTH = 24.dp

private val GRIP_WIDTH = 10.dp
private val GRIP_HEIGHT = 16.dp

/**
 * Deplacer un bloc : on maintient le doigt, puis on tire.
 *
 * `detectDragGesturesAfterLongPress` et pas un simple glissement : sur une
 * page qui defile, un glissement appartient au defilement. L'appui maintenu
 * est le seul geste qui ne se dispute avec rien — c'est deja celui d'
 * « Organiser ma journee ».
 */
@Composable
fun Modifier.blockDrag(
    onStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onEnd: () -> Unit,
): Modifier {
    // `pointerInput` n'installe son detecteur qu'une fois : les fonctions qu'on
    // lui donne y restent figees a ce qu'elles etaient au premier passage.
    // C'est le meme piege que sur les photos, et `rememberUpdatedState` est la
    // meme reponse — le geste appelle toujours la version du moment.
    val start = rememberUpdatedState(onStart)
    val drag = rememberUpdatedState(onDrag)
    val end = rememberUpdatedState(onEnd)

    return this.pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
            onDragStart = { start.value() },
            onDragEnd = { end.value() },
            onDragCancel = { end.value() },
            onDrag = { change, amount ->
                change.consume()
                drag.value(amount.y)
            },
        )
    }
}

/** Retient la place d'un bloc dans la page : c'est ce qui permet de le deplacer. */
fun Modifier.reportPlacement(onPlace: (top: Float, height: Float) -> Unit): Modifier =
    onGloballyPositioned { onPlace(it.positionInParent().y, it.size.height.toFloat()) }

/** La largeur reservee au trait d'une citation, poignee comprise. */
val QUOTE_GRIP = 22.dp
private val QUOTE_BAR_WIDTH = 4.dp
