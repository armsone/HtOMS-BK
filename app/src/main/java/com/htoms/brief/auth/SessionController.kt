package com.htoms.brief.auth

import com.htoms.brief.security.SecureStoring
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * 세션 수립·복원·해제를 담당하는 상태 컨트롤러.
 * 토큰은 주입된 SecureStoring(앱에서는 Android Keystore 기반 저장소)에만 기록한다.
 */
class SessionController(private val store: SecureStoring) {

    private val _session = MutableStateFlow<AuthSession?>(null)
    val session: StateFlow<AuthSession?> = _session.asStateFlow()

    init {
        restore()
    }

    val isAuthenticated: Boolean get() = _session.value != null

    /** 앱 시작 시 저장소에 남아 있는 유효한 세션을 복원한다. */
    fun restore() {
        val restored = runCatching {
            store.load(STORAGE_KEY)?.let { json.decodeFromString<AuthSession>(it.decodeToString()) }
        }.getOrNull()
        if (restored == null || restored.token.isEmpty()) {
            runCatching { store.delete(STORAGE_KEY) }
            _session.value = null
            return
        }
        _session.value = restored
    }

    /** 인증 성공 시(API 또는 테스트 주입) 세션을 저장소에 기록하고 활성화한다. */
    fun establish(newSession: AuthSession) {
        if (newSession.token.isEmpty()) throw AuthError.InvalidSession
        val data = json.encodeToString(AuthSession.serializer(), newSession).encodeToByteArray()
        store.save(data, STORAGE_KEY)
        _session.value = newSession
    }

    /** 저장소의 세션을 제거하고 로그인 화면으로 되돌린다. */
    fun logout() {
        runCatching { store.delete(STORAGE_KEY) }.getOrElse { return }
        _session.value = null
    }

    companion object {
        const val STORAGE_KEY = "auth-session"
        private val json = Json { ignoreUnknownKeys = true }
    }
}
