package com.htoms.brief.model

import kotlinx.serialization.Serializable

@Serializable
data class ChannelShare(
    val name: String,
    val count: Int,
    val percentage: Int
) {
    val id: String get() = name
}

@Serializable
data class SlicePortion(
    val name: String,
    val value: Double
) {
    val id: String get() = name
}

@Serializable
data class DailyPoint(
    val label: String,
    val reference: Int?,
    val result: Int?
) {
    val id: String get() = label
}

@Serializable
data class MonthlyPoint(
    val date: Long,
    val count: Int
) {
    val id: Long get() = date
}

@Serializable
data class TodaySales(
    val day: Int,
    val level: String,
    val amount: Int
)

@Serializable
data class ServerStatus(
    val name: String,
    val isOperational: Boolean
) {
    val id: String get() = name
}

@Serializable
enum class DeliveryStatus(val label: String) {
    PREPARING("준비"),
    ACCEPTED("인수"),
    MOVING("이동"),
    DEPARTING("출발"),
    COMPLETED("완료"),
    INVOICE_ERROR("송장오류"),
    UNDELIVERED("미배달"),
    UNAVAILABLE("배송불가")
}

@Serializable
data class DeliveryStatusCount(
    val status: DeliveryStatus,
    val count: Int
) {
    val id: DeliveryStatus get() = status
}

@Serializable
data class BriefOverview(
    val todaySales: TodaySales,
    val channels: List<ChannelShare>,
    val monthLabel: String,
    val monthProgress: Int,
    val monthTotal: Int,
    val monthAverage: Int,
    val levelMix: List<SlicePortion>,
    val categoryMix: List<SlicePortion>,
    val refreshedAt: String,
    val serverStatuses: List<ServerStatus>
)

@Serializable
data class DeliverySummary(
    val dateRange: String,
    val statuses: List<DeliveryStatusCount>
) {
    val total: Int get() = statuses.sumOf { it.count }
}

@Serializable
data class BriefSnapshot(
    val overview: BriefOverview,
    val dayTrend: List<DailyPoint>,
    val monthTrend: List<MonthlyPoint>,
    val monthAverage: Int,
    val deliverySummary: DeliverySummary,
    val widgetSnapshot: DashboardWidgetSnapshot? = null
)
