package com.ismael.daybyday.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.ismael.daybyday.data.Hashtag
import com.ismael.daybyday.data.MediaItem
import com.ismael.daybyday.data.MediaLayer
import com.ismael.daybyday.data.MediaShape
import com.ismael.daybyday.data.Placement
import com.ismael.daybyday.data.StyleFamily
import com.ismael.daybyday.data.TextSpan
import com.ismael.daybyday.data.TextStyleKind
import com.ismael.daybyday.data.VoiceNote
import com.ismael.daybyday.data.Waveform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/**
 * L'export d'une page du journal en PDF.
 *
 * Le principe : **la page est redessinee**, pas photographiee. Une capture
 * d'ecran donnerait une image floue a l'impression, coupee a la hauteur de
 * l'ecran, et sans texte selectionnable. Ici on repasse par la meme mise en
 * page que l'ecran — memes polices, memes couleurs, memes photos aux memes
 * endroits — mais mesuree pour une feuille A4.
 *
 * Deux astuces rendent ca simple :
 *
 * 1. On mesure tout dans une densite de **1**, donc un point de l'ecran vaut
 *    une unite du PDF. Toutes les coordonnees enregistrees (les placements de
 *    photos, la hauteur de ligne) s'utilisent telles quelles, sans conversion
 *    nulle part — et une conversion oubliee quelque part est exactement ce qui
 *    fait qu'une photo se retrouve a dix centimetres de sa place.
 * 2. Une page plus longue qu'une feuille n'est pas re-mise en page : on dessine
 *    **le meme dessin** sur chaque feuille, decale vers le haut et rogne. Le
 *    texte et les photos restent donc alignes entre eux d'une feuille a
 *    l'autre, ce qu'une remise en page par feuille ne garantirait pas.
 */
object PagePdf {

    /**
     * Le cadre d'une citation, aux mesures de l'ecran : un fond aux coins
     * arrondis, et un trait de quatre points pose a quatre points du bord.
     * Voir `QuoteBlockView`.
     */
    private const val QUOTE_RADIUS = 8f
    private const val QUOTE_BAR_X = 4f
    private const val QUOTE_BAR_W = 4f

    /** A4 en points PostScript (72 par pouce), la seule unite que comprend un PDF. */
    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842

    /** La marge de la feuille. Une page de journal collee au bord se lit mal. */
    private const val MARGIN = 36f

    /**
     * Ecrit la page du [date] dans [target].
     *
     * Rend le nombre de feuilles produites, pour pouvoir le dire a l'ecran.
     */
    suspend fun write(
        context: Context,
        target: Uri,
        date: LocalDate,
        title: String,
        body: String,
        spans: List<TextSpan>,
        photos: List<MediaItem>,
        voiceNotes: List<VoiceNote>,
        photoFile: (MediaItem) -> File,
        paper: Color,
        ink: Color,
        lineColor: Color,
        /** La couleur d'accent, pour le bouton de lecture des vocaux. */
        accent: Color,
        ruled: Boolean,
        baseFont: FontFamily?,
        textSize: Int,
        /** La largeur de la page telle qu'elle est a l'ecran, en points. */
        pageWidth: Float,
    ): Int = withContext(Dispatchers.IO) {
        // Densite 1 : un point d'ecran = une unite de dessin. Voir plus haut.
        val density = Density(density = 1f, fontScale = 1f)
        val measurer = TextMeasurer(
            defaultFontFamilyResolver = createFontFamilyResolver(context),
            defaultDensity = density,
            defaultLayoutDirection = LayoutDirection.Ltr,
        )

        val width = if (pageWidth > 0f) pageWidth else 360f
        val rhythm = JournalPaper.LINE_SPACING.value.sp
        val sidePadding = 20f
        val topPadding = JournalPaper.TOP_PADDING.value

        val titleLayout = if (title.isBlank()) {
            null
        } else {
            measurer.measure(
                text = title,
                style = TextStyle(
                    color = ink,
                    fontFamily = baseFont,
                    fontSize = (textSize + 7).sp,
                    fontWeight = FontWeight.Bold,
                ),
                constraints = Constraints(maxWidth = (width - 2 * sidePadding).toInt()),
            )
        }

        val bodyLayout = measurer.measure(
            text = buildAnnotatedStringWithSpans(body, spans, rhythm, hashtagColors = true),
            style = TextStyle(
                color = ink,
                fontFamily = baseFont,
                fontSize = textSize.sp,
                lineHeight = rhythm,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Bottom,
                    trim = LineHeightStyle.Trim.None,
                ),
            ),
            constraints = Constraints(maxWidth = (width - 2 * sidePadding).toInt()),
        )

