package com.example.walklog.data.photo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.example.walklog.data.PhotoUnreadableException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.SimpleTimeZone
import java.util.TimeZone

class PreparedPhoto(
    val jpeg: ByteArray,
    val lat: Double?,
    val lng: Double?,
    val takenAt: Long?,
)

class PhotoProcessor(context: Context) {

    private val appContext = context.applicationContext
    private val contentResolver = appContext.contentResolver

    /** 사진을 열거나 읽거나 다시 압축하지 못하면 [PhotoUnreadableException]을 던진다. */
    suspend fun prepare(uri: Uri): PreparedPhoto = withContext(Dispatchers.IO) {
        try {
            read(uri)
        } catch (e: Exception) {
            // ContentResolver 는 IOException 말고도 SecurityException(권한) · IllegalArgumentException(잘못된 주소)을 던진다
            throw PhotoUnreadableException(e)
        }
    }

    private fun read(uri: Uri): PreparedPhoto {
        val exif = readExif(uri)
        val latLong = FloatArray(2)
        val location = if (exif.getLatLong(latLong)) {
            validPhotoLocation(latLong[0].toDouble(), latLong[1].toDouble())
        } else {
            null
        }
        val takenAt = readTakenAt(exif)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )

        return PreparedPhoto(
            jpeg = encodeResized(uri, orientation),
            lat = location?.first,
            lng = location?.second,
            takenAt = takenAt,
        )
    }

    // 날짜 태그와 짝이 되는 시간대 태그를 함께 읽는다. 다른 짝의 시간대를 섞으면 시각이 틀어질 수 있다
    private fun readTakenAt(exif: ExifInterface): Long? {
        val original = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        val (dateTime, offset) = if (original != null) {
            original to readOffset(exif, original = true)
        } else {
            exif.getAttribute(ExifInterface.TAG_DATETIME) to readOffset(exif, original = false)
        }
        return parseExifDateTime(dateTime, TimeZone.getDefault(), offset)
    }

    // 플랫폼 ExifInterface 는 API 30부터 시간대 태그를 읽는다. 그 아래에서는 기기 시간대로 계산된다
    private fun readOffset(exif: ExifInterface, original: Boolean): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val tag = if (original) ExifInterface.TAG_OFFSET_TIME_ORIGINAL else ExifInterface.TAG_OFFSET_TIME
        return exif.getAttribute(tag)
    }

    // 비트맵을 새로 JPEG로 압축하면 EXIF가 따라오지 않는다. 올리는 파일에서 위치 정보가 빠지는 건 이 때문이다.
    private fun encodeResized(uri: Uri, orientation: Int): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("Cannot read image size: $uri")
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_LONG_SIDE)
            // Adobe RGB · Display P3 같은 넓은 색 공간 사진을 그대로 JPEG로 다시 저장하면 화면에 따라 색이 바래 보여서, 읽을 때 sRGB로 바꾼다
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
            }
        }
        val decoded = open(uri).use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Cannot decode image: $uri")

        val (targetWidth, targetHeight) = scaledSize(decoded.width, decoded.height, MAX_LONG_SIDE)
        val matrix = Matrix().apply {
            postScale(
                targetWidth.toFloat() / decoded.width,
                targetHeight.toFloat() / decoded.height,
            )
            applyOrientation(orientation)
        }
        val transformed = decoded.replaceWith { Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true) }
        // JPEG 는 투명도를 담지 못해 투명한 부분이 검게 저장되므로, 흰 바탕에 먼저 그린다
        val result = if (transformed.hasAlpha()) transformed.replaceWith { it.onWhiteBackground() } else transformed

        return try {
            ByteArrayOutputStream().use { out ->
                if (!result.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) {
                    throw IOException("Cannot compress image to JPEG: $uri")
                }
                out.toByteArray()
            }
        } finally {
            result.recycle()
        }
    }

    // 변환할 게 없으면 createBitmap이 원본을 그대로 돌려줄 수 있어서, 같은 객체면 recycle하지 않는다.
    private inline fun Bitmap.replaceWith(transform: (Bitmap) -> Bitmap): Bitmap {
        val replaced = try {
            transform(this)
        } catch (e: Throwable) {
            recycle()
            throw e
        }
        if (replaced !== this) recycle()
        return replaced
    }

    private fun Bitmap.onWhiteBackground(): Bitmap {
        val flattened = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(flattened).apply {
            drawColor(Color.WHITE)
            drawBitmap(this@onWhiteBackground, 0f, 0f, null)
        }
        return flattened
    }

    private fun open(uri: Uri): InputStream =
        contentResolver.openInputStream(uri) ?: throw IOException("Cannot open photo: $uri")

    // Android 10+ 의 MediaStore 는 ACCESS_MEDIA_LOCATION 권한으로 원본을 요청하지 않으면 EXIF 위치를 지운(0으로 채운) 사본을 준다.
    // 원본을 못 열거나 원본의 EXIF 를 못 읽으면 일반 주소로 다시 읽고, 위치만 비게 된다
    private fun readExif(uri: Uri): ExifInterface {
        openOriginal(uri)?.let { original ->
            try {
                return original.use { ExifInterface(it) }
            } catch (e: Exception) {
                // 아래에서 일반 주소로 다시 읽는다
            }
        }
        return open(uri).use { ExifInterface(it) }
    }

    private fun openOriginal(uri: Uri): InputStream? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            uri.authority != MediaStore.AUTHORITY ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        return try {
            contentResolver.openInputStream(MediaStore.setRequireOriginal(uri))
        } catch (e: Exception) {
            null
        }
    }

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

