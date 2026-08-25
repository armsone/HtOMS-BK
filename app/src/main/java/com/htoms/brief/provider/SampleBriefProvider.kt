package com.htoms.brief.provider

import com.htoms.brief.model.BriefOverview
import com.htoms.brief.model.BriefSnapshot
import com.htoms.brief.model.ChannelShare
import com.htoms.brief.model.DailyPoint
import com.htoms.brief.model.DeliveryStatus
import com.htoms.brief.model.DeliveryStatusCount
import com.htoms.brief.model.DeliverySummary
import com.htoms.brief.model.MonthlyPoint
import com.htoms.brief.model.ServerStatus
import com.htoms.brief.model.SlicePortion
import com.htoms.brief.model.TodaySales
import java.time.LocalDate
import java.time.ZoneId

/**
 * 네트워크를 전혀 사용하지 않는 결정적 화면 샘플.
 * 2026-08-20(Asia/Seoul) 고정 fixture이며 실데이터를 읽거나 바꾸지 않는다.
 */
class SampleBriefProvider : BriefProviding {
    override suspend fun loadSnapshot(): BriefSnapshot = snapshot

    companion object {
        private val seoul = ZoneId.of("Asia/Seoul")

        val snapshot: BriefSnapshot by lazy { makeSnapshot() }

        private fun makeSnapshot(): BriefSnapshot {
            val start = LocalDate.of(2026, 7, 21)
            val monthValues = listOf(
                1710, 1450, 1080, 1660, 0, 0, 1960, 1310, 1430, 820,
                1360, 0, 0, 1450, 1560, 1730, 1490, 1670, 0, 0,
                2200, 2220, 2100, 920, 650, 0, 0, 0, 1220, 1820, 1067
            )

            val overview = BriefOverview(
                todaySales = TodaySales(day = 20, level = "보통", amount = 1_067),
                channels = listOf(
                    ChannelShare(name = "스토어", count = 4_065, percentage = 20),
                    ChannelShare(name = "방판", count = 216, percentage = 1),
                    ChannelShare(name = "전화", count = 16_072, percentage = 79)
                ),
                monthLabel = "8월",
                monthProgress = 66,
                monthTotal = 20_353,
                monthAverage = 1_018,
                levelMix = listOf(
                    SlicePortion(name = "최고", value = 23.0),
                    SlicePortion(name = "높음", value = 38.0),
                    SlicePortion(name = "보통", value = 23.0),
                    SlicePortion(name = "낮음", value = 8.0),
                    SlicePortion(name = "위험", value = 8.0)
                ),
                categoryMix = listOf(
                    SlicePortion(name = "일회용식판", value = 30.7),
                    SlicePortion(name = "한통식판본체", value = 14.5),
                    SlicePortion(name = "한통식판뚜껑", value = 12.6),
                    SlicePortion(name = "실링비닐D", value = 7.9),
                    SlicePortion(name = "6찬식판본체", value = 7.8),
                    SlicePortion(name = "기타", value = 26.5)
                ),
                refreshedAt = "09:48",
                serverStatuses = listOf("장항", "인천", "삼송", "초월").map {
                    ServerStatus(name = it, isOperational = true)
                }
            )

            val dayTrend = listOf(
                DailyPoint(label = "08h", reference = 5_400, result = 800),
                DailyPoint(label = "09h", reference = 5_000, result = 2_300),
                DailyPoint(label = "10h", reference = 3_200, result = 200),
                DailyPoint(label = "11h", reference = 4_000, result = 2_600),
                DailyPoint(label = "12h", reference = 200, result = 100),
                DailyPoint(label = "13h", reference = 5_800, result = 2_700),
                DailyPoint(label = "14h", reference = 5_000, result = 800),
                DailyPoint(label = "15h", reference = 4_400, result = 600),
                DailyPoint(label = "16h", reference = 400, result = 150),
                DailyPoint(label = "17h", reference = 0, result = null)
            )

            val monthTrend = monthValues.mapIndexed { index, value ->
                MonthlyPoint(
                    date = start.plusDays(index.toLong()).atStartOfDay(seoul).toInstant().toEpochMilli(),
                    count = value
                )
            }

            return BriefSnapshot(
                overview = overview,
                dayTrend = dayTrend,
                monthTrend = monthTrend,
                monthAverage = 1_079,
                deliverySummary = DeliverySummary(
                    dateRange = "2026-08-06~2026-08-19(14일)",
                    statuses = listOf(
                        DeliveryStatusCount(DeliveryStatus.PREPARING, 0),
                        DeliveryStatusCount(DeliveryStatus.ACCEPTED, 0),
                        DeliveryStatusCount(DeliveryStatus.MOVING, 17),
                        DeliveryStatusCount(DeliveryStatus.DEPARTING, 41),
                        DeliveryStatusCount(DeliveryStatus.COMPLETED, 1_046),
                        DeliveryStatusCount(DeliveryStatus.INVOICE_ERROR, 0),
                        DeliveryStatusCount(DeliveryStatus.UNDELIVERED, 0),
                        DeliveryStatusCount(DeliveryStatus.UNAVAILABLE, 0)
                    )
                )
            )
        }
    }
}
