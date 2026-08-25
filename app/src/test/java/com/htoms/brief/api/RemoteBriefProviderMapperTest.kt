package com.htoms.brief.api

import com.htoms.brief.model.DailyPoint
import com.htoms.brief.model.ServerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** iOS OMSAPITests.testMapperBuildsTodayMonthChartsAndWidgetFromServerJSON 대응 테스트. */
class RemoteBriefProviderMapperTest {

    private val seoul = ZoneId.of("Asia/Seoul")
    private val current = LocalDate.of(2026, 8, 20).atStartOfDay(seoul).toInstant().toEpochMilli()

    private fun decode(json: String): JSONValue = JSONValue.parse(json)

    @Test
    fun mapperBuildsTodayMonthChartsAndWidgetFromServerJson() {
        val snapshot = RemoteBriefProvider.makeSnapshot(
            currentMillis = current,
            month = "2026-08",
            daily = decode(
                """[{"date":"2026-08-19","is_store":100000,"is_onsite":0,"is_normal":200000},
                    {"date":"2026-08-20","is_store":46000,"is_onsite":10000,"is_normal":50000}]"""
            ),
            chart = decode(
                """{"chartData":[{"date":"Jul21","sales":300000},{"date":"Aug20","sales":100000}],"average":200000}"""
            ),
            referenceHourly = decode("""[{"hour":"10h","sales":100000},{"hour":"09h","sales":100000}]"""),
            todayHourly = decode("""[{"hour":"9h","sales":20000}]"""),
            targets = decode("""[{"date":"2026-08","sales_target":1000000}]"""),
            monthlySales = decode(
                """[{"year_month":"2026-07","monthly_sales_amount":700000},
                    {"year_month":"2026-08","monthly_sales_amount":526000}]"""
            ),
            levels = decode("""[{"level2_min":100000,"level3_min":200000,"level4_min":300000,"level5_min":400000}]"""),
            products = decode("""[{"product_name":"한통식판","price_sum":300000},{"product_name":"실링비닐","price_sum":100000}]"""),
            botPings = decode(
                """[{"name":"HBot-0 : 장항","ping_date":"2026-08-20T00:05:00Z"},
                    {"name":"HBot-2 : 인천","ping_date":"2026-08-19T23:49:00Z"},
                    {"name":"HBot-1 : 삼송","ping_date":"2026-08-20T00:10:00Z"},
                    {"name":"HBot-3 : 초월","ping_date":"2026-08-20T00:09:59.000Z"}]"""
            )
        )

        assertEquals(11, snapshot.overview.todaySales.amount)
        assertEquals("낮음", snapshot.overview.todaySales.level)
        assertEquals(53, snapshot.overview.monthTotal)
        assertEquals(3, snapshot.overview.monthAverage)
        assertEquals(53, snapshot.overview.monthProgress)
        assertEquals(listOf("09h", "10h"), snapshot.dayTrend.map { it.label })
        assertEquals(DailyPoint(label = "09h", reference = 10, result = 20), snapshot.dayTrend.first())
        assertEquals(DailyPoint(label = "10h", reference = 10, result = null), snapshot.dayTrend.last())
        assertEquals(2, snapshot.monthTrend.size)

        val firstDate = Instant.ofEpochMilli(snapshot.monthTrend[0].date).atZone(seoul).toLocalDate()
        assertEquals(7, firstDate.monthValue)
        assertEquals(21, firstDate.dayOfMonth)
        assertEquals(2026, firstDate.year)
        val secondDate = Instant.ofEpochMilli(snapshot.monthTrend[1].date).atZone(seoul).toLocalDate()
        assertEquals(8, secondDate.monthValue)
        assertEquals(20, secondDate.dayOfMonth)
        assertEquals(30, snapshot.monthTrend[0].count)
        assertEquals(10, snapshot.monthTrend[1].count)

        val widget = snapshot.widgetSnapshot
        assertNotNull(widget)
        assertEquals(106_000L, widget!!.todayAmount)
        assertEquals(300_000L, widget.yesterdayAmount)
        assertEquals(526_000L, widget.monthTotal)
        assertEquals(700_000L, widget.previousMonthTotal)
        assertEquals(1_000_000L, widget.targetAmount)
        assertFalse(widget.isSample)

        assertEquals(
            listOf(
                ServerStatus("장항", isOperational = true),
                ServerStatus("인천", isOperational = false),
                ServerStatus("삼송", isOperational = true),
                ServerStatus("초월", isOperational = true)
            ),
            snapshot.overview.serverStatuses
        )
    }

