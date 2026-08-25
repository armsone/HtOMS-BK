package com.htoms.brief.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htoms.brief.model.DailyPoint
import com.htoms.brief.theme.BriefFormat
import com.htoms.brief.theme.BriefTheme

/** 시간대별 매출 비교: 오늘(×10, 오렌지 실선)과 30일 합계(회색 점선). */
@Composable
fun DayTrendPage(points: List<DailyPoint>) {
    val plotted = points.mapNotNull { point ->
        point.label.filter { it.isDigit() }.toIntOrNull()?.let { hour -> point to hour }
    }
    val accessibilitySummary = run {
        val latest = points.lastOrNull { it.result != null }
        if (latest?.result == null) {
            "시간대별 매출. 오늘 집계 데이터가 없습니다."
        } else {
            "시간대별 매출 비교. 오렌지는 오늘 매출 10배, 회색은 최근 30일 합계. " +
                "최신 ${latest.label} 오늘 비교값 ${BriefFormat.number(latest.result)}"
        }
    }

    BriefSection(title = "시간대별 매출", subtitle = "DAY · 기존 OMS 비교 기준") {
        BriefCard("시간대별 매출", modifier = Modifier.focusHighlight()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Legend(color = BriefTheme.warning, label = "오늘 ×10", dashed = false)
                Legend(color = Color.White.copy(alpha = 0.66f), label = "30일 합계", dashed = true)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "만원 비교값",
                    fontSize = 11.sp,
                    color = BriefTheme.mutedText
                )
            }

            val textMeasurer = rememberTextMeasurer()
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .testTag("day-trend-chart")
                    .clearAndSetSemantics { contentDescription = accessibilitySummary }
            ) {
                drawDayTrendChart(plotted, textMeasurer)
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String, dashed: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) {}
    ) {
        Canvas(modifier = Modifier.size(width = 22.dp, height = 2.dp)) {
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = if (dashed) {
                    PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
                } else {
                    null
                }
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = BriefTheme.mutedText
        )
    }
}

private fun DrawScope.drawDayTrendChart(
    plotted: List<Pair<DailyPoint, Int>>,
    textMeasurer: TextMeasurer
) {
    if (plotted.isEmpty()) return
    val leftGutter = 44.dp.toPx()
    val bottomGutter = 22.dp.toPx()
    val chartWidth = size.width - leftGutter
    val chartHeight = size.height - bottomGutter
    if (chartWidth <= 0f || chartHeight <= 0f) return

    val firstHour = plotted.first().second
    val lastHour = plotted.last().second
    val domainStart = firstHour - 1
    val domainEnd = lastHour + 1
    val domainSpan = (domainEnd - domainStart).coerceAtLeast(1)

    val maxValue = plotted.maxOf { maxOf(it.first.reference ?: 0, it.first.result ?: 0) }
    val ticks = ChartSupport.yTicks(maxValue)
    val yTop = ticks.last().coerceAtLeast(1)

    fun x(hour: Int): Float = leftGutter + (hour - domainStart).toFloat() / domainSpan * chartWidth
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

    // X축 눈금·그리드: 첫 시각부터 2시간 간격
    var hour = firstHour
    while (hour <= lastHour) {
        val tx = x(hour)
        drawLine(
            color = Color.White.copy(alpha = 0.05f),
            start = Offset(tx, 0f),
            end = Offset(tx, chartHeight),
            strokeWidth = 1f
        )
        val layout = textMeasurer.measure("${hour}시", axisLabelStyle)
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(tx - layout.size.width / 2f, chartHeight + 4.dp.toPx())
        )
        hour += 2
    }

    // 30일 합계: 회색 점선 1.15dp. 누락 시간은 선을 끊는다(0으로 강제하지 않음).
    drawSeries(
        plotted = plotted,
        value = { it.reference },
        x = ::x,
        y = ::y,
        lineColor = Color.White.copy(alpha = 0.66f),
        pointColor = Color.White.copy(alpha = 0.72f),
        strokeWidth = 1.15.dp.toPx(),
        pointRadius = 1.95.dp.toPx(),
        dash = floatArrayOf(4.dp.toPx(), 4.dp.toPx())
    )

    // 오늘 ×10: 오렌지 실선 1.6dp.
    drawSeries(
        plotted = plotted,
        value = { it.result },
        x = ::x,
        y = ::y,
        lineColor = BriefTheme.warning,
        pointColor = BriefTheme.warning,
        strokeWidth = 1.6.dp.toPx(),
        pointRadius = 2.25.dp.toPx(),
        dash = null
    )
}

private fun DrawScope.drawSeries(
    plotted: List<Pair<DailyPoint, Int>>,
    value: (DailyPoint) -> Int?,
    x: (Int) -> Float,
    y: (Int) -> Float,
    lineColor: Color,
    pointColor: Color,
    strokeWidth: Float,
    pointRadius: Float,
    dash: FloatArray?
) {
    val pathEffect = dash?.let { PathEffect.dashPathEffect(it) }
    var previous: Offset? = null
    plotted.forEach { (point, hour) ->
        val v = value(point)
        if (v == null) {
            previous = null
            return@forEach
        }
        val current = Offset(x(hour), y(v))
        previous?.let {
            drawLine(
                color = lineColor,
                start = it,
                end = current,
                strokeWidth = strokeWidth,
                pathEffect = pathEffect
            )
        }
        drawCircle(color = pointColor, radius = pointRadius, center = current)
        previous = current
    }
}
