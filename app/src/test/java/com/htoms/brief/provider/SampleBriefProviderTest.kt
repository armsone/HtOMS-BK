package com.htoms.brief.provider

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** iOS SampleBriefProviderTests 대응: 결정적 fixture 검증. */
class SampleBriefProviderTest {

    private val seoul = ZoneId.of("Asia/Seoul")

    @Test
    fun sampleSnapshotIsDeterministicAndMatchesContractFixtures() = runTest {
        val snapshot = SampleBriefProvider().loadSnapshot()
        assertEquals(snapshot, SampleBriefProvider().loadSnapshot())

        val overview = snapshot.overview
        assertEquals(20, overview.todaySales.day)
        assertEquals("보통", overview.todaySales.level)
        assertEquals(1_067, overview.todaySales.amount)
        assertEquals(listOf("스토어", "방판", "전화"), overview.channels.map { it.name })
        assertEquals(listOf(4_065, 216, 16_072), overview.channels.map { it.count })
        assertEquals(listOf(20, 1, 79), overview.channels.map { it.percentage })
        assertEquals("8월", overview.monthLabel)
        assertEquals(66, overview.monthProgress)
        assertEquals(20_353, overview.monthTotal)
        assertEquals(1_018, overview.monthAverage)
        assertEquals("09:48", overview.refreshedAt)
        assertEquals(listOf("장항", "인천", "삼송", "초월"), overview.serverStatuses.map { it.name })
        assertEquals(true, overview.serverStatuses.all { it.isOperational })

        assertEquals(10, snapshot.dayTrend.size)
        assertEquals("08h", snapshot.dayTrend.first().label)
        assertEquals(31, snapshot.monthTrend.size)
        assertEquals(1_079, snapshot.monthAverage)

        val firstDate = Instant.ofEpochMilli(snapshot.monthTrend.first().date).atZone(seoul).toLocalDate()
        assertEquals(LocalDate.of(2026, 7, 21), firstDate)
        val lastDate = Instant.ofEpochMilli(snapshot.monthTrend.last().date).atZone(seoul).toLocalDate()
        assertEquals(LocalDate.of(2026, 8, 20), lastDate)

        assertEquals("2026-08-06~2026-08-19(14일)", snapshot.deliverySummary.dateRange)
        assertEquals(1_104, snapshot.deliverySummary.total)

        // 샘플 모드는 위젯 스냅샷을 만들지 않아 실데이터 캐시를 덮어쓰지 않는다.
        assertNull(snapshot.widgetSnapshot)
    }
}