    @Test
    fun channelSharesUseStoreOnsiteNormalWithRoundedPercentages() {
        val snapshot = RemoteBriefProvider.makeSnapshot(
            currentMillis = current,
            month = "2026-08",
            daily = decode("""[{"date":"2026-08-20","is_store":46000,"is_onsite":10000,"is_normal":50000}]"""),
            chart = decode("""{"chartData":[],"average":0}"""),
            referenceHourly = decode("[]"),
            todayHourly = decode("[]"),
            targets = decode("[]"),
            monthlySales = decode("[]"),
            levels = decode("[]"),
            products = decode("[]")
        )
        val channels = snapshot.overview.channels
        assertEquals(listOf("스토어", "방판", "전화"), channels.map { it.name })
        assertEquals(listOf(5, 1, 5), channels.map { it.count })
        assertEquals(listOf(43, 9, 47), channels.map { it.percentage })
        // 임계값 데이터가 없으면 등급은 "확인중"으로 둔다.
        assertEquals("확인중", snapshot.overview.todaySales.level)
    }

    @Test
    fun productMixKeepsTopFiveAndAggregatesRemainderIntoEtc() {
        val snapshot = RemoteBriefProvider.makeSnapshot(
            currentMillis = current,
            month = "2026-08",
            daily = decode("[]"),
            chart = decode("""{"chartData":[],"average":0}"""),
            referenceHourly = decode("[]"),
            todayHourly = decode("[]"),
            targets = decode("[]"),
            monthlySales = decode("[]"),
            levels = decode("[]"),
            products = decode(
                """[{"product_name":"A","price_sum":700},{"product_name":"B","price_sum":600},
                    {"product_name":"C","price_sum":500},{"product_name":"D","price_sum":400},
                    {"product_name":"E","price_sum":300},{"product_name":"F","price_sum":200},
                    {"product_name":"G","price_sum":100},{"product_name":"A","price_sum":50}]"""
            )
        )
        val mix = snapshot.overview.categoryMix
        assertEquals(listOf("A", "B", "C", "D", "E", "기타"), mix.map { it.name })
        assertEquals(750.0, mix[0].value, 0.0)
        assertEquals(300.0, mix.last().value, 0.0)
    }

    @Test
    fun pingDateParsingSupportsAllDocumentedFormats() {
        val expected = LocalDate.of(2026, 8, 20).atStartOfDay(seoul)
            .plusHours(1).plusMinutes(2).plusSeconds(3).toInstant().toEpochMilli()
        assertEquals(expected, RemoteBriefProvider.parsePingDate("2026-08-20 01:02:03"))
        assertEquals(expected, RemoteBriefProvider.parsePingDate("2026-08-20 01:02:03.000"))
        assertEquals(expected, RemoteBriefProvider.parsePingDate("2026-08-20 01:02:03.000000"))
        assertEquals(expected, RemoteBriefProvider.parsePingDate("2026-08-20T01:02:03Z"))
        assertNull(RemoteBriefProvider.parsePingDate("not-a-date"))
    }

    @Test
    fun livenessThresholdIsExactlyTenMinutes() {
        fun statuses(pingDate: String) = RemoteBriefProvider.makeSnapshot(
            currentMillis = current,
            month = "2026-08",
            daily = decode("[]"),
            chart = decode("""{"chartData":[],"average":0}"""),
            referenceHourly = decode("[]"),
            todayHourly = decode("[]"),
            targets = decode("[]"),
            monthlySales = decode("[]"),
            levels = decode("[]"),
            products = decode("[]"),
            botPings = decode("""[{"name":"HBot-0 : 장항","ping_date":"$pingDate"}]""")
        ).overview.serverStatuses

        // 정확히 10분 전 → 정상, 10분 1초 전 → 문제
        assertEquals(true, statuses("2026-08-19 23:50:00").first { it.name == "장항" }.isOperational)
        assertEquals(false, statuses("2026-08-19 23:49:59").first { it.name == "장항" }.isOperational)
        // 핑 데이터가 없는 서버는 문제로 표시
        assertEquals(false, statuses("2026-08-19 23:50:00").first { it.name == "인천" }.isOperational)
    }
}
