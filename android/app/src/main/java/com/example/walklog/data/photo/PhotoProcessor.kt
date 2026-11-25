package com.example.walklog.data.photo

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class PreparedPhoto(
    val jpeg: ByteArray,
    val lat: Double?,
    val lng: Double?,
    val takenAt: Long?,
)

class PhotoProcessor(private val contentResolver: ContentResolver) {

    suspend fun prepare(uri: Uri): PreparedPhoto = withContext(Dispatchers.IO) {
        val exif = open(uri).use { ExifInterface(it) }
        val latLong = FloatArray(2)
        val hasLatLong = exif.getLatLong(latLong)
        val takenAt = parseExifDateTime(
            exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME),
            TimeZone.getDefault(),
        )
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )

        PreparedPhoto(
            jpeg = encodeResized(uri, orientation),
            lat = if (hasLatLong) latLong[0].toDouble() else null,
            lng = if (hasLatLong) latLong[1].toDouble() else null,
            takenAt = takenAt,
        )
    }

    // 비트맵을 새로 JPEG로 압축하면 EXIF가 따라오지 않는다. 올리는 파일에서 위치 정보가 빠지는 건 이 때문이다.
    private fun encodeResized(uri: Uri, orientation: Int): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("이미지 크기를 읽을 수 없습니다: $uri")
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_LONG_SIDE)
            // Adobe RGB · Display P3 같은 넓은 색 공간 사진을 그대로 JPEG로 다시 저장하면 화면에 따라 색이 바래 보여서, 읽을 때 sRGB로 바꾼다
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
            }
        }
        val decoded = open(uri).use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("이미지를 디코딩할 수 없습니다: $uri")

        val (targetWidth, targetHeight) = scaledSize(decoded.width, decoded.height, MAX_LONG_SIDE)
        val matrix = Matrix().apply {
            postScale(
                targetWidth.toFloat() / decoded.width,
                targetHeight.toFloat() / decoded.height,
            )
            applyOrientation(orientation)
        }
        val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        // 변환할 게 없으면 createBitmap이 원본을 그대로 돌려줄 수 있어서, 같은 객체면 먼저 recycle하지 않는다.
        if (result !== decoded) decoded.recycle()

        return try {
            ByteArrayOutputStream().use { out ->
                if (!result.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) {
                    throw IOException("JPEG로 압축하지 못했습니다: $uri")
                }
                out.toByteArray()
            }
        } finally {
            result.recycle()
        }
    }

    private fun open(uri: Uri): InputStream =
        contentResolver.openInputStream(uri) ?: throw IOException("사진을 열 수 없습니다: $uri")

    private fun Matrix.applyOrientation(orientation: Int) {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                postRotate(90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                postRotate(-90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(-90f)
        }
    }

    private companion object {
        const val MAX_LONG_SIDE = 1600
        const val JPEG_QUALITY = 80
    }
}

/** 디코딩한 긴 변이 [maxLongSide] 아래로 내려가지 않는 가장 큰 2의 거듭제곱. */
internal fun calculateInSampleSize(width: Int, height: Int, maxLongSide: Int): Int {
    val longSide = maxOf(width, height)
    if (longSide <= 0 || maxLongSide <= 0) return 1
    var sampleSize = 1
    while (longSide / (sampleSize * 2) >= maxLongSide) {
        sampleSize *= 2
    }
    return sampleSize
}

/** 긴 변을 [maxLongSide] 이하로 줄인 크기. 이미 작으면 키우지 않고 그대로 둔다. */
internal fun scaledSize(width: Int, height: Int, maxLongSide: Int): Pair<Int, Int> {
    val longSide = maxOf(width, height)
    if (longSide <= maxLongSide || longSide <= 0) return width to height
    val ratio = maxLongSide.toDouble() / longSide
    val scaledWidth = Math.round(width * ratio).toInt().coerceAtLeast(1)
    val scaledHeight = Math.round(height * ratio).toInt().coerceAtLeast(1)
    return scaledWidth to scaledHeight
}

/** EXIF 촬영 시각("yyyy:MM:dd HH:mm:ss")에는 시간대가 없어서, 찍은 기기의 시간대로 보고 [timeZone]을 받는다. */
internal fun parseExifDateTime(value: String?, timeZone: TimeZone): Long? {
    // 일부 기기는 문자열 끝에 NUL 문자를 붙여서 공백과 함께 잘라낸다.
    val text = value?.trim { it <= ' ' }
    if (text.isNullOrEmpty()) return null
    val format = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).apply {
        isLenient = false
        this.timeZone = timeZone
    }
    val position = ParsePosition(0)
    val date = format.parse(text, position) ?: return null
    if (position.index != text.length) return null
    return date.time
}