        val titleHeight = titleLayout?.size?.height?.toFloat()?.plus(10f) ?: 0f
        val textTop = topPadding + titleHeight
        // La hauteur du texte s'arrete a sa derniere ligne **qui porte quelque
        // chose**, pas au bas de la mise en page.
        //
        // Un bloc de texte vide est une ligne blanche voulue : la page en garde
        // autant qu'on en a laisse, et une page qui finit par trois retours a
        // la ligne mesurait trois lignes de plus. Trois lignes suffisaient a
        // pousser la feuille suivante, qui ne contenait alors rien du tout.
        val textHeight = lastContentLine(bodyLayout, body, spans)
            ?.let { bodyLayout.getLineBottom(it) }
            ?: 0f
        // La page descend jusqu'au plus bas des deux : le texte, ou la photo la
        // plus basse. Une photo posee sous le dernier mot ne doit pas etre
        // coupee parce que le texte s'arretait avant elle.
        val lowestPhoto = photos.filter { it.isPlaced }
            .maxOfOrNull { (it.placedY ?: 0f) + it.displayHeight } ?: 0f
        // Les vocaux se rangent sous le texte, comme a l'ecran : ils ne sont
        // pas poses a une hauteur choisie, ils suivent.
        val voiceTop = textTop + textHeight + 8f
        val voiceHeight = voiceNotes.size * (Placement.VOICE_HEIGHT + 10f)
        val contentHeight = maxOf(
            voiceTop + voiceHeight + topPadding,
            lowestPhoto + topPadding,
        )

        val images = photos.filter { it.isPlaced }
            .mapNotNull { item -> decode(photoFile(item))?.let { item to it } }

        val scale = (A4_WIDTH - 2 * MARGIN) / width
        val sheetHeight = (A4_HEIGHT - 2 * MARGIN) / scale
        val sheets = maxOf(1, Math.ceil((contentHeight / sheetHeight).toDouble()).toInt())

        val document = PdfDocument()
        val drawScope = CanvasDrawScope()

        repeat(sheets) { sheet ->
            val info = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, sheet + 1).create()
            val page = document.startPage(info)
            val canvas = Canvas(page.canvas)

            drawScope.draw(
                density = density,
                layoutDirection = LayoutDirection.Ltr,
                canvas = canvas,
                size = Size(A4_WIDTH.toFloat(), A4_HEIGHT.toFloat()),
            ) {
                drawRect(color = paper)
                translate(MARGIN, MARGIN) {
                    // Le pivot est l'origine, pas le centre : par defaut une
                    // mise a l'echelle se fait autour du milieu de la feuille,
                    // et toute la page partirait de travers.
                    scale(scale, scale, pivot = Offset.Zero) {
                        clipRect(0f, 0f, width, sheetHeight) {
                            translate(0f, -sheet * sheetHeight) {
                                drawSheet(
                                    width = width,
                                    height = contentHeight,
                                    paper = paper,
                                    lineColor = lineColor,
                                    ruled = ruled,
                                    images = images,
                                    titleLayout = titleLayout,
                                    bodyLayout = bodyLayout,
                                    sidePadding = sidePadding,
                                    topPadding = topPadding,
                                    textTop = textTop,
                                    ink = ink,
                                    body = body,
                                    spans = spans,
                                    voiceNotes = voiceNotes,
                                    voiceTop = voiceTop,
                                    accent = accent,
                                )
                            }
                        }
                    }
                }
                // Le numero de feuille, discret, seulement s'il y en a
                // plusieurs : sur une feuille unique il n'apprend rien.
                if (sheets > 1) {
                    val label = measurer.measure(
                        text = "${Dates.dayMedium(date)} · ${sheet + 1}/$sheets",
                        style = TextStyle(color = ink.copy(alpha = 0.45f), fontSize = 8.sp),
                    )
                    drawText(
                        textLayoutResult = label,
                        topLeft = Offset(MARGIN, A4_HEIGHT - MARGIN + 8f),
                    )
                }
            }

