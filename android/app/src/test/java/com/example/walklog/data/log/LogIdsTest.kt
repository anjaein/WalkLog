package com.example.walklog.data.log

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LogIdsTest {

    @Test
    fun isValid_acceptsAllowedIds() {
        listOf("3f2b8c1e-9a4d-4e6f-b1c2-7d8e9f0a1b2c", "draft-123", "a_b", "__x", "x__", "a".repeat(64)).forEach {
            assertTrue(it, LogIds.isValid(it))
        }
    }

    @Test
    fun isValid_rejectsBrokenIds() {
        listOf("", "a/b", "a b", "한글", "a".repeat(65), "__x__").forEach {
            assertFalse(it, LogIds.isValid(it))
        }
    }

    @Test
    fun requireValid_throwsIllegalArgumentForInvalidId() {
        assertThrows(IllegalArgumentException::class.java) { LogIds.requireValid("a/b") }
    }

    @Test
    fun requireValid_passesForValidId() {
        LogIds.requireValid("draft-123")
    }
}
