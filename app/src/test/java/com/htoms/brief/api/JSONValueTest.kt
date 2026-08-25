package com.htoms.brief.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Swift JSONValue와 동일한 유연한 문자열·숫자 변환 규칙 검증. */
class JSONValueTest {

    @Test
    fun stringValueCoercesIntegralNumbersWithoutDecimalPoint() {
        val root = JSONValue.parse("""{"date":"2026-08","code":30,"rate":1.5}""").objectValue!!
        assertEquals("2026-08", root["date"]?.stringValue)
        assertEquals("30", root["code"]?.stringValue)
        assertEquals("1.5", root["rate"]?.stringValue)
    }

    @Test
    fun doubleValueParsesNumbersAndCommaGroupedStrings() {
        val root = JSONValue.parse("""{"a":100000,"b":"1,067","c":"oops","d":null}""").objectValue!!
        assertEquals(100000.0, root["a"]?.doubleValue)
        assertEquals(1067.0, root["b"]?.doubleValue)
        assertNull(root["c"]?.doubleValue)
        assertNull(root["d"]?.doubleValue)
    }

    @Test
    fun nestedStructuresRoundTrip() {
        val root = JSONValue.parse("""{"data":[{"x":1},{"x":2}],"flag":true}""")
        val rows = root.objectValue?.get("data")?.arrayValue!!
        assertEquals(2, rows.size)
        assertEquals(1.0, rows[0].objectValue?.get("x")?.doubleValue)
        assertEquals(JSONValue.Bool(true), root.objectValue?.get("flag"))
    }
}
