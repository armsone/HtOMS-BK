package com.htoms.brief.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htoms.brief.model.BriefOverview
import com.htoms.brief.model.ChannelShare
import com.htoms.brief.theme.BriefFormat
import com.htoms.brief.theme.BriefTheme
import java.util.Locale

/** 매출 요약 섹션: 오늘 매출 KPI, 갱신 카드, 서버 상태, 판매 채널, 월간 지표 그리드. */
@Composable
fun OverviewPage(
    overview: BriefOverview,
    refreshCountdown: Int?,
    onRefresh: (() -> Unit)?
) {
    BriefSection(title = "매출 요약", subtitle = "BRIEF · 오늘과 월간 판매") {
        TodaySalesCard(overview)
        RefreshCard(overview, refreshCountdown, onRefresh)
        ServerStatusCard(overview)

        BriefCard("판매 채널", modifier = Modifier.focusHighlight().testTag("channel-mix-card")) {
            ChannelMixView(channels = overview.channels)
        }

        AdaptiveGrid(
            minCellWidth = 300.dp,
            horizontalSpacing = 16.dp,
            verticalSpacing = 16.dp,
            items = listOf(
                {
                    MetricCard(
                        title = "월 누계 · ${overview.monthLabel} (${overview.monthProgress}%)",
                        value = overview.monthTotal,
                        tag = "metric-month-total"
                    )
                },
                {
                    MetricCard(
                        title = "일 평균 · MONTH AVG",
                        value = overview.monthAverage,
                        tag = "metric-month-average"
                    )
                },
                {
                    BriefCard("매출 등급 · LEVEL", modifier = Modifier.focusHighlight().testTag("level-mix-card")) {
                        DoughnutChartView(unitName = "Level", portions = overview.levelMix)
                    }
                },
                {
                    BriefCard("상품 분류 · CATEGORY", modifier = Modifier.focusHighlight().testTag("category-mix-card")) {
                        DoughnutChartView(unitName = "Category", portions = overview.categoryMix)
                    }
                }
            )
        )
    }
}

@Composable
private fun TodaySalesCard(overview: BriefOverview) {
    val summary = "오늘 ${overview.todaySales.day}일 매출 " +
        "${BriefFormat.number(overview.todaySales.amount)}, 등급 ${overview.todaySales.level}"
    BriefCard(
        "오늘 매출 · TODAY",
        modifier = Modifier
            .focusHighlight()
            .testTag("today-sales-card")
            .semantics { contentDescription = summary }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = overview.todaySales.level,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(BriefTheme.warning.copy(alpha = 0.72f))
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${overview.todaySales.day}일",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BriefTheme.accent
                )
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = BriefFormat.number(overview.todaySales.amount),
                    fontSize = 48.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = "만원",
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = BriefTheme.mutedText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun RefreshCard(
    overview: BriefOverview,
    refreshCountdown: Int?,
    onRefresh: (() -> Unit)?
) {
    val refreshText = if (refreshCountdown != null) {
        String.format(Locale.US, "%02d:%02d", refreshCountdown / 60, refreshCountdown % 60)
    } else {
        overview.refreshedAt
    }
    val cardModifier = if (onRefresh != null) {
        Modifier
            .focusBorder()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = "OMS 데이터를 다시 불러옵니다", onClick = onRefresh)
    } else {
        Modifier.focusHighlight()
    }
    BriefCard("다음 갱신 · REFRESH", modifier = cardModifier.testTag("refresh-card")) {
        CenteredValueText(refreshText)
    }
}

@Composable
private fun ServerStatusCard(overview: BriefOverview) {
    val summary = overview.serverStatuses.joinToString(", ") {
        "${it.name} ${if (it.isOperational) "정상" else "문제"}"
    }
    BriefCard(
        "서버 상태 · SERVER",
        modifier = Modifier
            .focusHighlight()
            .testTag("server-status-card")
            .clearAndSetSemantics { contentDescription = summary }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= 360.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    overview.serverStatuses.forEach { server -> ServerLabel(server.name, server.isOperational) }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    overview.serverStatuses.forEach { server -> ServerLabel(server.name, server.isOperational) }
                }
            }
        }
    }
}

@Composable
private fun ServerLabel(name: String, isOperational: Boolean) {
    Text(
        text = name,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (isOperational) BriefTheme.mutedText else BriefTheme.negative
    )
}

@Composable
private fun MetricCard(title: String, value: Int, tag: String) {
    BriefCard(title, modifier = Modifier.focusHighlight().testTag(tag)) {
        CenteredValueText(BriefFormat.number(value))
    }
}

@Composable
private fun CenteredValueText(value: String) {
    Text(
        text = value,
        fontSize = 22.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

/** 판매 채널 비례 바 + 범례. */
@Composable
fun ChannelMixView(channels: List<ChannelShare>) {
    val summary = channels.joinToString(", ") {
        "${it.name} ${BriefFormat.number(it.count)}, ${it.percentage}퍼센트"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = summary },
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
        ) {
            channels.forEachIndexed { index, channel ->
                if (channel.percentage > 0) {
                    Box(
                        modifier = Modifier
                            .weight(channel.percentage.toFloat())
                            .height(20.dp)
                            .background(BriefTheme.seriesPalette[index % BriefTheme.seriesPalette.size])
                    )
                }
            }
            val filled = channels.sumOf { it.percentage }
            if (filled < 100) {
                Spacer(modifier = Modifier.weight((100 - filled).toFloat()))
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= 480.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    ChannelLabels(channels)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChannelLabels(channels)
                }
            }
        }
    }
}

@Composable
private fun ChannelLabels(channels: List<ChannelShare>) {
    channels.forEachIndexed { index, channel ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(BriefTheme.seriesPalette[index % BriefTheme.seriesPalette.size])
            )
            Text(
                text = "${channel.name}: ${BriefFormat.number(channel.count)} [${channel.percentage}%]",
                fontSize = 13.sp,
                color = BriefTheme.mutedText
            )
        }
    }
}
