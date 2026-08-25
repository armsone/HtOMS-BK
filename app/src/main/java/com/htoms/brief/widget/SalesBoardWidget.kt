package com.htoms.brief.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.htoms.brief.model.DashboardWidgetSnapshot

/** 홈 화면 위젯 리시버. 시스템 APPWIDGET_UPDATE를 Glance 위젯으로 전달한다. */
class SalesBoardGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SalesBoardWidget()
}

/**
 * 8행 공항 안내판 스타일 매출 현황 위젯.
 * 앱이 저장한 표시 전용 스냅샷만 읽으며, 없으면 SAMPLE 데이터를 보여 준다.
 */
class SalesBoardWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SMALL, MEDIUM, LARGE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotStore(context).load() ?: DashboardWidgetSnapshot.sample
        provideContent {
            BoardContent(snapshot)
        }
    }

    @Composable
    private fun BoardContent(snapshot: DashboardWidgetSnapshot) {
        val size = LocalSize.current
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(BACKGROUND)
        ) {
            when {
                size.height >= LARGE.height && size.width >= LARGE.width -> LargeBoard(snapshot)
                size.width >= MEDIUM.width -> MediumBoard(snapshot)
                else -> SmallBoard(snapshot)
            }
        }
    }

    @Composable
    private fun SmallBoard(snapshot: DashboardWidgetSnapshot) {
        Column(modifier = GlanceModifier.fillMaxSize().padding(10.dp)) {
            CompactValueRow("금일(${WidgetFormat.currentDay(snapshot)})", snapshot.todayAmount)
            CompactValueRow("전일", snapshot.yesterdayAmount)
            CompactValueRow("당월(${WidgetFormat.currentMonth(snapshot)})", snapshot.monthTotal)
            CompactValueRow("전월", snapshot.previousMonthTotal)
            CompactValueRow("평균", snapshot.dailyAverage)
            CompactValueRow("목표(${WidgetFormat.achievementRate(snapshot)}%)", snapshot.targetAmount)
            CompactTimestampRow("시간", snapshot.refreshedAt)
            CompactTimestampRow("서버", snapshot.serverTime)
        }
    }

    @Composable
    private fun MediumBoard(snapshot: DashboardWidgetSnapshot) {
        Column(modifier = GlanceModifier.fillMaxSize().padding(16.dp)) {
            BoardHeader(snapshot)
            Spacer(GlanceModifier.height(10.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Column(modifier = GlanceModifier.width(90.dp)) {
                    ValueRow("금일", snapshot.todayAmount)
                    Spacer(GlanceModifier.height(7.dp))
                    ValueRow("전일", snapshot.yesterdayAmount)
                }
                Spacer(GlanceModifier.width(8.dp))
                Column(modifier = GlanceModifier.width(90.dp)) {
                    ValueRow("당월", snapshot.monthTotal)
                    Spacer(GlanceModifier.height(7.dp))
                    ValueRow("목표", snapshot.targetAmount)
                }
            }
            Spacer(GlanceModifier.height(4.dp))
            TimestampRow("갱신", snapshot.refreshedAt)
        }
    }

    @Composable
    private fun LargeBoard(snapshot: DashboardWidgetSnapshot) {
        Column(modifier = GlanceModifier.fillMaxSize().padding(18.dp)) {
            BoardHeader(snapshot)
            Spacer(GlanceModifier.height(9.dp))
            listOf(
                "금일" to snapshot.todayAmount,
                "전일" to snapshot.yesterdayAmount,
                "당월" to snapshot.monthTotal,
                "전월" to snapshot.previousMonthTotal,
                "평균" to snapshot.dailyAverage,
                "목표" to snapshot.targetAmount
            ).forEach { (label, value) ->
                ValueRow(label, value)
                Spacer(GlanceModifier.height(9.dp))
            }
            Spacer(GlanceModifier.height(4.dp))
            TimestampRow("갱신", snapshot.refreshedAt)
            TimestampRow("서버", snapshot.serverTime)
        }
    }

    @Composable
    private fun BoardHeader(snapshot: DashboardWidgetSnapshot) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "현황판",
                style = TextStyle(
                    color = WHITE,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            )
            Text(
                text = " 매출 · 만원",
                style = TextStyle(
                    color = MUTED,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
            Spacer(GlanceModifier.width(8.dp))
            if (snapshot.isSample) {
                Text(
                    text = "SAMPLE",
                    style = TextStyle(
                        color = ORANGE,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }

    @Composable
    private fun ValueRow(label: String, value: Long) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(BOARD_CELL)
                .cornerRadius(6.dp)
                .padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = TextStyle(
                    color = MUTED,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
            )
            Spacer(GlanceModifier.width(12.dp))
            Text(
                text = WidgetFormat.compactWon(value),
                style = TextStyle(
                    color = WHITE,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }

    @Composable
    private fun CompactValueRow(label: String, value: Long) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = TextStyle(
                    color = MUTED,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                maxLines = 1
            )
            Spacer(GlanceModifier.width(4.dp))
            Text(
                text = WidgetFormat.compactWon(value),
                style = TextStyle(
                    color = WHITE,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                maxLines = 1
            )
        }
    }

    @Composable
    private fun CompactTimestampRow(label: String, epochMillis: Long) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = TextStyle(
                    color = MUTED,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                ),
                maxLines = 1
            )
            Spacer(GlanceModifier.width(4.dp))
            Text(
                text = WidgetFormat.compactTimestamp(epochMillis),
                style = TextStyle(
                    color = WHITE,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                ),
                maxLines = 1
            )
        }
    }

    @Composable
    private fun TimestampRow(label: String, epochMillis: Long) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = TextStyle(color = MUTED, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = WidgetFormat.compactTimestamp(epochMillis),
                style = TextStyle(color = MUTED, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
        }
    }

    companion object {
        private val SMALL = DpSize(110.dp, 110.dp)
        private val MEDIUM = DpSize(220.dp, 110.dp)
        private val LARGE = DpSize(220.dp, 240.dp)

        private val BACKGROUND = ColorProvider(Color(0xFF0E0E0F))
        private val BOARD_CELL = ColorProvider(Color(0xFF18181B))
        private val ORANGE = ColorProvider(Color(0xFFF26436))
        private val MUTED = ColorProvider(Color(0xFFADADB3))
        private val WHITE = ColorProvider(Color.White)
    }
}
