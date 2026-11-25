package com.example.walklog.data.user

import org.junit.Assert.assertEquals
import org.junit.Test

class HandleClaimRulesTest {

    private data class Case(
        val name: String,
        val newHandleExists: Boolean,
        val newHandleOwner: String?,
        val oldHandle: String?,
        val oldHandleOwner: String?,
        val expected: ClaimDecision,
    )

    @Test
    fun decideClaim_table() {
        val cases = listOf(
            Case("새 아이디 · 예전 아이디 없음", false, null, null, null, ClaimDecision.Claim(null)),
            Case("새 아이디를 남이 씀", true, "other", null, null, ClaimDecision.Taken),
            Case("새 아이디 문서에 주인 값이 없음", true, null, null, null, ClaimDecision.Taken),
            Case("새 아이디를 남이 쓰면 예전 아이디도 그대로", true, "other", "old.walks", ME, ClaimDecision.Taken),
            Case("새 아이디가 이미 내 것", true, ME, null, null, ClaimDecision.Claim(null)),
            Case("같은 아이디로 다시 만듦", true, ME, NEW, ME, ClaimDecision.Claim(null)),
            Case("아이디를 바꾸면 내 예전 아이디를 풀어 줌", false, null, "old.walks", ME, ClaimDecision.Claim("old.walks")),
            Case("예전 아이디를 그사이 남이 차지함", false, null, "old.walks", "other", ClaimDecision.Claim(null)),
            Case("예전 아이디 문서가 없음", false, null, "old.walks", null, ClaimDecision.Claim(null)),
            Case("예전 아이디가 규칙에 안 맞음", false, null, "bad/handle", ME, ClaimDecision.Claim(null)),
        )
        cases.forEach { case ->
            val actual = decideClaim(
                newHandle = NEW,
                newHandleExists = case.newHandleExists,
                newHandleOwner = case.newHandleOwner,
                oldHandle = case.oldHandle,
                oldHandleOwner = case.oldHandleOwner,
                myUid = ME,
            )
            assertEquals(case.name, case.expected, actual)
        }
    }

    @Test
    fun releaseCandidate_table() {
        listOf(
            Triple("예전 아이디 없음", null, null),
            Triple("새 아이디와 같음", NEW, null),
            Triple("규칙에 안 맞음", "bad/handle", null),
            Triple("예약값", "__old__", null),
            Triple("다른 올바른 아이디", "old.walks", "old.walks"),
        ).forEach { (name, oldHandle, expected) ->
            assertEquals(name, expected, releaseCandidate(oldHandle, NEW))
        }
    }

    private companion object {
        const val ME = "me"
        const val NEW = "new.walks"
    }
}
