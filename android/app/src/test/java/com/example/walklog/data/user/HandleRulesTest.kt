package com.example.walklog.data.user

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandleRulesTest {

    @Test
    fun isValid_acceptsAllowedHandles() {
        listOf("seokdu.walks", "mangwon_cat", "abc", "a".repeat(20), "__abc", "abc__", "_a_").forEach {
            assertTrue(it, HandleRules.isValid(it))
        }
    }

    @Test
    fun isValid_rejectsBrokenHandles() {
        listOf("ab", "a".repeat(21), ".abc", "abc.", "a..b", "seok/du", "한글", "has space", "", "__abc__", "____", "___").forEach {
            assertFalse(it, HandleRules.isValid(it))
        }
    }

    @Test
    fun normalize_trimsAndLowercases() {
        assertEquals("seokdu.walks", HandleRules.normalize("  SeokDu.Walks \n"))
    }
}
