package com.htoms.brief.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.htoms.brief.model.BriefSnapshot
import com.htoms.brief.provider.BriefProviding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max

/**
 * 브리프 로드·10분 주기 자동 갱신·초 단위 카운트다운을 담당한다.
 * viewModelScope에 묶여 화면 종료(로그아웃) 시 주기 작업과 네트워크가 함께 취소된다.
 */
class BriefViewModel(
    private val provider: BriefProviding,
    private val onSnapshotLoaded: suspend (BriefSnapshot) -> Unit = {},
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    data class UiState(
        val snapshot: BriefSnapshot? = null,
        val loadError: String? = null,
        val isLoading: Boolean = false,
        val refreshCountdown: Int = REFRESH_INTERVAL_SECONDS
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var nextRefreshAtMillis: Long = nowMillis() + REFRESH_INTERVAL_SECONDS * 1000L

    init {
        viewModelScope.launch {
            load()
            while (isActive) {
                delay(1_000)
                val remaining = max(
                    0,
                    ceil((nextRefreshAtMillis - nowMillis()) / 1000.0).toInt()
                )
                _state.value = _state.value.copy(refreshCountdown = remaining)
                if (remaining == 0) {
                    load()
                }
            }
        }
    }

    /** 수동 갱신(헤더 버튼·REFRESH 카드·재시도 버튼). 로드 중이면 무시한다. */
    fun refresh() {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        if (_state.value.isLoading) return
        _state.value = _state.value.copy(isLoading = true, loadError = null)
        try {
            val loaded = provider.loadSnapshot()
            _state.value = _state.value.copy(snapshot = loaded)
            runCatching { onSnapshotLoaded(loaded) }
        } catch (error: Exception) {
            _state.value = _state.value.copy(
                loadError = error.message ?: "네트워크 연결을 확인한 뒤 다시 시도해 주세요."
            )
        } finally {
            nextRefreshAtMillis = nowMillis() + REFRESH_INTERVAL_SECONDS * 1000L
            _state.value = _state.value.copy(
                isLoading = false,
                refreshCountdown = REFRESH_INTERVAL_SECONDS
            )
        }
    }

    companion object {
        const val REFRESH_INTERVAL_SECONDS = 10 * 60

        fun factory(
            provider: BriefProviding,
            onSnapshotLoaded: suspend (BriefSnapshot) -> Unit
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                BriefViewModel(provider, onSnapshotLoaded) as T
        }
    }
}
