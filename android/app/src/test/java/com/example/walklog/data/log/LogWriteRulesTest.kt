package com.example.walklog.data.log

import com.example.walklog.data.model.Visibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogWriteRulesTest {

    @Test
    fun decideCreate_noDocument_creates() {
        assertEquals(CreateDecision.CREATE, decideCreate(exists = false, existingAuthorId = null, uid = "me"))
    }

    @Test
    fun decideCreate_sameAuthor_isAlreadySaved() {
        assertEquals(CreateDecision.ALREADY_SAVED, decideCreate(exists = true, existingAuthorId = "me", uid = "me"))
    }

    @Test
    fun decideCreate_otherAuthor_isIdTaken() {
        assertEquals(CreateDecision.ID_TAKEN, decideCreate(exists = true, existingAuthorId = "other", uid = "me"))
    }

    @Test
    fun decideCreate_documentWithoutAuthor_isIdTaken() {
        assertEquals(CreateDecision.ID_TAKEN, decideCreate(exists = true, existingAuthorId = null, uid = "me"))
    }

    @Test
    fun decideChange_author_isAllowed() {
        assertEquals(ChangeDecision.ALLOWED, decideChange(exists = true, existingAuthorId = "me", uid = "me"))
    }

    @Test
    fun decideChange_noDocument_isNotFound() {
        assertEquals(ChangeDecision.NOT_FOUND, decideChange(exists = false, existingAuthorId = null, uid = "me"))
    }

    @Test
    fun decideChange_otherAuthor_isNotAuthor() {
        assertEquals(ChangeDecision.NOT_AUTHOR, decideChange(exists = true, existingAuthorId = "other", uid = "me"))
    }

    @Test
    fun decideChange_documentWithoutAuthor_isNotAuthor() {
        assertEquals(ChangeDecision.NOT_AUTHOR, decideChange(exists = true, existingAuthorId = null, uid = "me"))
    }

    @Test
    fun decideChange_notSignedIn_isNotAuthorEvenIfMissing() {
        assertEquals(ChangeDecision.NOT_AUTHOR, decideChange(exists = true, existingAuthorId = "me", uid = null))
        assertEquals(ChangeDecision.NOT_AUTHOR, decideChange(exists = false, existingAuthorId = null, uid = null))
    }

    @Test
    fun isVisibleTo_publicLog_isVisibleToEveryone() {
        assertTrue(isVisibleTo(Visibility.PUBLIC, authorId = "author", uid = "other"))
        assertTrue(isVisibleTo(Visibility.PUBLIC, authorId = "author", uid = null))
    }

    @Test
    fun isVisibleTo_privateLog_isVisibleOnlyToAuthor() {
        assertTrue(isVisibleTo(Visibility.PRIVATE, authorId = "me", uid = "me"))
        assertFalse(isVisibleTo(Visibility.PRIVATE, authorId = "author", uid = "other"))
    }

    @Test
    fun isVisibleTo_privateLog_isHiddenWhenNotSignedInOrNoAuthor() {
        assertFalse(isVisibleTo(Visibility.PRIVATE, authorId = "author", uid = null))
        assertFalse(isVisibleTo(Visibility.PRIVATE, authorId = null, uid = null))
        assertFalse(isVisibleTo(Visibility.PRIVATE, authorId = null, uid = "me"))
    }

    @Test
    fun normalizeTags_trimsDropsBlanksAndDuplicates() {
        assertEquals(
            listOf("hangang", "night"),
            normalizeTags(listOf(" hangang ", "", "   ", "night", "hangang", "night ")),
        )
    }

    @Test
    fun normalizeTags_keepsFirstOrderAndCase() {
        assertEquals(listOf("b", "a", "A"), normalizeTags(listOf("b", "a", "A", "b")))
        assertEquals(emptyList<String>(), normalizeTags(emptyList()))
    }

    @Test
    fun normalizeTag_trimsAndBlankIsNull() {
        assertEquals("hangang", normalizeTag("  hangang "))
        assertNull(normalizeTag(null))
        assertNull(normalizeTag(""))
        assertNull(normalizeTag("   "))
    }
}
