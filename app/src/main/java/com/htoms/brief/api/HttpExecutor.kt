package com.htoms.brief.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** 네트워크 실행 추상화. 앱은 HttpURLConnection 구현을 쓰고, 테스트는 대역을 주입한다. */
data class HttpRequest(
    val url: String,
    val method: String,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray? = null
)

data class HttpResponse(
    val statusCode: Int,
    val body: ByteArray
)

fun interface HttpExecutor {
    suspend fun execute(request: HttpRequest): HttpResponse
}

/** 추가 의존성 없는 공식 플랫폼 API(HttpURLConnection) 기반 기본 구현. */
class UrlConnectionHttpExecutor(
    private val connectTimeoutMillis: Int = 15_000,
    private val readTimeoutMillis: Int = 30_000
) : HttpExecutor {
    override suspend fun execute(request: HttpRequest): HttpResponse = withContext(Dispatchers.IO) {
        val connection = URL(request.url).openConnection() as? HttpURLConnection
            ?: throw OMSAPIError.InvalidResponse
        try {
            connection.requestMethod = request.method
            connection.connectTimeout = connectTimeoutMillis
            connection.readTimeout = readTimeoutMillis
            request.headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            request.body?.let { body ->
                connection.doOutput = true
                connection.outputStream.use { it.write(body) }
            }
            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { it.readBytes() } ?: ByteArray(0)
            HttpResponse(statusCode = statusCode, body = body)
        } finally {
            connection.disconnect()
        }
    }
}
