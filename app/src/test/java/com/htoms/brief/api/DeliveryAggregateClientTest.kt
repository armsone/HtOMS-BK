package com.htoms.brief.api

import com.htoms.brief.model.DeliveryStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** iOS OMSAPITests의 Firestore 배송 집계 대응 테스트. */
class DeliveryAggregateClientTest {

    private fun decode(json: String): JSONValue = JSONValue.parse(json)

    private fun count(summaryJson: String, status: DeliveryStatus): Int =
        DeliveryAggregateClient.makeSummary(decode(summaryJson)).statuses
            .first { it.status == status }.count

    @Test
    fun aggregateCountsOnlyStatusesAndComputesDateRange() {
        val result = DeliveryAggregateClient.makeSummary(decode(
            """
            {
              "documents": [{
                "name": "projects/test/databases/(default)/documents/ViewData/2026-08-20",
                "fields": {
                  "keys": {"arrayValue":{"values":[{"stringValue":"a"},{"stringValue":"b"},{"stringValue":"c"},{"stringValue":"d"}]}},
                  "a": {"mapValue":{"fields":{"statusId":{"stringValue":"information_received"},"statusText":{"stringValue":"준비"}}}},
                  "b": {"mapValue":{"fields":{"statusId":{"stringValue":"delivered"},"statusText":{"stringValue":"완료"}}}},
                  "c": {"mapValue":{"fields":{"statusId":{"stringValue":"track_error"},"statusText":{"stringValue":"미배달"}}}},
                  "d": {"mapValue":{"fields":{"statusId":{"stringValue":"track_error"},"statusText":{"stringValue":"배송불가"}}}}
                }
              }]
            }
            """.trimIndent()
        ))

        assertEquals("2026-08-20~2026-08-20(1일)", result.dateRange)
        assertEquals(1, result.statuses.first { it.status == DeliveryStatus.PREPARING }.count)
        assertEquals(1, result.statuses.first { it.status == DeliveryStatus.COMPLETED }.count)
        assertEquals(1, result.statuses.first { it.status == DeliveryStatus.UNDELIVERED }.count)
        assertEquals(1, result.statuses.first { it.status == DeliveryStatus.UNAVAILABLE }.count)
        assertEquals(4, result.total)
    }

    @Test
    fun deliveredWithUnavailableTextIsClassifiedAsUnavailable() {
        val json = """
            {
              "documents": [{
                "name": "projects/test/databases/(default)/documents/ViewData/2026-08-20",
                "fields": {
                  "keys": {"arrayValue":{"values":[{"stringValue":"a"}]}},
                  "a": {"mapValue":{"fields":{"statusId":{"stringValue":"delivered"},"statusText":{"stringValue":"배송불가 반송"}}}}
                }
              }]
            }
        """.trimIndent()
        assertEquals(1, count(json, DeliveryStatus.UNAVAILABLE))
        assertEquals(0, count(json, DeliveryStatus.COMPLETED))
    }

    @Test
    fun fallbackKeywordsCountAsCompletedAndUnknownIsIgnored() {
        val json = """
            {
              "documents": [{
                "name": "projects/test/databases/(default)/documents/ViewData/2026-08-19",
                "fields": {
                  "keys": {"arrayValue":{"values":[{"stringValue":"a"},{"stringValue":"b"},{"stringValue":"c"}]}},
                  "a": {"mapValue":{"fields":{"statusId":{"stringValue":""},"statusText":{"stringValue":"용달착불 처리"}}}},
                  "b": {"mapValue":{"fields":{"statusId":{"stringValue":""},"statusText":{"stringValue":"쿠팡"}}}},
                  "c": {"mapValue":{"fields":{"statusId":{"stringValue":""},"statusText":{"stringValue":"알수없음"}}}}
                }
              }]
            }
        """.trimIndent()
        assertEquals(2, count(json, DeliveryStatus.COMPLETED))
        assertEquals(2, DeliveryAggregateClient.makeSummary(decode(json)).total)
    }

    @Test
    fun multiDayDocumentsProduceInclusiveDayRange() {
        val result = DeliveryAggregateClient.makeSummary(decode(
            """
            {
              "documents": [
                {"name": "p/d/documents/ViewData/2026-08-06", "fields": {}},
                {"name": "p/d/documents/ViewData/2026-08-19", "fields": {}}
              ]
            }
            """.trimIndent()
        ))
        assertEquals("2026-08-06~2026-08-19(14일)", result.dateRange)
        assertEquals(0, result.total)
    }

    @Test
    fun emptyDocumentsFallBackToPlaceholderRange() {
        val result = DeliveryAggregateClient.makeSummary(decode("""{"documents":[]}"""))
        assertEquals("택배 집계 조회", result.dateRange)
        assertEquals(DeliveryStatus.entries.size, result.statuses.size)
    }

    @Test
    fun loadSummaryUsesOnlyGetOnFirestoreUrl() = runTest {
        var requested: HttpRequest? = null
        val client = DeliveryAggregateClient(
            url = DeliveryAggregateClient.PRODUCTION_URL,
            executor = { request ->
                requested = request
                HttpResponse(200, """{"documents":[]}""".encodeToByteArray())
            }
        )
        client.loadSummary()
        assertEquals("GET", requested?.method)
        assertTrue(requested!!.url.startsWith("https://firestore.googleapis.com/"))
        assertTrue(requested!!.url.contains("/documents/ViewData"))
    }
}
