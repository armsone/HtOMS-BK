package com.htoms.brief.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * 병렬 다중 스트림 로드의 부분 실패·우아한 성능 저하·인증 만료 전파 검증.
 * 실제 네트워크 없이 HttpExecutor 대역으로 엔드포인트별 응답을 흉내 낸다.
 */
class RemoteBriefProviderResilienceTest {

    private val seoul = ZoneId.of("Asia/Seoul")
    private val currentMillis = LocalDate.of(2026, 8, 20).atStartOfDay(seoul).toInstant().toEpochMilli()

    private fun provider(handler: (HttpRequest) -> HttpResponse): RemoteBriefProvider {
        val executor = HttpExecutor { request -> handler(request) }
        return RemoteBriefProvider(
            token = "read-token",
            client = OMSAPIClient(baseUrl = "https://example.invalid", executor = executor),
            deliveryClient = DeliveryAggregateClient(url = "https://firestore.example.invalid/ViewData", executor = executor),
            nowMillis = { currentMillis }
        )
    }

    private fun ok(json: String) = HttpResponse(200, json.encodeToByteArray())

    private fun essentialResponses(path: String): HttpResponse? = when {
        path.contains("/daily-sales-amount/") ->
            ok("""[{"date":"2026-08-20","is_store":46000,"is_onsite":10000,"is_normal":50000}]""")
        path.endsWith("/sales-chart-data/30") -> ok("""{"chartData":[],"average":200000}""")
        path.endsWith("/sales-chart-data/30-per-hour") -> ok("""[{"hour":"09h","sales":100000}]""")
        path.endsWith("/sales-chart-data/day-per-hour") -> ok("""[{"hour":"09h","sales":20000}]""")
        else -> null
    }

    @Test
    fun nonEssentialFailuresDegradeGracefully() = runTest {
        val snapshot = provider { request ->
            essentialResponses(request.url)
                ?: HttpResponse(500, ByteArray(0)) // 보조 엔드포인트·Firestore 전부 실패
        }.loadSnapshot()

        // 필수 스트림 결과는 유지된다.
        assertEquals(11, snapshot.overview.todaySales.amount)
        assertEquals(listOf("09h"), snapshot.dayTrend.map { it.label })
        assertEquals(20, snapshot.monthAverage)
        // 보조 스트림 실패는 빈 값으로 대체된다.
        assertEquals("확인중", snapshot.overview.todaySales.level)
        assertEquals(0, snapshot.overview.monthProgress)
        assertTrue(snapshot.overview.categoryMix.isEmpty())
        assertTrue(snapshot.overview.serverStatuses.none { it.isOperational })
        assertEquals("택배 집계 조회 대기", snapshot.deliverySummary.dateRange)
        assertEquals(0, snapshot.deliverySummary.total)
    }

    @Test
    fun essentialFailurePropagatesServerError() = runTest {
        try {
            provider { request ->
                if (request.url.endsWith("/sales-chart-data/30")) {
                    HttpResponse(500, ByteArray(0))
                } else {
                    essentialResponses(request.url) ?: ok("[]")
                }
            }.loadSnapshot()
            fail("에러가 발생해야 합니다")
        } catch (error: OMSAPIError.Server) {
            assertEquals(500, error.statusCode)
        }
    }

    @Test
    fun unauthorizedOnNonEssentialEndpointIsNeverHidden() = runTest {
        try {
            provider { request ->
                if (request.url.contains("/hantong_bot_ping/")) {
                    HttpResponse(401, ByteArray(0))
                } else {
                    essentialResponses(request.url) ?: ok("[]")
                }
            }.loadSnapshot()
            fail("에러가 발생해야 합니다")
        } catch (error: OMSAPIError.Unauthorized) {
            assertEquals("로그인이 만료되었거나 계정 정보가 올바르지 않습니다.", error.message)
        }
    }
}
