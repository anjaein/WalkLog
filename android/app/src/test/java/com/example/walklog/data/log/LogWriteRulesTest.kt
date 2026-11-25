package com.example.walklog.data.log

import org.junit.Assert.assertEquals
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
}
