@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package org.hack4impact.risedc.core.data.snap

import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFBooleanFalse
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFNumberIntType
import platform.CoreGraphics.CGImageRef
import platform.ImageIO.CGImageSourceCreateThumbnailAtIndex
import platform.ImageIO.CGImageSourceCreateWithData
import platform.ImageIO.kCGImageSourceCreateThumbnailFromImageAlways
import platform.ImageIO.kCGImageSourceCreateThumbnailWithTransform
import platform.ImageIO.kCGImageSourceShouldCache
import platform.ImageIO.kCGImageSourceShouldCacheImmediately
import platform.ImageIO.kCGImageSourceThumbnailMaxPixelSize
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy

/* Downsamples with ImageIO, then encodes JPEG at 85% quality without metadata.
Returns null for unsupported input or processing failure. */
internal actual suspend fun preparePhoto(bytes: ByteArray): PreparedPhoto? = withContext(Dispatchers.Default) {
    // Stop and return null if the input is not a supported JPEG/PNG image or is too large
    if (!supportedPhoto(bytes)) return@withContext null

    createThumbnail(bytes).useCF { thumbnail ->
        // stop processing if the coroutine is cancelled, to avoid wasting CPU and memory
        ensureActive()
        // Encodes the thumbnail as JPEG at 85% quality, without metadata. Returns null if encoding fails.
        val encoded = UIImageJPEGRepresentation(UIImage.imageWithCGImage(thumbnail), JPEG_QUALITY)
            ?: return@withContext null

        // If the encoded data is empty, return null
        if (encoded.length == 0uL) return@withContext null

        // Copy the encoded data to a new ByteArray, because the NSData returned by
        // UIImageJPEGRepresentation is not guaranteed to be valid after the function returns.
        val result = ByteArray(encoded.length.toInt())
        result.usePinned { memcpy(it.addressOf(0), encoded.bytes, encoded.length) }
        PreparedPhoto(result, "image/jpeg")
    }
}

/** Returns an owned thumbnail; the caller must release it. */
private fun createThumbnail(bytes: ByteArray): CGImageRef? {
    // Copies Kotlin photo bytes into a CFData object that can be read by ImageIO
    val data = bytes.usePinned { CFDataCreate(null, it.addressOf(0).reinterpret(), bytes.size.toLong()) }

    return data.useCF { input ->
        CFDictionaryCreateMutable(null, 0, null, null).useCF { options ->
            // Prevents ImageIO from caching the input image in memory which wastes memory and can cause crashes
            // on large images. Instead only cache the thumbnail.
            CFDictionarySetValue(options, kCGImageSourceShouldCache, kCFBooleanFalse)

            // Create an image source from the input data
            CGImageSourceCreateWithData(input, options).useCF { source ->
                memScoped {
                    val maxSide = alloc<IntVar> { value = MAX_IMAGE_SIDE }

                    // Sets pixel limit, rotation and decoding options then creates the thumbnail from the source image
                    CFNumberCreate(null, kCFNumberIntType, maxSide.ptr).useCF { size ->
                        CFDictionarySetValue(options, kCGImageSourceThumbnailMaxPixelSize, size)
                        CFDictionarySetValue(options, kCGImageSourceCreateThumbnailFromImageAlways, kCFBooleanTrue)
                        CFDictionarySetValue(options, kCGImageSourceCreateThumbnailWithTransform, kCFBooleanTrue)
                        // Cache the thumbnail immediately since it needs to be used for JPEG encoding right after the function returns
                        CFDictionarySetValue(options, kCGImageSourceShouldCacheImmediately, kCFBooleanTrue)
                        // Create a thumbnail image from the source image using the specified options. Returns null if the thumbnail can't be created.
                        CGImageSourceCreateThumbnailAtIndex(source, 0u, options)
                    }
                }
            }
        }
    }
}

// Safely uses a Core Foundation object and releases it's memory after the block is executed
private inline fun <T : CPointed, R> CPointer<T>?.useCF(block: (CPointer<T>) -> R): R? {
    // return null if the pointer is null, to avoid calling the block with a null pointer which may crash
    if (this == null) return null

    // Call the block on the pointer and release the memory after, even if the block throws an exception
    return try {
        block(this)
    } finally {
        CFRelease(this)
    }
}

private const val JPEG_QUALITY = 0.85
