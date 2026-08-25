package com.htoms.brief.theme

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * 숫자 표기 도우미.
 * Swift BriefFormat과 동일한 통화/건수/축약 단위 포맷팅.
 */
object BriefFormat {
    private val decimalFormat = DecimalFormat("#,###", DecimalFormatSymbols(Locale.KOREA))

    fun number(value: Int): String = decimalFormat.format(value)
    fun number(value: Long): String = decimalFormat.format(value)

    fun won(value: Int): String = "${number(value)}원"
    fun won(value: Long): String = "${number(value)}원"

    fun count(value: Int): String = "${number(value)}건"
    fun count(value: Long): String = "${number(value)}건"

    /**
     * 차트 축 등 좁은 자리용 축약 표기.
     */
    fun compactWon(value: Int): String = compactWon(value.toLong())

    fun compactWon(value: Long): String {
        if (value >= 100_000_000L) {
            val eok = value.toDouble() / 100_000_000.0
            return if (eok % 1.0 == 0.0) {
                String.format(Locale.KOREA, "%.0f억", eok)
            } else {
                String.format(Locale.KOREA, "%.1f억", eok)
            }
        }
        if (value >= 10_000L) {
            return "${number(value / 10_000L)}만"
        }
        return number(value)
    }
}
