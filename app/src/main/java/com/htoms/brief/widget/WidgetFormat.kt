package com.htoms.brief.widget

import com.htoms.brief.model.DashboardWidgetSnapshot
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/** 위젯 표시 값 계산. Glance 클래스와 분리해 JVM 단위 테스트가 가능하다. */
object WidgetFormat {
    private val SEOUL = ZoneId.of("Asia/Seoul")

    /** 원 단위 값을 만원 단위 반올림으로 표기한다(iOS 위젯과 동일 규칙). */
    fun compactWon(value: Long): String {
        val manwon = Math.round(value.toDouble() / 10_000.0)
        return String.format(Locale.US, "%,d", manwon)
    }

    fun compactTimestamp(epochMillis: Long): String {
        val time = seoulTime(epochMillis)
        return String.format(
            Locale.US, "%02d-%02d %02d:%02d",
            time.monthValue, time.dayOfMonth, time.hour, time.minute
        )
    }

    fun currentDay(snapshot: DashboardWidgetSnapshot): Int =
        seoulTime(snapshot.refreshedAt).dayOfMonth

    fun currentMonth(snapshot: DashboardWidgetSnapshot): String =
        String.format(Locale.US, "%02d", seoulTime(snapshot.refreshedAt).monthValue)

    fun achievementRate(snapshot: DashboardWidgetSnapshot): Int {
        if (snapshot.targetAmount <= 0) return 0
        return Math.round(snapshot.monthTotal.toDouble() / snapshot.targetAmount * 100).toInt()
    }

    private fun seoulTime(epochMillis: Long): ZonedDateTime =
        Instant.ofEpochMilli(epochMillis).atZone(SEOUL)
}
