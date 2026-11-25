package com.example.walklog.data.auth

import com.example.walklog.data.InvalidCredentialsException
import com.example.walklog.data.InvalidEmailException
import com.example.walklog.data.WeakPasswordException
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthInputRulesTest {

    @Test
    fun signUp_blankEmail_isInvalidEmail() {
        assertTrue(signUpInputFailure("  ", "secret12") is InvalidEmailException)
    }

    @Test
    fun signUp_emptyPassword_isWeakPassword() {
        assertTrue(signUpInputFailure("a@b.com", "") is WeakPasswordException)
    }

    @Test
    fun signIn_blankEmail_isInvalidEmail() {
        assertTrue(signInInputFailure("", "") is InvalidEmailException)
    }

    @Test
    fun signIn_emptyPassword_isInvalidCredentials() {
        assertTrue(signInInputFailure("a@b.com", "") is InvalidCredentialsException)
    }

    @Test
    fun whitespaceOnlyPassword_passesToFirebase() {
        assertNull(signUpInputFailure("a@b.com", "      "))
        assertNull(signInInputFailure("a@b.com", "      "))
    }

    @Test
    fun passwordReset_blankEmail_isInvalidEmail() {
        assertTrue(passwordResetInputFailure("") is InvalidEmailException)
    }

    @Test
    fun google_blankToken_isInvalidCredentials() {
        assertTrue(googleTokenInputFailure("") is InvalidCredentialsException)
    }

    @Test
    fun filledInputs_passToFirebase() {
        assertNull(signUpInputFailure("a@b.com", "secret12"))
        assertNull(signInInputFailure("a@b.com", "secret12"))
        assertNull(passwordResetInputFailure("a@b.com"))
        assertNull(googleTokenInputFailure("token"))
    }
}
