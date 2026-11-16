package org.hack4impact.risedc.core.data.snap

// Processed photo that can be sent to Gemini, bytes is the image data, mimeType is the image format
internal data class PreparedPhoto(val bytes: ByteArray, val mimeType: String)

/** Resize JPEG/PNG to at most 1024 pixels on the longest side, then encode JPEG at 85% quality without metadata. */
internal expect suspend fun preparePhoto(bytes: ByteArray): PreparedPhoto?

internal const val MAX_IMAGE_SIDE = 1024
private const val MAX_INPUT_BYTES = 10 * 1024 * 1024 // max 10 MB input
private val JPEG_MAGIC = byteArrayOf(-1, -40, -1)
private val PNG_MAGIC = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)

internal fun supportedPhoto(bytes: ByteArray): Boolean = bytes.size in PNG_MAGIC.size..MAX_INPUT_BYTES &&
    listOf(JPEG_MAGIC, PNG_MAGIC).any { prefix -> prefix.indices.all { bytes[it] == prefix[it] } }
