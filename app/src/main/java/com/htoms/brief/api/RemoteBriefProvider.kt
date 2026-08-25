package com.htoms.brief.api

import com.htoms.brief.model.BriefOverview
import com.htoms.brief.model.BriefSnapshot
import com.htoms.brief.model.ChannelShare
import com.htoms.brief.model.DailyPoint
import com.htoms.brief.model.DashboardWidgetSnapshot
import com.htoms.brief.model.DeliveryStatus
import com.htoms.brief.model.DeliveryStatusCount
import com.htoms.brief.model.DeliverySummary
import com.htoms.brief.model.MonthlyPoint
import com.htoms.brief.model.ServerStatus
import com.htoms.brief.model.SlicePortion
import com.htoms.brief.model.TodaySales
import com.htoms.brief.provider.BriefProviding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.MonthDay
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale
import kotlin.math.roundToInt

/**
 * OMS REST + Firestore 집계를 병렬로 읽어 BriefSnapshot으로 매핑한다.
 * 필수 4개 스트림 실패는 오류로 전파하고, 보조 스트림 실패는 빈 값으로 대체해
 * 화면을 계속 표시한다. 인증 만료(Unauthorized)는 숨기지 않고 즉시 전파한다.
 */
class RemoteBriefProvider(
    private val token: String,
    private val client: OMSAPIClient = OMSAPIClient(),
    private val deliveryClient: DeliveryAggregateClient = DeliveryAggregateClient(),
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) : BriefProviding {

    override suspend fun loadSnapshot(): BriefSnapshot {
        val currentMillis = nowMillis()
        val current = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(currentMillis), SEOUL)
        val month = current.format(MONTH_FORMATTER)
        val year = current.format(YEAR_FORMATTER)
        val monthStart = current.toLocalDate().withDayOfMonth(1)
        val monthEnd = monthStart.plusMonths(1).minusDays(1)

        return coroutineScope {
            val daily = async { client.get(OMSAPIClient.ReadEndpoint.DailySales(month), token) }
            val chart = async { client.get(OMSAPIClient.ReadEndpoint.MonthChart, token) }
            val reference = async { client.get(OMSAPIClient.ReadEndpoint.ReferenceHourly, token) }
            val today = async { client.get(OMSAPIClient.ReadEndpoint.TodayHourly, token) }
            val targets = async { optional(OMSAPIClient.ReadEndpoint.SalesTargets(year)) }
            val monthly = async { optional(OMSAPIClient.ReadEndpoint.MonthlySales) }
            val levels = async { optional(OMSAPIClient.ReadEndpoint.SalesLevels) }
            val products = async {
                optional(
                    OMSAPIClient.ReadEndpoint.ProductSales(
                        start = monthStart.format(DAY_FORMATTER),
                        end = monthEnd.format(DAY_FORMATTER)
                    )
                )
            }
            val botPings = async { optional(OMSAPIClient.ReadEndpoint.BotPings) }
            val delivery = async { optionalDelivery() }

            makeSnapshot(
                currentMillis = currentMillis,
                month = month,
                daily = daily.await(),
                chart = chart.await(),
                referenceHourly = reference.await(),
                todayHourly = today.await(),
                targets = targets.await(),
                monthlySales = monthly.await(),
                levels = levels.await(),
                products = products.await(),
                botPings = botPings.await(),
                refreshedAtMillis = nowMillis(),
                deliverySummary = delivery.await()
            )
        }
    }

    /** 보조 패널 하나가 비어도 오늘 매출과 핵심 차트는 표시하되, 인증 만료는 숨기지 않는다. */
    private suspend fun optional(endpoint: OMSAPIClient.ReadEndpoint): JSONValue = try {
        client.get(endpoint, token)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (unauthorized: OMSAPIError.Unauthorized) {
        throw unauthorized
    } catch (_: Exception) {
        JSONValue.Array(emptyList())
    }

    private suspend fun optionalDelivery(): DeliverySummary = try {
        deliveryClient.loadSummary()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        emptyDeliverySummary()
    }

    companion object {
        private val SEOUL = ZoneId.of("Asia/Seoul")
        private val DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
        private val MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM", Locale.US)
        private val YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy", Locale.US)
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
        private val CHART_DATE_FORMATTERS = listOf(
            DateTimeFormatter.ofPattern("MMM d", Locale.US),
            DateTimeFormatter.ofPattern("MMMd", Locale.US)
        )
        private val PING_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .toFormatter(Locale.US)

        fun emptyDeliverySummary() = DeliverySummary(
            dateRange = "택배 집계 조회 대기",
            statuses = DeliveryStatus.entries.map { DeliveryStatusCount(it, 0) }
        )

        fun makeSnapshot(
            currentMillis: Long,
            month: String,
            daily: JSONValue,
            chart: JSONValue,
            referenceHourly: JSONValue,
            todayHourly: JSONValue,
            targets: JSONValue,
            monthlySales: JSONValue,
            levels: JSONValue,
            products: JSONValue,
            botPings: JSONValue = JSONValue.Array(emptyList()),
            refreshedAtMillis: Long? = null,
            deliverySummary: DeliverySummary = emptyDeliverySummary()
        ): BriefSnapshot {
            val current = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(currentMillis), SEOUL)
            val day = current.dayOfMonth
            val todayKey = current.format(DAY_FORMATTER)
            val dailyRows = rows(daily)
            val todayRow = dailyRows.firstOrNull { string(it, "date") == todayKey }
            val todayWon = channelWon(todayRow)
            val monthWon = dailyRows.sumOf { channelWon(it) }
            val yesterdayKey = current.minusDays(1).format(DAY_FORMATTER)
            val yesterdayWon = channelWon(dailyRows.firstOrNull { string(it, "date") == yesterdayKey })
            val thresholds = rows(levels).firstOrNull()
            val todayLevel = level(todayWon.toDouble(), thresholds)

            val chartBody = chart.objectValue ?: emptyMap()
            val chartRows = rows(chartBody["chartData"] ?: chart)
            val averageWon = number(chartBody, "average").toInt()
            val monthTrend = chartRows.mapNotNull { row ->
                val text = string(row, "date") ?: return@mapNotNull null
                val date = parseChartDate(text, current) ?: return@mapNotNull null
                MonthlyPoint(
                    date = date.atStartOfDay(SEOUL).toInstant().toEpochMilli(),
                    count = Math.round(number(row, "sales") / 10_000.0).toInt()
                )
            }
            val monthPoints = monthTrend.filter {
                val pointDate = java.time.Instant.ofEpochMilli(it.date).atZone(SEOUL).toLocalDate()
                pointDate.year == current.year && pointDate.month == current.month
            }
            val levelMix = listOf("위험", "낮음", "보통", "높음", "최고").map { name ->
                SlicePortion(
                    name = name,
                    value = monthPoints.count { level((it.count * 10_000).toDouble(), thresholds) == name }.toDouble()
                )
            }

            val referenceByHour = hourlyValues(rows(referenceHourly), multiplier = 1.0)
            val todayByHour = hourlyValues(rows(todayHourly), multiplier = 10.0)
            val hours = (referenceByHour.keys + todayByHour.keys).sorted()
            val dayTrend = hours.map { hour ->
                DailyPoint(
                    label = String.format(Locale.US, "%02dh", hour),
                    reference = referenceByHour[hour],
                    result = todayByHour[hour]
                )
            }

            val targetWon = rows(targets).firstOrNull { string(it, "date") == month }
                ?.let { number(it, "sales_target").toInt() } ?: 0
            val monthlyRows = rows(monthlySales)
            val previousMonth = current.minusMonths(1).format(MONTH_FORMATTER)
            val previousMonthWon = monthlyRows.firstOrNull { string(it, "year_month") == previousMonth }
                ?.let { number(it, "monthly_sales_amount").toInt() } ?: 0
            val authoritativeMonthWon = monthlyRows.firstOrNull { string(it, "year_month") == month }
                ?.let { number(it, "monthly_sales_amount").toInt() } ?: monthWon
            val monthAverageWon = if (day > 0) authoritativeMonthWon / day else averageWon
            val refreshMillis = refreshedAtMillis ?: currentMillis
            val refreshedAtText = ZonedDateTime
                .ofInstant(java.time.Instant.ofEpochMilli(refreshMillis), SEOUL)
                .format(TIME_FORMATTER)

            val widget = DashboardWidgetSnapshot(
                schemaVersion = DashboardWidgetSnapshot.CURRENT_SCHEMA_VERSION,
                todayAmount = todayWon.toLong(),
                yesterdayAmount = yesterdayWon.toLong(),
                monthTotal = authoritativeMonthWon.toLong(),
                previousMonthTotal = previousMonthWon.toLong(),
                dailyAverage = monthAverageWon.toLong(),
                targetAmount = targetWon.toLong(),
                refreshedAt = refreshMillis,
                serverTime = refreshMillis,
                isSample = false
            )

            return BriefSnapshot(
                overview = BriefOverview(
                    todaySales = TodaySales(day = day, level = todayLevel, amount = tenThousandUnits(todayWon)),
                    channels = channelShares(todayRow),
                    monthLabel = "${current.monthValue}월",
                    monthProgress = if (targetWon > 0) {
                        (authoritativeMonthWon.toDouble() / targetWon * 100).roundToInt()
                    } else {
                        0
                    },
                    monthTotal = tenThousandUnits(authoritativeMonthWon),
                    monthAverage = tenThousandUnits(monthAverageWon),
                    levelMix = levelMix,
                    categoryMix = productMix(rows(products)),
                    refreshedAt = refreshedAtText,
                    serverStatuses = serverStatuses(rows(botPings), currentMillis)
                ),
                dayTrend = dayTrend,
                monthTrend = monthTrend,
                monthAverage = tenThousandUnits(averageWon),
                deliverySummary = deliverySummary,
                widgetSnapshot = widget
            )
        }

        private fun rows(value: JSONValue): List<Map<String, JSONValue>> {
            val source = value.arrayValue
                ?: value.objectValue?.get("data")?.arrayValue
                ?: emptyList()
            return source.mapNotNull { it.objectValue }
        }

        private fun string(row: Map<String, JSONValue>, key: String): String? = row[key]?.stringValue
        private fun number(row: Map<String, JSONValue>, key: String): Double = row[key]?.doubleValue ?: 0.0

        /** 기존 OMS 화면의 Math.round(amount / 10000)와 같은 만원 단위 표기 규칙. */
        private fun tenThousandUnits(amount: Int): Int =
            Math.round(amount.toDouble() / 10_000.0).toInt()

        private fun channelWon(row: Map<String, JSONValue>?): Int {
            if (row == null) return 0
            return (number(row, "is_onsite") + number(row, "is_store") + number(row, "is_normal")).toInt()
        }

        private fun channelShares(row: Map<String, JSONValue>?): List<ChannelShare> {
            if (row == null) return emptyList()
            val entries = listOf(
                "스토어" to number(row, "is_store"),
                "방판" to number(row, "is_onsite"),
                "전화" to number(row, "is_normal")
            )
            val total = entries.sumOf { it.second }
            return entries.map { (name, won) ->
                ChannelShare(
                    name = name,
                    count = Math.round(won / 10_000.0).toInt(),
                    percentage = if (total > 0) Math.round(won / total * 100).toInt() else 0
                )
            }
        }

        /** 기존 OMS 현황판의 비교 규칙: 30일 합계는 만원, 오늘 값은 비교를 위해 10배 확대한다. */
        private fun hourlyValues(rows: List<Map<String, JSONValue>>, multiplier: Double): Map<Int, Int> {
            val rawByHour = mutableMapOf<Int, Double>()
            for (row in rows) {
                val text = string(row, "hour") ?: continue
                val hour = normalizedHour(text) ?: continue
                rawByHour[hour] = (rawByHour[hour] ?: 0.0) + number(row, "sales")
            }
            return rawByHour.mapValues { (_, raw) -> Math.round(raw * multiplier / 10_000.0).toInt() }
        }

        private fun normalizedHour(text: String): Int? {
            val digits = text.filter { it.isDigit() }
            val hour = digits.toIntOrNull() ?: return null
            return if (hour in 0..23) hour else null
        }

        private fun level(sales: Double, thresholds: Map<String, JSONValue>?): String {
            if (thresholds == null) return "확인중"
            return when {
                sales >= number(thresholds, "level5_min") -> "최고"
                sales >= number(thresholds, "level4_min") -> "높음"
                sales >= number(thresholds, "level3_min") -> "보통"
                sales >= number(thresholds, "level2_min") -> "낮음"
                else -> "위험"
            }
        }

        private fun productMix(rows: List<Map<String, JSONValue>>): List<SlicePortion> {
            val sums = mutableMapOf<String, Double>()
            rows.forEach { row ->
                val name = string(row, "product_name") ?: "기타"
                sums[name] = (sums[name] ?: 0.0) + number(row, "price_sum")
            }
            val sorted = sums.entries.sortedByDescending { it.value }
            val result = sorted.take(5).map { SlicePortion(it.key, it.value) }.toMutableList()
            val remainder = sorted.drop(5).sumOf { it.value }
            if (remainder > 0) result.add(SlicePortion("기타", remainder))
            return result
        }

        /** 기존 OMS와 동일하게 마지막 핑이 현재 시각 기준 10분 이내면 정상으로 본다. */
        private fun serverStatuses(rows: List<Map<String, JSONValue>>, currentMillis: Long): List<ServerStatus> {
            val servers = listOf(
                "HBot-0 : 장항" to "장항",
                "HBot-2 : 인천" to "인천",
                "HBot-1 : 삼송" to "삼송",
                "HBot-3 : 초월" to "초월"
            )
            return servers.map { (botName, displayName) ->
                val row = rows.firstOrNull { string(it, "name") == botName }
                val pingMillis = row?.let { string(it, "ping_date") }?.let { parsePingDate(it) }
                val isOperational = pingMillis?.let { currentMillis - it <= 10 * 60 * 1000L } ?: false
                ServerStatus(name = displayName, isOperational = isOperational)
            }
        }

        internal fun parsePingDate(text: String): Long? {
            val normalized = text.replace("T", " ").replace("Z", "")
            return runCatching {
                LocalDateTime.parse(normalized, PING_DATE_FORMATTER)
                    .atZone(SEOUL)
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()
        }

        internal fun parseChartDate(text: String, current: ZonedDateTime): LocalDate? {
            runCatching { return LocalDate.parse(text, DAY_FORMATTER) }
            val monthDay = CHART_DATE_FORMATTERS.firstNotNullOfOrNull { formatter ->
                runCatching { MonthDay.parse(text, formatter) }.getOrNull()
            } ?: return null
            var candidate = runCatching {
                LocalDate.of(current.year, monthDay.month, monthDay.dayOfMonth)
            }.getOrNull() ?: return null
            if (candidate.isAfter(current.toLocalDate().plusDays(1))) {
                candidate = candidate.minusYears(1)
            }
            return candidate
        }
    }
}
