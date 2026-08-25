package com.htoms.brief.api

import com.htoms.brief.auth.AuthError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** iOS OMSAPITests의 요청 형태 검증(POST는 인증 전용, 업무 요청은 GET+Bearer) 대응. */
class OMSAPIClientTest {

    private class RecordingExecutor(
        private val handler: (HttpRequest) -> HttpResponse
    ) : HttpExecutor {
        val requests = mutableListOf<HttpRequest>()
        override suspend fun execute(request: HttpRequest): HttpResponse {
            requests.add(request)
            return handler(request)
        }
    }

    private fun ok(json: String) = HttpResponse(200, json.encodeToByteArray())

    @Test
    fun signInUsesOnlyFixedAuthPostAndDecodesSession() = runTest {
        val executor = RecordingExecutor { request ->
            assertEquals("POST", request.method)
            assertTrue(request.url.endsWith("/api/auth/signin"))
            assertNull(request.headers["Authorization"])
            assertEquals("application/json", request.headers["Content-Type"])
            assertTrue(request.body != null && request.body!!.isNotEmpty())
            ok("""{"access_token":"jwt-token","user":{"username":"대표"}}""")
        }
        val client = OMSAPIClient(baseUrl = "https://example.invalid", executor = executor)

        val session = client.signIn(email = "ceo@example.com", password = "secret")
        assertEquals("jwt-token", session.token)
        assertEquals("대표", session.username)
    }

    @Test
    fun signInRejectsEmptyCredentialsWithoutNetworkCall() = runTest {
        val executor = RecordingExecutor { fail("네트워크 호출이 없어야 합니다"); ok("{}") }
        val client = OMSAPIClient(baseUrl = "https://example.invalid", executor = executor)
        try {
            client.signIn(email = "", password = "")
            fail("에러가 발생해야 합니다")
        } catch (error: AuthError.EmptyCredentials) {
            assertEquals("아이디와 비밀번호를 모두 입력해 주세요.", error.message)
        }
        assertEquals(0, executor.requests.size)
    }

    @Test
    fun signInWithoutAccessTokenIsMalformedData() = runTest {
        val executor = RecordingExecutor { ok("""{"user":{"username":"대표"}}""") }
        val client = OMSAPIClient(baseUrl = "https://example.invalid", executor = executor)
        try {
            client.signIn(email = "a@b.c", password = "pw")
            fail("에러가 발생해야 합니다")
        } catch (error: OMSAPIError.MalformedData) {
            assertEquals("OMS 서버 데이터 형식이 예상과 다릅니다.", error.message)
        }
    }

    @Test
    fun businessEndpointAlwaysUsesGetAndBearerToken() = runTest {
        val executor = RecordingExecutor { request ->
            assertEquals("GET", request.method)
            assertEquals("Bearer read-token", request.headers["Authorization"])
            assertEquals("application/json", request.headers["Accept"])
            assertTrue(request.url.endsWith("/api/data-view/sales-chart-data/30"))
            ok("""{"chartData":[],"average":0}""")
        }
        val client = OMSAPIClient(baseUrl = "https://example.invalid", executor = executor)
        client.get(OMSAPIClient.ReadEndpoint.MonthChart, token = "read-token")
        assertEquals(1, executor.requests.size)
    }

    @Test
    fun productSalesEncodesDeliveryDateRangeQuery() = runTest {
        val executor = RecordingExecutor { request ->
            assertTrue(request.url.contains("/api/dynamic-crud/v_sales_status/range?"))
            assertTrue(request.url.contains("gteKey=delivery_date"))
            assertTrue(request.url.contains("gteValue=2026-08-01"))
            assertTrue(request.url.contains("lteKey=delivery_date"))
            assertTrue(request.url.contains("lteValue=2026-08-31"))
            ok("[]")
        }
        val client = OMSAPIClient(baseUrl = "https://example.invalid", executor = executor)
        client.get(
            OMSAPIClient.ReadEndpoint.ProductSales(start = "2026-08-01", end = "2026-08-31"),
            token = "read-token"
        )
    }

    @Test
    fun readEndpointWhitelistContainsNoMutationRoute() {
        val endpoints = listOf(
            OMSAPIClient.ReadEndpoint.DailySales("2026-08"),
            OMSAPIClient.ReadEndpoint.MonthChart,
            OMSAPIClient.ReadEndpoint.ReferenceHourly,
            OMSAPIClient.ReadEndpoint.TodayHourly,
            OMSAPIClient.ReadEndpoint.SalesTargets("2026"),
            OMSAPIClient.ReadEndpoint.MonthlySales,
            OMSAPIClient.ReadEndpoint.SalesLevels,
            OMSAPIClient.ReadEndpoint.ProductSales("2026-08-01", "2026-08-31"),
            OMSAPIClient.ReadEndpoint.BotPings
        )
        endpoints.forEach { endpoint ->
            assertTrue(!endpoint.path.contains("delete"))
            assertTrue(!endpoint.path.contains("update"))
            assertTrue(!endpoint.path.contains("create"))
        }
    }

    @Test
    fun unauthorizedStatusCodesMapToUnauthorizedError() = runTest {
        listOf(401, 403).forEach { status ->
            val executor = RecordingExecutor { HttpResponse(status, ByteArray(0)) }
            val client = OMSAPIClient(baseUrl = "https://example.invalid", executor = executor)
            try {
                client.get(OMSAPIClient.ReadEndpoint.MonthChart, token = "expired")
                fail("에러가 발생해야 합니다")
            } catch (error: OMSAPIError.Unauthorized) {
                assertEquals("로그인이 만료되었거나 계정 정보가 올바르지 않습니다.", error.message)
            }
        }
    }

    @Test
    fun serverErrorCarriesStatusCodeInMessage() = runTest {
        val executor = RecordingExecutor { HttpResponse(503, ByteArray(0)) }
        val client = OMSAPIClient(baseUrl = "https://example.invalid", executor = executor)
        try {
            client.get(OMSAPIClient.ReadEndpoint.MonthChart, token = "token")
            fail("에러가 발생해야 합니다")
        } catch (error: OMSAPIError.Server) {
            assertEquals(503, error.statusCode)
            assertEquals("OMS 서버 요청에 실패했습니다. (503)", error.message)
        }
    }
}
