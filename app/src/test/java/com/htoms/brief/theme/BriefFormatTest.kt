package com.htoms.brief.theme

import org.junit.Assert.assertEquals
import org.junit.Test

/** iOS BriefFormat과 동일한 통화·건수·축약 표기 규칙 검증. */
class BriefFormatTest {

    @Test
    fun numberUsesCommaGrouping() {
        assertEquals("1,067", BriefFormat.number(1_067))
        assertEquals("20,353", BriefFormat.number(20_353))
        assertEquals("0", BriefFormat.number(0))
    }

    @Test
    fun wonAndCountAppendUnits() {
        assertEquals("1,067원", BriefFormat.won(1_067))
        assertEquals("1,104건", BriefFormat.count(1_104))
    }

    @Test
    fun compactWonAbbreviatesEokAndManwon() {
        assertEquals("2억", BriefFormat.compactWon(200_000_000L))
        assertEquals("1.5억", BriefFormat.compactWon(150_000_000L))
        assertEquals("2만", BriefFormat.compactWon(20_000))
        assertEquals("1,000만", BriefFormat.compactWon(10_000_000L))
        assertEquals("9,999", BriefFormat.compactWon(9_999))
    }
}
