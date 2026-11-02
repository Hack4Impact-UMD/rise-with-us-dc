@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package org.hack4impact.risedc.core.data.snap

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.test.runTest
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImageOrientation
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIRectFill
import platform.posix.memcpy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PhotoTest {
    @Test
    fun downsamplesPngAndKeepsSmallImagesAtOriginalSize() = runTest {
        for ((inputWidth, expectedWidth) in listOf(2048 to 1024, 100 to 100)) {
            val prepared = assertNotNull(preparePhoto(fixture(inputWidth, inputWidth / 2, png = true)))
            assertEquals("image/jpeg", prepared.mimeType)
            assertEquals(listOf(-1, -40, -1), prepared.bytes.take(3).map { it.toInt() })
            val image = assertNotNull(UIImage.imageWithData(prepared.bytes.toData()))
            image.size.useContents {
                assertEquals(expectedWidth.toDouble(), this.width)
                assertEquals((expectedWidth / 2).toDouble(), this.height)
            }
        }
    }

    @Test
    fun appliesExifOrientationBeforeStrippingIt() = runTest {
        val jpeg = fixture(2048, 1024, png = false)
        // Synthetic EXIF APP1: little-endian TIFF with orientation 6 (90 degrees clockwise).
        val orientation = byteArrayOf(
            -1, -31, 0, 34, 69, 120, 105, 102, 0, 0,
            73, 73, 42, 0, 8, 0, 0, 0, 1, 0,
            18, 1, 3, 0, 1, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0,
        )
        val oriented = jpeg.copyOfRange(0, 2) + orientation + jpeg.copyOfRange(2, jpeg.size)
        val prepared = assertNotNull(preparePhoto(oriented))
        val image = assertNotNull(UIImage.imageWithData(prepared.bytes.toData()))
        assertEquals(UIImageOrientation.UIImageOrientationUp, image.imageOrientation)
        image.size.useContents {
            assertEquals(512.0, width)
            assertEquals(1024.0, height)
        }
    }

    @Test
    fun rejectsTruncatedAndUnsupportedImages() = runTest {
        assertNull(preparePhoto(byteArrayOf()))
        assertNull(preparePhoto(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)))
    }

    private fun fixture(width: Int, height: Int, png: Boolean): ByteArray {
        val format = UIGraphicsImageRendererFormat.defaultFormat().apply { scale = 1.0 }
        val image = UIGraphicsImageRenderer(
            size = CGSizeMake(width.toDouble(), height.toDouble()),
            format = format,
        ).imageWithActions {
            UIColor.greenColor.setFill()
            UIRectFill(CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()))
        }
        val data = if (png) UIImagePNGRepresentation(image) else UIImageJPEGRepresentation(image, 0.85)
        val encoded = assertNotNull(data)
        return ByteArray(encoded.length.toInt()).also { result ->
            result.usePinned { memcpy(it.addressOf(0), encoded.bytes, encoded.length) }
        }
    }

    private fun ByteArray.toData(): NSData = usePinned {
        NSData.create(bytes = it.addressOf(0), length = size.toULong())
    }
}
