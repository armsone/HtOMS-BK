package com.htoms.brief.api

import com.htoms.brief.auth.AuthError
import com.htoms.brief.auth.AuthServicing
import com.htoms.brief.auth.AuthSession
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder

/**
 * OMS Cafe24 백엔드 읽기 전용 REST 클라이언트.
 * 인증 수립에 필요한 유일한 POST 요청은 signIn이며, 업무 데이터 요청은 get만 허용한다.
 */
class OMSAPIClient(
    private val baseUrl: String = PRODUCTION_BASE_URL,
    private val executor: HttpExecutor = UrlConnectionHttpExecutor(),
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) {

    suspend fun signIn(email: String, password: String): AuthSession {
        if (email.isEmpty() || password.isEmpty()) throw AuthError.EmptyCredentials
        val body = json.encodeToString(SignInRequest.serializer(), SignInRequest(email, password))
        val response = execute(
            HttpRequest(
                url = "$baseUrl/api/auth/signin",
                method = "POST",
                headers = mapOf("Content-Type" to "application/json"),
                body = body.encodeToByteArray()
            )
        )
        val root = response.objectValue ?: throw OMSAPIError.MalformedData
        val token = root["access_token"]?.stringValue
        if (token.isNullOrEmpty()) throw OMSAPIError.MalformedData
        val user = root["user"]?.objectValue
        val displayName = user?.get("username")?.stringValue
            ?: user?.get("email")?.stringValue
            ?: email
        return AuthSession(token = token, username = displayName, issuedAt = nowMillis())
    }

    /** 읽기 전용 엔드포인트만 표현할 수 있어 PUT/PATCH/DELETE 및 업무 POST가 생성되지 않는다. */
    suspend fun get(endpoint: ReadEndpoint, token: String): JSONValue {
        val query = endpoint.queryItems
            ?.joinToString("&") { (name, value) ->
                "${URLEncoder.encode(name, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
            }
            ?.let { "?$it" }
            ?: ""
        return execute(
            HttpRequest(
                url = "$baseUrl${endpoint.path}$query",
                method = "GET",
                headers = mapOf(
                    "Accept" to "application/json",
                    "Authorization" to "Bearer $token"
                )
            )
        )
    }

    private suspend fun execute(request: HttpRequest): JSONValue {
        val response = executor.execute(request)
        when (response.statusCode) {
            in 200..299 -> Unit
            401, 403 -> throw OMSAPIError.Unauthorized
            else -> throw OMSAPIError.Server(response.statusCode)
        }
        return try {
            JSONValue.parse(response.body.decodeToString())
        } catch (_: Exception) {
            throw OMSAPIError.MalformedData
        }
    }

    @Serializable
    private data class SignInRequest(val email: String, val password: String)

    sealed class ReadEndpoint {
        data class DailySales(val month: String) : ReadEndpoint()
        data object MonthChart : ReadEndpoint()
        data object ReferenceHourly : ReadEndpoint()
        data object TodayHourly : ReadEndpoint()
        data class SalesTargets(val year: String) : ReadEndpoint()
        data object MonthlySales : ReadEndpoint()
        data object SalesLevels : ReadEndpoint()
        data class ProductSales(val start: String, val end: String) : ReadEndpoint()
        data object BotPings : ReadEndpoint()

        val path: String
            get() = when (this) {
                is DailySales -> "/api/data-view/daily-sales-amount/$month"
                MonthChart -> "/api/data-view/sales-chart-data/30"
                ReferenceHourly -> "/api/data-view/sales-chart-data/30-per-hour"
                TodayHourly -> "/api/data-view/sales-chart-data/day-per-hour"
                is SalesTargets -> "/api/data-view/sales-target/$year"
                MonthlySales -> "/api/dynamic-crud/v_monthly_sales_amount/all"
                SalesLevels -> "/api/dynamic-crud/calendar_sales_level/all"
                is ProductSales -> "/api/dynamic-crud/v_sales_status/range"
                BotPings -> "/api/dynamic-crud/hantong_bot_ping/all"
            }

        val queryItems: List<Pair<String, String>>?
            get() = (this as? ProductSales)?.let {
                listOf(
                    "lteKey" to "delivery_date",
                    "lteValue" to it.end,
                    "gteKey" to "delivery_date",
                    "gteValue" to it.start
                )
            }
    }

    companion object {
        const val PRODUCTION_BASE_URL = "https://htoms.cafe24.com"
        private val json = Json
    }
}

class OMSAuthService(
    private val client: OMSAPIClient = OMSAPIClient()
) : AuthServicing {
    override suspend fun authenticate(username: String, password: String): AuthSession =
        client.signIn(email = username, password = password)
}
