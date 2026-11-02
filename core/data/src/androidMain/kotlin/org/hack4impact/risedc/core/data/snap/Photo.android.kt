package org.hack4impact.risedc.core.data.snap

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

internal actual suspend fun preparePhoto(bytes: ByteArray): PreparedPhoto? = withContext(Dispatchers.Default) {
    // Stop and return null if the input is not a supported JPEG/PNG image or is too large
    if (!supportedPhoto(bytes)) return@withContext null

    // Decode the dimensions of the image without loading it into memory, to avoid crashing on large images
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

    // If the image can't be decoded then the width or height will be <= 0, so return null
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

    // Downsample by a power of 2 to at most 2048px to save memory; the final resize below keeps the details while getting it 1024px.
    val options = BitmapFactory.Options().apply {
        inSampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / inSampleSize > MAX_IMAGE_SIDE * 2) inSampleSize *= 2
    }

    // Decode the image into a Bitmap
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return@withContext null

    // Resize the bitmap to at most 1024px on the longest side, keeping the aspect ratio
    val scale = minOf(1.0, MAX_IMAGE_SIDE.toDouble() / maxOf(bitmap.width, bitmap.height))

    // Create a new bitmap with the scaled dimensions
    val resized = Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * scale).toInt().coerceAtLeast(1),
        (bitmap.height * scale).toInt().coerceAtLeast(1),
        true,
    )

    try {
        // Encode the resized bitmap as JPEG at 85% quality into output, without metadata. Returns null if encoding fails.
        val output = ByteArrayOutputStream()
        if (!resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) return@withContext null
        PreparedPhoto(output.toByteArray(), "image/jpeg")
    } finally {
        // Recycle the bitmaps to free memory
        if (resized !== bitmap) resized.recycle()
        bitmap.recycle()
    }
}

private const val JPEG_QUALITY = 85
