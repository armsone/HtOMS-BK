package com.htoms.brief.model

import kotlinx.serialization.Serializable
import java.util.Calendar
import java.util.TimeZone

/**
 * 위젯에 전달하는 표시 전용 데이터. 인증정보나 사용자 식별자는 포함하지 않는다.
 */
@Serializable
data class DashboardWidgetSnapshot(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val todayAmount: Long,
    val yesterdayAmount: Long,
    val monthTotal: Long,
    val previousMonthTotal: Long,
    val dailyAverage: Long,
    val targetAmount: Long,
    val refreshedAt: Long,
    val serverTime: Long,
    val isSample: Boolean
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        val sample: DashboardWidgetSnapshot by lazy {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply {
                set(2026, Calendar.AUGUST, 20, 16, 48, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val refreshedAt = calendar.timeInMillis

            calendar.apply {
                set(2026, Calendar.AUGUST, 20, 16, 40, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val serverTime = calendar.timeInMillis

            DashboardWidgetSnapshot(
                schemaVersion = CURRENT_SCHEMA_VERSION,
                todayAmount = 10_667_000L,
                yesterdayAmount = 18_160_000L,
                monthTotal = 203_527_398L,
                previousMonthTotal = 320_595_397L,
                dailyAverage = 10_791_012L,
                targetAmount = 307_482_479L,
                refreshedAt = refreshedAt,
                serverTime = serverTime,
                isSample = true
            )
        }
    }
}
