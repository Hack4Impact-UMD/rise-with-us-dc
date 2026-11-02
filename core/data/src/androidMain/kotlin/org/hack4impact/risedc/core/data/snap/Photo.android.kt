package org.hack4impact.risedc.core.data.snap

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

internal actual suspend fun preparePhoto(bytes: ByteArray): PreparedPhoto? = withContext(Dispatchers.Default) {
    // Stop and return null if the input is not a supported JPEG/PNG image or is too large
    if (!supportedPhoto(bytes)) return@withContext null

    // Decode the dimensions of the image without loading it into memory, to avoid crashing on large images
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

    // If the image can't be decoded then the width or height will be <= 0, so return null
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

    // Downsample by a power of 2 to at most 2048px to save memory; the final resize below keeps
    // the details while getting it 1024px.
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

    // Apply the photo's EXIF orientation to the pixels, since re-encoding below drops that metadata
    val upright = orientationMatrix(bytes)?.let {
        Bitmap.createBitmap(resized, 0, 0, resized.width, resized.height, it, true)
    } ?: resized

    try {
        // Encode the upright bitmap as JPEG at 85% quality into output, without metadata.
        val output = ByteArrayOutputStream()
        if (!upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) return@withContext null
        PreparedPhoto(output.toByteArray(), "image/jpeg")
    } finally {
        // Recycle the bitmaps to free memory. Some of these can be the same bitmap.
        listOf(upright, resized, bitmap).distinct().forEach { it.recycle() }
    }
}

/** Returns the transform that makes the photo upright, or null if it's already upright or has no orientation. */
private fun orientationMatrix(bytes: ByteArray): Matrix? {
    val orientation = try {
        ExifInterface(ByteArrayInputStream(bytes))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (_: IOException) {
        ExifInterface.ORIENTATION_NORMAL
    }
    val (degrees, mirrored) = ORIENTATION_TRANSFORMS[orientation] ?: return null
    return Matrix().apply {
        setRotate(degrees)
        if (mirrored) postScale(-1f, 1f)
    }
}

/** Clockwise rotation, then whether to mirror horizontally, for each non-upright EXIF orientation. */
private val ORIENTATION_TRANSFORMS = mapOf(
    ExifInterface.ORIENTATION_FLIP_HORIZONTAL to (0f to true),
    ExifInterface.ORIENTATION_ROTATE_180 to (180f to false),
    ExifInterface.ORIENTATION_FLIP_VERTICAL to (180f to true),
    ExifInterface.ORIENTATION_TRANSPOSE to (90f to true),
    ExifInterface.ORIENTATION_ROTATE_90 to (90f to false),
    ExifInterface.ORIENTATION_TRANSVERSE to (-90f to true),
    ExifInterface.ORIENTATION_ROTATE_270 to (-90f to false),
)

private const val JPEG_QUALITY = 85