            document.finishPage(page)
        }

        context.contentResolver.openOutputStream(target)?.use { document.writeTo(it) }
        document.close()
        sheets
    }

    /**
     * Le dessin de la page, une seule fois, dans son propre repere.
     *
     * C'est **le meme** dessin pour toutes les feuilles : ce sont le decalage
     * et le rognage appeles au-dessus qui decident de ce qu'on en voit.
     */
    private fun DrawScope.drawSheet(
        width: Float,
        height: Float,
        paper: Color,
        lineColor: Color,
        ruled: Boolean,
        images: List<Pair<MediaItem, ImageBitmap>>,
        titleLayout: TextLayoutResult?,
        bodyLayout: TextLayoutResult,
        sidePadding: Float,
        topPadding: Float,
        textTop: Float,
        ink: Color,
        body: String,
        spans: List<TextSpan>,
        voiceNotes: List<VoiceNote>,
        voiceTop: Float,
        accent: Color,
    ) {
        drawRect(color = paper, topLeft = Offset.Zero, size = Size(width, height))

        if (ruled) {
            val margin = JournalPaper.SIDE_MARGIN.value

            // Le lignage est **deduit de la mise en page du texte**, ligne par
            // ligne, et non pose a un rythme calcule de son cote.
            //
            // C'est le meme piege qu'a l'ecran, et il se voyait autant : la
            // hauteur de ligne demandee tombe sur un nombre de points a
            // virgule, le moteur de texte arrondit chaque ligne, et l'ecart
            // s'accumule — apres vingt lignes, les traits passent au milieu des
            // mots. S'y ajoutait ici une erreur que l'ecran n'a pas : le titre
            // est dessine **dans** la feuille et decale tout le texte vers le
            // bas, alors que le lignage, lui, partait toujours du haut. Les
            // deux ne pouvaient pas tomber juste.
            //
            // En posant un trait sous chaque ligne reelle, il n'y a plus rien a
            // faire coincider : c'est juste par construction.
            val lines = bodyLayout.lineCount
            val rule = { y: Float ->
                drawLine(
                    color = lineColor,
                    start = Offset(margin, y),
                    end = Offset(width - margin, y),
                    strokeWidth = 1f,
                )
            }
            for (line in 0 until lines) rule(textTop + bodyLayout.getLineBottom(line))

            // Sous le texte, on continue au pas reel des dernieres lignes, pour
            // que le bas de la page garde le meme rythme que le haut.
            val advance = if (lines >= 2) {
                (bodyLayout.getLineBottom(lines - 1) - bodyLayout.getLineBottom(0)) / (lines - 1)
            } else {
                JournalPaper.LINE_SPACING.value
            }
            if (advance > 0.5f) {
                var y = textTop + bodyLayout.getLineBottom(lines - 1) + advance
                while (y < height) {
                    rule(y)
                    y += advance
                }
            }
            drawLine(
                color = lineColor,
                start = Offset(margin * 0.65f, 0f),
                end = Offset(margin * 0.65f, height),
                strokeWidth = 2f,
            )
        }

        // D'abord ce qui est derriere le texte, puis le texte, puis ce qui est
        // devant : le meme ordre qu'a l'ecran, sinon une photo de fond
        // recouvrirait ce qu'elle est censee laisser lire.
        images.filter { it.first.layer != MediaLayer.FRONT }
            .forEach { (item, image) -> drawPlaced(item, image) }

        titleLayout?.let {
            drawText(textLayoutResult = it, topLeft = Offset(sidePadding, topPadding))
        }

        translate(sidePadding, textTop) {
            drawJournalMarks(bodyLayout, body, spans, ink, width - 2 * sidePadding)
            drawText(textLayoutResult = bodyLayout, topLeft = Offset.Zero)
        }

        images.filter { it.first.layer == MediaLayer.FRONT }
            .forEach { (item, image) -> drawPlaced(item, image) }

        // Les vocaux ne s'ecoutent pas sur une feuille de papier, mais ils
        // occupent une place dans la page : les retirer de l'export ferait
        // deux mises en page differentes pour la meme journee.
        voiceNotes.forEachIndexed { index, note ->
            drawVoice(
                note = note,
                x = sidePadding,
                y = voiceTop + index * (Placement.VOICE_HEIGHT + 10f),
                barWidth = width - 2 * sidePadding,
                ink = ink,
                accent = accent,
            )
        }
    }

    /** Un vocal, dessine comme a l'ecran : le rond, la silhouette, la duree. */
    private fun DrawScope.drawVoice(
        note: VoiceNote,
        x: Float,
        y: Float,
        barWidth: Float,
        ink: Color,
        accent: Color,
    ) {
        val w = barWidth
        val h = Placement.VOICE_HEIGHT

        drawRoundRect(
            color = ink.copy(alpha = 0.07f),
            topLeft = Offset(x, y),
            size = Size(w, h),
            cornerRadius = CornerRadius(18f),
        )
        drawCircle(color = accent, radius = 19f, center = Offset(x + 8f + 19f, y + h / 2f))

        val heights = Waveform.decode(note.waveform)
        val waveLeft = x + 8f + 38f + 10f
        val waveWidth = (w - (waveLeft - x) - 44f).coerceAtLeast(1f)
        val step = waveWidth / heights.size
        val barWidth = (step * 0.55f).coerceAtLeast(0.6f)
        heights.forEachIndexed { index, value ->
            val barHeight = ((h - 28f) * value).coerceAtLeast(barWidth)
            drawRoundRect(
                color = ink.copy(alpha = 0.28f),
                topLeft = Offset(
                    waveLeft + index * step + (step - barWidth) / 2f,
                    y + (h - barHeight) / 2f,
                ),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }

    /**
     * Les pastilles des mots-cles, les traits des citations et les separations.
     *
     * Le meme dessin qu'a l'ecran, refait ici : ce sont des decorations
     * calculees a partir de la mise en page, pas des caracteres, donc elles
     * n'existent nulle part ou l'on pourrait aller les chercher.
     */
    private fun DrawScope.drawJournalMarks(
        layout: TextLayoutResult,
        text: String,
        spans: List<TextSpan>,
        ink: Color,
        /**
         * La largeur du texte. Surtout pas `size.width` : dans un `DrawScope`,
         * la taille reste celle de la feuille entiere quels que soient les
         * decalages et les mises a l'echelle appliques autour.
         */
        contentWidth: Float,
    ) {
        // Les fonds de citation d'abord : ils sont derriere tout le reste. Poses
        // apres les pastilles des mots-cles, ils effaceraient un mot-cle ecrit
        // dans une citation.
        val quotes = spans.filter { it.style == TextStyleKind.QUOTE && !it.isEmpty }
        quotes.forEach { quote ->
            val first = layout.getLineForOffset(quote.start.coerceIn(0, text.length - 1))
            val last = layout.getLineForOffset((quote.end - 1).coerceIn(0, text.length - 1))
            val background = quoteFill(spans, quote, ink) ?: return@forEach
            drawRoundRect(
                color = background,
                topLeft = Offset(0f, layout.getLineTop(first)),
                size = Size(contentWidth, layout.getLineBottom(last) - layout.getLineTop(first)),
                cornerRadius = CornerRadius(QUOTE_RADIUS),
            )
        }

        Hashtag.rangesIn(text).forEach { range ->
            val start = range.first
            val end = range.last + 1
            val tint = hashtagTint(text.substring(start + 1, end))
            val line = layout.getLineForOffset(start)
            val left = layout.getHorizontalPosition(start, usePrimaryDirection = true)
            val right = layout.getHorizontalPosition(
                minOf(end, layout.getLineEnd(line, visibleEnd = true)),
                usePrimaryDirection = true,
            )
            val bottom = layout.getLineBottom(line)
            val size = layout.layoutInput.style.fontSize.value
            drawRoundRect(
                color = tint.copy(alpha = 0.18f),
                topLeft = Offset(left - size * 0.22f, bottom - size * 1.24f),
                size = Size((right - left) + size * 0.44f, size * 1.46f),
                cornerRadius = CornerRadius(size * 0.73f),
            )
        }

        // Puis les traits de citation, par-dessus les fonds.
        quotes.forEach { quote ->
            val first = layout.getLineForOffset(quote.start.coerceIn(0, text.length - 1))
            val last = layout.getLineForOffset((quote.end - 1).coerceIn(0, text.length - 1))
            val top = layout.getLineTop(first)
            val bottom = layout.getLineBottom(last)
            drawRoundRect(
                color = quoteTint(spans, quote, ink),
                topLeft = Offset(QUOTE_BAR_X, top + 3f),
                size = Size(QUOTE_BAR_W, (bottom - top - 6f).coerceAtLeast(1f)),
                cornerRadius = CornerRadius(QUOTE_BAR_W / 2f),
            )
        }

        spans.filter { it.style.isRule && !it.isEmpty }.forEach { rule ->
            val line = layout.getLineForOffset(rule.start.coerceIn(0, text.length - 1))
            val y = (layout.getLineTop(line) + layout.getLineBottom(line)) / 2f
            val thickness = ruleThickness(rule.style).value
            val ruleWidth = contentWidth * ruleWidth(rule.style)
            drawRoundRect(
                color = ink.copy(alpha = 0.45f),
                topLeft = Offset((contentWidth - ruleWidth) / 2f, y - thickness / 2f),
                size = Size(ruleWidth, thickness),
                cornerRadius = CornerRadius(thickness / 2f),
            )
        }
    }

    /**
     * La couleur du trait d'une citation.
     *
     * Elle se lit dans la famille **QUOTE_BAR**, la seule qui la porte.
     * L'ancienne version cherchait une couleur de *texte* : une citation n'en a
     * pas, donc le trait retombait toujours sur l'encre — noir, quelle que soit
     * la couleur choisie a l'ecran.
     */
    private fun quoteTint(spans: List<TextSpan>, quote: TextSpan, ink: Color): Color =
        spans.firstOrNull {
            it.style.family == StyleFamily.QUOTE_BAR &&
                it.start <= quote.start && it.end >= quote.end
        }?.let { Color(it.style.argb) } ?: ink

    /** Le fond d'une citation, ou `null` quand elle n'en a pas. */
    private fun quoteFill(spans: List<TextSpan>, quote: TextSpan, ink: Color): Color? {
        val fill = spans.firstOrNull {
            it.style.family == StyleFamily.QUOTE_FILL &&
                it.start <= quote.start && it.end >= quote.end
        }?.style ?: return null
        val tint = quoteTint(spans, quote, ink)
        return when (fill) {
            TextStyleKind.QUOTE_FILL_SOFT -> tint.copy(alpha = 0.08f)
            TextStyleKind.QUOTE_FILL_FULL -> tint.copy(alpha = 0.18f)
            else -> null
        }
    }

    /**
     * La derniere ligne de la page qui porte reellement quelque chose, ou
     * `null` si la page est vide.
     *
     * « Porter quelque chose » veut dire du texte, ou un trait de separation —
     * un trait est une ligne sans caractere visible, et l'oublier couperait la
     * page juste au-dessus de lui.
     */
    private fun lastContentLine(
        layout: TextLayoutResult,
        text: String,
        spans: List<TextSpan>,
    ): Int? {
        var last = -1
        for (line in 0 until layout.lineCount) {
            val from = layout.getLineStart(line)
            val to = layout.getLineEnd(line, visibleEnd = true).coerceAtMost(text.length)
            if (to > from && text.substring(from, to).isNotBlank()) last = line
        }
        spans.filter { it.style.isRule }.forEach { rule ->
            if (text.isEmpty()) return@forEach
            val line = layout.getLineForOffset(rule.start.coerceIn(0, text.length - 1))
            if (line > last) last = line
        }
        return last.takeIf { it >= 0 }
    }

    /** Une photo, a sa place, dans sa forme et son inclinaison. */
    private fun DrawScope.drawPlaced(item: MediaItem, image: ImageBitmap) {
        val x = item.placedX ?: return
        val y = item.placedY ?: return
        val w = item.placedWidth
        val h = item.displayHeight
        if (w <= 0f || h <= 0f) return

        rotate(item.placedRotation, pivot = Offset(x + w / 2f, y + h / 2f)) {
            val destination = Rect(x, y, x + w, y + h)
            val shape = item.shape
            val path = Path().apply {
                when {
                    shape == MediaShape.CIRCLE -> addOval(destination)
                    else -> addRect(destination)
                }
            }
            clipPath(path, ClipOp.Intersect) {
                // Recadrage au centre : la photo remplit son cadre sans se
                // deformer, exactement comme a l'ecran.
                val ratio = image.width.toFloat() / image.height.toFloat()
                val target = w / h
                val cropWidth: Int
                val cropHeight: Int
                if (ratio > target) {
                    cropHeight = image.height
                    cropWidth = (image.height * target).toInt().coerceAtLeast(1)
                } else {
                    cropWidth = image.width
                    cropHeight = (image.width / target).toInt().coerceAtLeast(1)
                }
                drawImage(
                    image = image,
                    srcOffset = IntOffset(
                        (image.width - cropWidth) / 2,
                        (image.height - cropHeight) / 2,
                    ),
                    srcSize = IntSize(cropWidth, cropHeight),
                    dstOffset = IntOffset(x.toInt(), y.toInt()),
                    dstSize = IntSize(w.toInt(), h.toInt()),
                )
            }
        }
    }

    /**
     * L'image, chargee a une taille raisonnable.
     *
     * Une photo de telephone fait douze millions de pixels ; a la taille ou
     * elle est posee sur la page, la moitie suffit largement, et le PDF reste
     * ouvrable sur un telephone.
     */
    private fun decode(file: File): ImageBitmap? {
        if (!file.exists()) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            val options = BitmapFactory.Options().apply {
                inSampleSize = 1
                while (longest / inSampleSize > MAX_IMAGE) inSampleSize *= 2
            }
            BitmapFactory.decodeFile(file.path, options)?.asImageBitmap()
        }.getOrNull()
    }

    private const val MAX_IMAGE = 1600
}
