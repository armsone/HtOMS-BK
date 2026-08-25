package com.htoms.brief.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htoms.brief.model.MonthlyPoint
import com.htoms.brief.theme.BriefFormat
import com.htoms.brief.theme.BriefTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val SEOUL = ZoneId.of("Asia/Seoul")
private val RANGE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.US)

/** 월간 일별 매출 추이: 회색 실선, 0 매출 지점은 적색, 평균선은 오렌지 점선. */
@Composable
fun MonthTrendPage(points: List<MonthlyPoint>, average: Int) {
    val dates = points.map { Instant.ofEpochMilli(it.date).atZone(SEOUL).toLocalDate() }
    val dateRange = if (dates.isNotEmpty()) {
        "${dates.first().format(RANGE_FORMATTER)}~${dates.last().format(RANGE_FORMATTER)}"
    } else {
        "최근 30일"
    }

    BriefSection(title = "월간 매출", subtitle = "MONTH · $dateRange") {
        BriefCard("일별 매출", modifier = Modifier.focusHighlight()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "단위: 만원",
                    fontSize = 11.sp,
                    color = BriefTheme.mutedText
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "평균 ${BriefFormat.number(average)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BriefTheme.warning
                )
            }

            val textMeasurer = rememberTextMeasurer()
            val summary = "월간 일별 판매 추이, 평균 $average"
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .testTag("month-trend-chart")
                    .clearAndSetSemantics { contentDescription = summary }
            ) {
                drawMonthTrendChart(points, dates, average, textMeasurer)
            }
        }
    }
}

private fun DrawScope.drawMonthTrendChart(
    points: List<MonthlyPoint>,
    dates: List<LocalDate>,
    average: Int,
    textMeasurer: TextMeasurer
) {
    if (points.isEmpty()) return
    val leftGutter = 44.dp.toPx()
    val bottomGutter = 22.dp.toPx()
    val chartWidth = size.width - leftGutter
    val chartHeight = size.height - bottomGutter
    if (chartWidth <= 0f || chartHeight <= 0f) return

    val firstDate = dates.first()
    val lastDate = dates.last()
    val domainSpan = ChronoUnit.DAYS.between(firstDate, lastDate).coerceAtLeast(1L).toFloat()

    val maxValue = maxOf(points.maxOf { it.count }, average)
    val ticks = ChartSupport.yTicks(maxValue)
    val yTop = ticks.last().coerceAtLeast(1)

    fun x(date: LocalDate): Float =
        leftGutter + ChronoUnit.DAYS.between(firstDate, date).toFloat() / domainSpan * chartWidth

    fun y(value: Int): Float = chartHeight - value.toFloat() / yTop * chartHeight

    val axisLabelStyle = TextStyle(
        color = BriefTheme.mutedText,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace
    )

    // Y축 눈금·그리드 (leading)
    ticks.forEach { tick ->
        val ty = y(tick)
        drawLine(
            color = Color.White.copy(alpha = 0.06f),
            start = Offset(leftGutter, ty),
            end = Offset(size.width, ty),
            strokeWidth = 1f
        )
        val layout = textMeasurer.measure(BriefFormat.number(tick), axisLabelStyle)
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(leftGutter - layout.size.width - 6.dp.toPx(), ty - layout.size.height / 2f)
        )
    }

    // X축 눈금: 6일 간격, "7월 21일" 형식
    var axisDate = firstDate
    while (!axisDate.isAfter(lastDate)) {
        val tx = x(axisDate)
        drawLine(
            color = Color.White.copy(alpha = 0.05f),
            start = Offset(tx, 0f),
            end = Offset(tx, chartHeight),
            strokeWidth = 1f
        )
        val label = "${axisDate.monthValue}월 ${axisDate.dayOfMonth}일"
        val layout = textMeasurer.measure(label, axisLabelStyle)
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(
                (tx - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width),
                chartHeight + 4.dp.toPx()
            )
        )
        axisDate = axisDate.plusDays(6)
    }

    // 일별 매출 선: 회색 1.6dp 실선
    var previous: Offset? = null
    points.forEachIndexed { index, point ->
        val current = Offset(x(dates[index]), y(point.count))
        previous?.let {
            drawLine(
                color = Color.White.copy(alpha = 0.54f),
                start = it,
                end = current,
                strokeWidth = 1.6.dp.toPx()
            )
        }
        previous = current
    }

    // 평균선: 오렌지 1.15dp 점선 [5,4]
    val avgY = y(average.coerceIn(0, yTop))
    drawLine(
        color = BriefTheme.warning.copy(alpha = 0.72f),
        start = Offset(leftGutter, avgY),
        end = Offset(size.width, avgY),
        strokeWidth = 1.15.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))
    )

    // 점: 판매 있음(흰색 62%) / 0 매출(적색)
    points.forEachIndexed { index, point ->
        drawCircle(
            color = if (point.count > 0) Color.White.copy(alpha = 0.62f) else BriefTheme.negative,
            radius = 2.25.dp.toPx(),
            center = Offset(x(dates[index]), y(point.count))
        )
    }
}
