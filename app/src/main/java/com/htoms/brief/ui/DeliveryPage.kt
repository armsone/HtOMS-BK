package com.htoms.brief.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htoms.brief.model.DeliverySummary
import com.htoms.brief.theme.BriefFormat
import com.htoms.brief.theme.BriefTheme

/** 택배 현황 섹션: 전체 건수와 8단계 상태별 집계 그리드. */
@Composable
fun DeliveryPage(summary: DeliverySummary) {
    BriefSection(title = "택배 현황", subtitle = summary.dateRange) {
        BriefCard("전체 배송 · DELIVERY", modifier = Modifier.focusHighlight().testTag("delivery-card")) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "전체 배송 ${BriefFormat.number(summary.total)}건"
                }
            ) {
                Text(
                    text = BriefFormat.number(summary.total),
                    fontSize = 44.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White
                )
                Text(
                    text = "건",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BriefTheme.mutedText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BriefTheme.mutedText.copy(alpha = 0.35f))
            )

            AdaptiveGrid(
                minCellWidth = 145.dp,
                horizontalSpacing = 12.dp,
                verticalSpacing = 14.dp,
                items = summary.statuses.map { item ->
                    {
                        val color = BriefTheme.color(item.status)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) {
                                    contentDescription =
                                        "${item.status.label} ${BriefFormat.number(item.count)}건"
                                }
                                .testTag("delivery-status-${item.status.name}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = item.status.label, fontSize = 19.sp, color = color)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${BriefFormat.number(item.count)} 건",
                                fontSize = 19.sp,
                                fontFamily = FontFamily.Monospace,
                                color = color
                            )
                        }
                    }
                }
            )
        }
    }
}
