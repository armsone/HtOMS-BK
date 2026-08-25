package com.htoms.brief.ui

import com.htoms.brief.api.OMSAPIError
import com.htoms.brief.model.BriefSnapshot
import com.htoms.brief.provider.BriefProviding
import com.htoms.brief.provider.SampleBriefProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** 로드 성공/실패 상태 전이, 10분 카운트다운, 이전 데이터 유지 배너 조건 검증. */
@OptIn(ExperimentalCoroutinesApi::class)
class BriefViewModelTest {

    private class ScriptedProvider(
        private val results: MutableList<Result<BriefSnapshot>>
    ) : BriefProviding {
        var callCount = 0
        override suspend fun loadSnapshot(): BriefSnapshot {
            callCount += 1
            return results.removeAt(0).getOrThrow()
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoadPopulatesSnapshotAndResetsCountdown() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val provider = ScriptedProvider(mutableListOf(Result.success(SampleBriefProvider.snapshot)))
        var savedCount = 0
        val viewModel = BriefViewModel(
            provider = provider,
            onSnapshotLoaded = { savedCount += 1 },
            nowMillis = { testScheduler.currentTime }
        )
        runCurrent()

        val state = viewModel.state.value
        assertEquals(SampleBriefProvider.snapshot, state.snapshot)
        assertNull(state.loadError)
        assertEquals(false, state.isLoading)
        assertEquals(BriefViewModel.REFRESH_INTERVAL_SECONDS, state.refreshCountdown)
        assertEquals(1, provider.callCount)
        assertEquals(1, savedCount)

        viewModel.viewModelScopeCancelForTest()
    }

    @Test
    fun failureWithoutSnapshotExposesFullScreenErrorMessage() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val provider = ScriptedProvider(mutableListOf(Result.failure(OMSAPIError.Server(503))))
        val viewModel = BriefViewModel(
            provider = provider,
            nowMillis = { testScheduler.currentTime }
        )
        runCurrent()

        val state = viewModel.state.value
        assertNull(state.snapshot)
        assertEquals("OMS 서버 요청에 실패했습니다. (503)", state.loadError)

        viewModel.viewModelScopeCancelForTest()
    }

    @Test
    fun refreshFailureKeepsPreviousSnapshotForStaleBanner() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val provider = ScriptedProvider(
            mutableListOf(
                Result.success(SampleBriefProvider.snapshot),
                Result.failure(OMSAPIError.Server(500))
            )
        )
        val viewModel = BriefViewModel(provider = provider, nowMillis = { testScheduler.currentTime })
        runCurrent()

        viewModel.refresh()
        runCurrent()

        val state = viewModel.state.value
        assertNotNull(state.snapshot) // 이전 데이터 유지
        assertEquals("OMS 서버 요청에 실패했습니다. (500)", state.loadError)

        viewModel.viewModelScopeCancelForTest()
    }

    @Test
    fun countdownTicksEverySecondAndTriggersReloadAtZero() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val provider = ScriptedProvider(
            mutableListOf(
                Result.success(SampleBriefProvider.snapshot),
                Result.success(SampleBriefProvider.snapshot)
            )
        )
        val viewModel = BriefViewModel(provider = provider, nowMillis = { testScheduler.currentTime })
        runCurrent()
        assertEquals(1, provider.callCount)

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(BriefViewModel.REFRESH_INTERVAL_SECONDS - 1, viewModel.state.value.refreshCountdown)

        // 10분 경과 시(0초 도달) 자동 재로드 후 카운트다운이 재설정된다.
        advanceTimeBy((BriefViewModel.REFRESH_INTERVAL_SECONDS - 1) * 1_000L)
        runCurrent()
        assertEquals(2, provider.callCount)
        assertEquals(BriefViewModel.REFRESH_INTERVAL_SECONDS, viewModel.state.value.refreshCountdown)

        viewModel.viewModelScopeCancelForTest()
    }
}

/** 테스트에서 주기 루프를 정리해 대기 중인 delay가 남지 않게 한다. */
private fun BriefViewModel.viewModelScopeCancelForTest() {
    viewModelScope.cancel()
}
