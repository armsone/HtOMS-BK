package com.htoms.brief.widget

import com.htoms.brief.model.DashboardWidgetSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

/** 위젯 만원 반올림·달성률·타임스탬프 규칙(iOS matchup widget_small_all_data 근거값) 검증. */
class WidgetFormatTest {

    @Test
    fun compactWonRoundsToNearestManwonWithGrouping() {
        assertEquals("1,067", WidgetFormat.compactWon(10_667_000L))
        assertEquals("20,353", WidgetFormat.compactWon(203_527_398L))
        assertEquals("32,060", WidgetFormat.compactWon(320_595_397L))
        assertEquals("1,079", WidgetFormat.compactWon(10_791_012L))
        assertEquals("30,748", WidgetFormat.compactWon(307_482_479L))
        assertEquals("0", WidgetFormat.compactWon(0L))
        // 정수 나눗셈이 아닌 반올림이어야 한다(5000원 → 1만원).
        assertEquals("1", WidgetFormat.compactWon(5_000L))
    }

    @Test
    fun achievementRateIsRoundedPercentageOfTarget() {
        assertEquals(66, WidgetFormat.achievementRate(DashboardWidgetSnapshot.sample))
        assertEquals(
            0,
            WidgetFormat.achievementRate(DashboardWidgetSnapshot.sample.copy(targetAmount = 0L))
        )
    }

    @Test
    fun sampleTimestampsRenderInSeoulTime() {
        assertEquals("08-20 16:48", WidgetFormat.compactTimestamp(DashboardWidgetSnapshot.sample.refreshedAt))
        assertEquals("08-20 16:40", WidgetFormat.compactTimestamp(DashboardWidgetSnapshot.sample.serverTime))
        assertEquals(20, WidgetFormat.currentDay(DashboardWidgetSnapshot.sample))
        assertEquals("08", WidgetFormat.currentMonth(DashboardWidgetSnapshot.sample))
    }
}
