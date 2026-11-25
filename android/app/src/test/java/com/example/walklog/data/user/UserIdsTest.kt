package com.example.walklog.data.user

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class UserIdsTest {

    @Test
    fun isValid_acceptsAllowedUids() {
        listOf("Xy3kP9qLmN0aBcD1eF2gH3iJ4kL5", "user-1", "__x", "x__", "a", "a.b", "...", ".a").forEach {
            assertTrue(it, UserIds.isValid(it))
        }
    }

    @Test
    fun isValid_rejectsBrokenUids() {
        listOf("", "   ", "\n", "a/b", "/", "__x__", "____", ".", "..").forEach {
            assertFalse(it, UserIds.isValid(it))
        }
    }

    @Test
    fun requireValid_throwsIllegalArgumentForInvalidUid() {
        assertThrows(IllegalArgumentException::class.java) { UserIds.requireValid("a/b") }
        assertThrows(IllegalArgumentException::class.java) { UserIds.requireValid("") }
    }

    @Test
    fun requireValid_passesForValidUid() {
        UserIds.requireValid("user-1")
    }
}
