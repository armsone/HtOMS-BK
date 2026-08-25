package com.htoms.brief.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.htoms.brief.theme.BriefTheme
import com.htoms.brief.update.AppUpdateManager
import com.htoms.brief.update.UpdatePhase

/** 모든 브리프 섹션을 세로 스크롤 한 페이지로 보여 주는 루트 화면. */
@Composable
fun BriefRootScreen(
    viewModel: BriefViewModel,
    onLogout: (() -> Unit)?
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BriefTheme.background)
    ) {
        if (onLogout != null) {
            HeaderBar(
                isLoading = state.isLoading,
                onRefresh = viewModel::refresh,
                onLogout = onLogout
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            val snapshot = state.snapshot
            when {
                snapshot != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 1000.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 20.dp, bottom = 48.dp),
                            verticalArrangement = Arrangement.spacedBy(36.dp)
                        ) {
                            OverviewPage(
                                overview = snapshot.overview,
                                refreshCountdown = state.refreshCountdown,
                                onRefresh = viewModel::refresh
                            )
                            DayTrendPage(points = snapshot.dayTrend)
                            MonthTrendPage(points = snapshot.monthTrend, average = snapshot.monthAverage)
                            DeliveryPage(summary = snapshot.deliverySummary)
                        }
                    }
                }
                state.loadError != null -> FullScreenError(
                    message = state.loadError ?: "",
                    onRetry = viewModel::refresh
                )
                else -> LoadingView()
            }

            if (state.snapshot != null && state.loadError != null) {
                StaleDataBanner(
                    loadError = state.loadError ?: "",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun AppUpdatePanel(updateManager: AppUpdateManager) {
    val state by updateManager.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BriefTheme.boardCell)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "앱 업데이트",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(text = state.message, fontSize = 12.sp, color = BriefTheme.mutedText)
                state.release?.notes?.lineSequence()?.firstOrNull { it.isNotBlank() }?.let { note ->
                    Text(text = note, fontSize = 11.sp, color = BriefTheme.mutedText)
                }
            }
            Text(text = "자동 다운로드", fontSize = 12.sp, color = BriefTheme.mutedText)
            Switch(
                checked = state.automaticallyDownloads,
                onCheckedChange = updateManager::setAutomaticallyDownloads,
                modifier = Modifier.testTag("update-auto-download")
            )
        }

        if (state.phase == UpdatePhase.DOWNLOADING || state.phase == UpdatePhase.VERIFYING) {
            val progress = state.progressPercent
            if (progress == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.phase != UpdatePhase.DOWNLOADING && state.phase != UpdatePhase.VERIFYING) {
                UpdateAction("업데이트 확인", "update-check") {
                    updateManager.checkForUpdates(manual = true)
                }
            }
            when (state.phase) {
                UpdatePhase.AVAILABLE -> UpdateAction("다운로드", "update-download", updateManager::downloadManually)
                UpdatePhase.DOWNLOADING -> UpdateAction("취소", "update-cancel", updateManager::cancelDownload)
                UpdatePhase.READY, UpdatePhase.PERMISSION_REQUIRED ->
                    UpdateAction("설치", "update-install", updateManager::install)
                UpdatePhase.ERROR -> UpdateAction("다시 시도", "update-retry", updateManager::retry)
                else -> Unit
            }
        }
    }
}

@Composable
private fun UpdateAction(title: String, testTag: String, action: () -> Unit) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = BriefTheme.boardAmber,
        modifier = Modifier
            .focusBorder(cornerRadius = 8.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable(onClick = action)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(testTag)
    )
}

@Composable
private fun HeaderBar(
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BriefTheme.boardCell)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HTOMS BRIEF",
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = BriefTheme.boardAmber
            )
            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = onRefresh,
                enabled = !isLoading,
                modifier = Modifier
                    .focusBorder(cornerRadius = 20.dp)
                    .size(40.dp)
                    .testTag("header-refresh")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = BriefTheme.mutedText,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(18.dp)
                            .testTag("header-refresh-progress")
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "데이터 새로고침. OMS의 최신 조회 데이터를 다시 불러옵니다",
                        tint = BriefTheme.mutedText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = "로그아웃",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = BriefTheme.mutedText,
                modifier = Modifier
                    .focusBorder(cornerRadius = 8.dp)
                    .clip(CircleShape)
                    .clickable(
                        onClickLabel = "세션을 종료하고 로그인 화면으로 돌아갑니다",
                        onClick = onLogout
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("header-logout")
            )
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BriefTheme.boardAmber.copy(alpha = 0.35f))
        )
    }
}

@Composable
private fun LoadingView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("brief-loading"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = BriefTheme.accent)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "브리프 불러오는 중",
            fontSize = 14.sp,
            color = BriefTheme.mutedText
        )
    }
}

@Composable
private fun FullScreenError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .testTag("brief-full-error"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.WifiOff,
            contentDescription = null,
            tint = BriefTheme.mutedText,
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "브리프를 불러오지 못했습니다",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            color = BriefTheme.mutedText
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "다시 불러오기",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1A1708),
            modifier = Modifier
                .focusBorder(cornerRadius = 10.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                .background(BriefTheme.boardAmber)
                .clickable(onClick = onRetry)
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .testTag("brief-retry")
        )
    }
}

@Composable
private fun StaleDataBanner(loadError: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(BriefTheme.boardCell)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "갱신 실패. 이전 데이터를 표시합니다. $loadError"
            }
            .testTag("stale-banner"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = BriefTheme.negative,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "갱신 실패 · 이전 데이터 표시 중",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = BriefTheme.negative
        )
    }
}
