package com.htoms.brief.api

import com.htoms.brief.model.DeliveryStatus
import com.htoms.brief.model.DeliveryStatusCount
import com.htoms.brief.model.DeliverySummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 기존 현황판의 공개 ViewData 컬렉션에서 배송 상태만 읽어 합계로 변환한다.
 * 개별 주문번호·송장번호는 모델에 담거나 저장하지 않는다.
 */
class DeliveryAggregateClient(
    private val url: String = PRODUCTION_URL,
    private val executor: HttpExecutor = UrlConnectionHttpExecutor()
) {

    suspend fun loadSummary(): DeliverySummary {
        val response = executor.execute(
            HttpRequest(
                url = url,
                method = "GET",
                headers = mapOf("Accept" to "application/json")
            )
        )
        if (response.statusCode !in 200..299) throw OMSAPIError.Server(response.statusCode)
        val root = try {
            JSONValue.parse(response.body.decodeToString())
        } catch (_: Exception) {
            throw OMSAPIError.MalformedData
        }
        return makeSummary(root)
    }

    companion object {
        const val PRODUCTION_URL =
            "https://firestore.googleapis.com/v1/projects/hantondeliverytrack/databases/(default)/documents/ViewData?pageSize=100"

        private val documentDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        fun makeSummary(root: JSONValue): DeliverySummary {
            val counts = DeliveryStatus.entries.associateWith { 0 }.toMutableMap()
            val documentDates = mutableListOf<LocalDate>()

            val documents = root.objectValue?.get("documents")?.arrayValue ?: emptyList()
            for (document in documents) {
                val body = document.objectValue ?: continue
                body["name"]?.stringValue
                    ?.substringAfterLast('/')
                    ?.let { runCatching { LocalDate.parse(it, documentDateFormatter) }.getOrNull() }
                    ?.let { documentDates.add(it) }

                val fields = body["fields"]?.objectValue ?: continue
                val keys = fields["keys"]?.objectValue?.get("arrayValue")?.objectValue
                    ?.get("values")?.arrayValue ?: emptyList()
                for (keyValue in keys) {
                    val key = keyValue.objectValue?.get("stringValue")?.stringValue ?: continue
                    val statusFields = fields[key]?.objectValue?.get("mapValue")?.objectValue
                        ?.get("fields")?.objectValue ?: continue
                    val statusId = statusFields["statusId"]?.objectValue?.get("stringValue")?.stringValue ?: ""
                    val statusText = statusFields["statusText"]?.objectValue?.get("stringValue")?.stringValue ?: ""
                    deliveryStatus(statusId, statusText)?.let { status ->
                        counts[status] = (counts[status] ?: 0) + 1
                    }
                }
            }

            val start = documentDates.minOrNull()
            val end = documentDates.maxOrNull()
            val dateRange = if (start != null && end != null) {
                val days = ChronoUnit.DAYS.between(start, end) + 1
                "${documentDateFormatter.format(start)}~${documentDateFormatter.format(end)}(${days}일)"
            } else {
                "택배 집계 조회"
            }
            return DeliverySummary(
                dateRange = dateRange,
                statuses = DeliveryStatus.entries.map { DeliveryStatusCount(it, counts[it] ?: 0) }
            )
        }

        private fun deliveryStatus(id: String, text: String): DeliveryStatus? = when (id) {
            "information_received" -> DeliveryStatus.PREPARING
            "at_pickup" -> DeliveryStatus.ACCEPTED
            "in_transit" -> DeliveryStatus.MOVING
            "out_for_delivery" -> DeliveryStatus.DEPARTING
            "delivered" -> if (text.contains("배송불가")) DeliveryStatus.UNAVAILABLE else DeliveryStatus.COMPLETED
            "track_error" -> when {
                text.contains("미배달") -> DeliveryStatus.UNDELIVERED
                text.contains("배송불가") -> DeliveryStatus.UNAVAILABLE
                else -> DeliveryStatus.INVOICE_ERROR
            }
            else -> if (listOf("용달신용", "용달착불", "직접수령", "쿠팡").any { text.contains(it) }) {
                DeliveryStatus.COMPLETED
            } else {
                null
            }
        }
    }
}
