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
import com.ismael.daybyday.data.StyleFamily
import com.ismael.daybyday.data.TextSpan
import com.ismael.daybyday.data.TextStyleKind
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
        photoFile: (MediaItem) -> File,
        paper: Color,
        ink: Color,
        lineColor: Color,
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
        val textHeight = bodyLayout.size.height.toFloat()
        // La page descend jusqu'au plus bas des deux : le texte, ou la photo la
        // plus basse. Une photo posee sous le dernier mot ne doit pas etre
        // coupee parce que le texte s'arretait avant elle.
        val lowestPhoto = photos.filter { it.isPlaced }
            .maxOfOrNull { (it.placedY ?: 0f) + it.displayHeight } ?: 0f
        val contentHeight = maxOf(textTop + textHeight + topPadding, lowestPhoto + topPadding)

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
    ) {
        drawRect(color = paper, topLeft = Offset.Zero, size = Size(width, height))

        if (ruled) {
            val spacing = JournalPaper.LINE_SPACING.value
            val margin = JournalPaper.SIDE_MARGIN.value
            var y = topPadding + spacing
            while (y < height) {
                drawLine(
                    color = lineColor,
                    start = Offset(margin, y),
                    end = Offset(width - margin, y),
                    strokeWidth = 1f,
                )
                y += spacing
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

        spans.filter { it.style == TextStyleKind.QUOTE && !it.isEmpty }.forEach { quote ->
            val first = layout.getLineForOffset(quote.start.coerceIn(0, text.length - 1))
            val last = layout.getLineForOffset((quote.end - 1).coerceIn(0, text.length - 1))
            val tint = spans.firstOrNull {
                it.style.family == StyleFamily.COLOR &&
                    it.start <= quote.start && it.end >= quote.end
            }?.let { Color(it.style.argb) } ?: ink
            drawRoundRect(
                color = tint,
                topLeft = Offset(layout.getLineLeft(first), layout.getLineTop(first) + 5f),
                size = Size(3f, layout.getLineBottom(last) - layout.getLineTop(first) - 10f),
                cornerRadius = CornerRadius(1.5f),
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