/**
 * EXIF 위치가 쓸 수 있는 값이면 (위도, 경도), 아니면 null.
 * 위치를 지운 사진은 (0, 0)으로 읽히는 경우가 많아서, 정확히 (0, 0)도 위치 없음으로 본다.
 */
internal fun validPhotoLocation(lat: Double, lng: Double): Pair<Double, Double>? {
    if (!lat.isFinite() || !lng.isFinite()) return null
    if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
    if (lat == 0.0 && lng == 0.0) return null
    return lat to lng
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

/**
 * EXIF 촬영 시각("yyyy:MM:dd HH:mm:ss")을 epoch 밀리초로 바꾼다.
 * 시간대 태그([offset], "+09:00" 꼴)가 올바르면 그 시간대로, 없거나 잘못됐으면 찍은 기기의 시간대로 보고 [timeZone]을 쓴다.
 */
internal fun parseExifDateTime(value: String?, timeZone: TimeZone, offset: String? = null): Long? {
    val text = value?.trimExif()
    if (text.isNullOrEmpty()) return null
    val zone = parseExifOffsetMillis(offset)?.let { SimpleTimeZone(it, "EXIF") } ?: timeZone
    val format = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).apply {
        isLenient = false
        this.timeZone = zone
    }
    val position = ParsePosition(0)
    val date = format.parse(text, position) ?: return null
    if (position.index != text.length) return null
    return date.time
}

private val EXIF_OFFSET = Regex("^([+-])(\\d{2}):(\\d{2})$")

// 모르는 시간대는 "   :  " 같은 공백으로 채워 오기도 해서, 꼴이 맞지 않으면 null 로 보고 기기 시간대로 넘긴다
internal fun parseExifOffsetMillis(offset: String?): Int? {
    val text = offset?.trimExif()
    if (text.isNullOrEmpty()) return null
    if (text == "Z") return 0
    val match = EXIF_OFFSET.matchEntire(text) ?: return null
    val (sign, hours, minutes) = match.destructured
    val h = hours.toInt()
    val m = minutes.toInt()
    if (h > MAX_OFFSET_HOURS || m >= 60 || (h == MAX_OFFSET_HOURS && m != 0)) return null
    val millis = (h * 60 + m) * 60_000
    return if (sign == "-") -millis else millis
}

// java.time.ZoneOffset 과 같은 한계(±18:00)
private const val MAX_OFFSET_HOURS = 18

// 일부 기기는 문자열 끝에 NUL 문자를 붙인다. trim() 은 NUL 을 공백으로 보지 않아서 제어 문자까지 함께 잘라낸다
private fun String.trimExif(): String = trim { it <= ' ' }
