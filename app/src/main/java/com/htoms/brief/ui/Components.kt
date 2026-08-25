package com.htoms.brief.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htoms.brief.model.SlicePortion
import com.htoms.brief.theme.BriefTheme
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * TV/D-pad 탐색을 위한 포커스 표시. 포커스를 받으면 브랜드 오렌지 2dp 테두리를 그린다.
 * clickable/focusable 등 포커스 대상이 되는 modifier보다 앞(왼쪽)에 배치해야 한다.
 * 터치 모드에서는 포커스 표시가 나타나지 않으므로 폰/태블릿 외관에는 영향이 없다.
 */
fun Modifier.focusBorder(cornerRadius: Dp = 12.dp): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    this
        .onFocusChanged { isFocused = it.isFocused }
        .border(
            width = 2.dp,
            color = if (isFocused) BriefTheme.brandOrange else Color.Transparent,
            shape = RoundedCornerShape(cornerRadius)
        )
}

/** 상호작용이 없는 카드도 D-pad로 훑을 수 있게 포커스 대상 + 표시를 함께 부여한다. */
fun Modifier.focusHighlight(cornerRadius: Dp = 12.dp): Modifier =
    focusBorder(cornerRadius).focusable()

/** 둥근 블루그레이 카드 컨테이너. */
@Composable
fun BriefCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BriefTheme.card)
            .border(1.dp, BriefTheme.cardStroke, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = BriefTheme.mutedText
        )
        content()
    }
}

/** 통합 브리프 페이지의 섹션 공통 골격: 인디케이터 바, 제목, 부제목, 본문. */
@Composable
fun BriefSection(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Spacer(
                modifier = Modifier
                    .width(36.dp)
                    .height(3.dp)
                    .background(BriefTheme.boardAmber)
            )
            Text(
                text = title,
                fontSize = 22.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = BriefTheme.mutedText
            )
        }
        content()
    }
}

/**
 * 최소 셀 너비 기준으로 열 수를 계산하는 적응형 그리드.
 * 스크롤 루트가 담당하는 세로 스크롤과 충돌하지 않도록 LazyGrid 대신 정적 배치를 쓴다.
 */
@Composable
fun AdaptiveGrid(
    minCellWidth: Dp,
    horizontalSpacing: Dp,
    verticalSpacing: Dp,
    modifier: Modifier = Modifier,
    items: List<@Composable () -> Unit>
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = max(
            1,
            floor(((maxWidth + horizontalSpacing) / (minCellWidth + horizontalSpacing))).toInt()
        )
        Column(verticalArrangement = Arrangement.spacedBy(verticalSpacing)) {
            items.chunked(columns).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)) {
                    rowItems.forEach { item ->
                        androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                            item()
                        }
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Level/Category 공용 도넛 차트. 색상은 seriesPalette 순서를 따른다. */
@Composable
fun DoughnutChartView(
    unitName: String,
    portions: List<SlicePortion>,
    modifier: Modifier = Modifier
) {
    val total = portions.sumOf { it.value }
    val summaryText = "$unitName 구성: " + portions.joinToString(", ") { portion ->
        val percent = if (total > 0) (portion.value * 100 / total).roundToInt() else 0
        "${portion.name} ${percent}퍼센트"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = summaryText },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            if (total <= 0) return@Canvas
            val outerRadius = size.minDimension / 2f
            val strokeWidth = outerRadius * (1f - 0.62f)
            val midRadius = outerRadius - strokeWidth / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val topLeft = Offset(center.x - midRadius, center.y - midRadius)
            val arcSize = Size(midRadius * 2f, midRadius * 2f)
            val insetDegrees = 1.5f
            var startAngle = -90f
            portions.forEachIndexed { index, portion ->
                val sweep = (portion.value / total * 360.0).toFloat()
                val drawSweep = (sweep - insetDegrees).coerceAtLeast(0f)
                if (drawSweep > 0f) {
                    drawArc(
                        color = BriefTheme.seriesPalette[index % BriefTheme.seriesPalette.size],
                        startAngle = startAngle + insetDegrees / 2f,
                        sweepAngle = drawSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )
                }
                startAngle += sweep
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            portions.chunked(2).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowItems.forEachIndexed { indexInRow, portion ->
                        val index = portions.indexOf(portion)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(BriefTheme.seriesPalette[index % BriefTheme.seriesPalette.size])
                            )
                            Text(
                                text = portion.name,
                                fontSize = 11.sp,
                                color = BriefTheme.mutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (indexInRow == rowItems.lastIndex && rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
