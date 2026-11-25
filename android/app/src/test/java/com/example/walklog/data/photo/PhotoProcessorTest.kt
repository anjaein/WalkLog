package com.example.walklog.data.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.TimeZone

class PhotoProcessorTest {

    private val seoul = TimeZone.getTimeZone("Asia/Seoul")

    @Test
    fun inSampleSize_keepsLongSideAtLeastTarget() {
        assertEquals(2, calculateInSampleSize(4032, 3024, 1600))
        assertEquals(4, calculateInSampleSize(3024, 8000, 1600))
        assertEquals(2, calculateInSampleSize(3200, 2400, 1600))
    }

    @Test
    fun inSampleSize_smallImageIsOne() {
        assertEquals(1, calculateInSampleSize(1200, 800, 1600))
        assertEquals(1, calculateInSampleSize(3000, 2000, 1600))
        assertEquals(1, calculateInSampleSize(0, 0, 1600))
    }

    @Test
    fun scaledSize_landscape() {
        assertEquals(1600 to 1200, scaledSize(4032, 3024, 1600))
    }

    @Test
    fun scaledSize_portrait() {
        assertEquals(1200 to 1600, scaledSize(3024, 4032, 1600))
    }

    @Test
    fun scaledSize_smallImageUnchanged() {
        assertEquals(1200 to 800, scaledSize(1200, 800, 1600))
        assertEquals(1600 to 900, scaledSize(1600, 900, 1600))
    }

    @Test
    fun parseExifDateTime_validInSeoul() {
        // 2026-11-26 09:30:00 KST = 2026-11-26 00:30:00 UTC
        assertEquals(1_795_653_000_000L, parseExifDateTime("2026:11:26 09:30:00", seoul))
    }

    @Test
    fun parseExifDateTime_trailingNulIsIgnored() {
        assertEquals(1_795_653_000_000L, parseExifDateTime("2026:11:26 09:30:00\u0000", seoul))
    }

    @Test
    fun parseExifDateTime_blankOrGarbageIsNull() {
        assertNull(parseExifDateTime(null, seoul))
        assertNull(parseExifDateTime("", seoul))
        assertNull(parseExifDateTime("   ", seoul))
        assertNull(parseExifDateTime("not a date", seoul))
        assertNull(parseExifDateTime("2026-11-26 09:30:00", seoul))
        assertNull(parseExifDateTime("0000:00:00 00:00:00", seoul))
        assertNull(parseExifDateTime("2026:11:26 09:30:00 extra", seoul))
    }

    @Test
    fun validPhotoLocation_keepsNormalCoordinates() {
        assertEquals(37.555 to 126.895, validPhotoLocation(37.555, 126.895))
        assertEquals(-33.86 to 151.2, validPhotoLocation(-33.86, 151.2))
        assertEquals(90.0 to -180.0, validPhotoLocation(90.0, -180.0))
        assertEquals(0.0 to 126.9, validPhotoLocation(0.0, 126.9))
    }

    @Test
    fun validPhotoLocation_redactedZeroIsNull() {
        assertNull(validPhotoLocation(0.0, 0.0))
    }

    @Test
    fun validPhotoLocation_outOfRangeIsNull() {
        assertNull(validPhotoLocation(90.1, 126.9))
        assertNull(validPhotoLocation(-90.1, 126.9))
        assertNull(validPhotoLocation(37.5, 180.1))
        assertNull(validPhotoLocation(37.5, -180.1))
    }

    @Test
    fun validPhotoLocation_nonFiniteIsNull() {
        assertNull(validPhotoLocation(Double.NaN, 126.9))
        assertNull(validPhotoLocation(37.5, Double.POSITIVE_INFINITY))
        assertNull(validPhotoLocation(Double.NEGATIVE_INFINITY, Double.NaN))
    }
}
