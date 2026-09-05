package com.ismael.daybyday.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.exifinterface.media.ExifInterface
import com.ismael.daybyday.data.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Chargement des vignettes photo / video sans bibliotheque externe :
 * tout est decode localement depuis le stockage prive de l'application.
 */
object MediaLoader {

    private val cache = LruCache<String, ImageBitmap>(24)

    suspend fun load(file: File, kind: MediaKind, maxSize: Int, useCache: Boolean): ImageBitmap? =
        withContext(Dispatchers.IO) {
            val key = "${file.path}:$maxSize"
            if (useCache) cache.get(key)?.let { return@withContext it }
            val bitmap = when (kind) {
                MediaKind.PHOTO -> decodePhoto(file, maxSize)
                MediaKind.VIDEO -> decodeVideoFrame(file, maxSize)
            } ?: return@withContext null
            val image = bitmap.asImageBitmap()
            if (useCache) cache.put(key, image)
            image
        }

    private fun decodePhoto(file: File, maxSize: Int): Bitmap? {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSize)
        }
        val bitmap = BitmapFactory.decodeFile(file.path, options) ?: return null
        val rotation = runCatching {
            when (ExifInterface(file.path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)
        return rotate(bitmap, rotation)
    }

    private fun decodeVideoFrame(file: File, maxSize: Int): Bitmap? {
        if (!file.exists()) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.path)
            val frame = retriever.getFrameAtTime(0) ?: return null
            val rotation = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toFloatOrNull() ?: 0f
            val scaled = scaleDown(frame, maxSize)
            rotate(scaled, rotation)
        } catch (error: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun sampleSizeFor(width: Int, height: Int, maxSize: Int): Int {
        var sample = 1
        var currentWidth = width
        var currentHeight = height
        while (currentWidth / 2 >= maxSize && currentHeight / 2 >= maxSize) {
            currentWidth /= 2
            currentHeight /= 2
            sample *= 2
        }
        return sample
    }

    private fun scaleDown(bitmap: Bitmap, maxSize: Int): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= maxSize) return bitmap
        val ratio = maxSize.toFloat() / largest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    }

    private fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Le rapport hauteur / largeur d'un media, sans le decoder entierement.
     * Une photo qu'on vient de poser doit garder ses proportions : la caler
     * d'office en carre defigurerait tout ce qui n'est pas carre.
     */
    suspend fun aspectRatio(file: File, kind: MediaKind): Float = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext 1f
        val size = when (kind) {
            MediaKind.PHOTO -> photoSize(file)
            MediaKind.VIDEO -> videoSize(file)
        } ?: return@withContext 1f
        val (width, height) = size
        if (width <= 0 || height <= 0) 1f else height.toFloat() / width.toFloat()
    }

    private fun photoSize(file: File): Pair<Int, Int>? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        // Une photo prise a la verticale porte ses dimensions a l'endroit et
        // son orientation a part : sans ca, un portrait ressort en paysage.
        val quarterTurn = runCatching {
            when (ExifInterface(file.path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )) {
                ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_ROTATE_270 -> true
                else -> false
            }
        }.getOrDefault(false)
        return if (quarterTurn) {
            bounds.outHeight to bounds.outWidth
        } else {
            bounds.outWidth to bounds.outHeight
        }
    }

    private fun videoSize(file: File): Pair<Int, Int>? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.path)
            val width = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            val height = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            val quarterTurn = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull()?.let { it == 90 || it == 270 } ?: false
            if (width == null || height == null) {
                null
            } else if (quarterTurn) {
                height to width
            } else {
                width to height
            }
        } catch (error: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun clear() = cache.evictAll()
}

/**
 * Une image posee comme un autocollant : pas de recadrage, pas de cadre, la
 * transparence gardee telle quelle.
 *
 * Le contour blanc suit la **silhouette** de l'image, pas son rectangle. Il est
 * obtenu en redessinant la meme image huit fois autour, toute blanche : ce que
 * la transparence laisse voir dessine le tour. Un seul decodage sert aux neuf
 * passes, donc ca ne coute rien de plus a charger.
 */
@Composable
fun StickerImage(
    file: File,
    kind: MediaKind,
    outline: Dp,
    modifier: Modifier = Modifier,
) {
    val image by produceState<ImageBitmap?>(initialValue = null, file.path) {
        value = MediaLoader.load(file, kind, 1280, useCache = false)
    }
    val bitmap = image ?: return

    Canvas(modifier = modifier) {
        // L'image entiere tient dans la place disponible, sans deformation.
        val scale = minOf(size.width / bitmap.width, size.height / bitmap.height)
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val left = ((size.width - width) / 2f).toInt()
        val top = ((size.height - height) / 2f).toInt()
        val target = IntSize(width, height)

        val ring = outline.toPx()
        if (ring > 0f) {
            val white = ColorFilter.tint(Color.White, BlendMode.SrcIn)
            val diagonal = (ring * 0.7071f)
            listOf(
                ring to 0f, -ring to 0f, 0f to ring, 0f to -ring,
                diagonal to diagonal, -diagonal to diagonal,
                diagonal to -diagonal, -diagonal to -diagonal,
            ).forEach { (dx, dy) ->
                drawImage(
                    image = bitmap,
                    dstOffset = IntOffset(left + dx.toInt(), top + dy.toInt()),
                    dstSize = target,
                    colorFilter = white,
                )
            }
        }

        drawImage(
            image = bitmap,
            dstOffset = IntOffset(left, top),
            dstSize = target,
        )
    }
}

@Composable
fun MediaImage(
    file: File,
    kind: MediaKind,
    modifier: Modifier = Modifier,
    maxSize: Int = 512,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val image by produceState<ImageBitmap?>(initialValue = null, file.path, maxSize) {
        value = MediaLoader.load(file, kind, maxSize, useCache = maxSize <= 512)
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        image?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        }
    }
}
