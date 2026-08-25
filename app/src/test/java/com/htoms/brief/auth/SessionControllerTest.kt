package com.htoms.brief.auth

import com.htoms.brief.security.SecureStoring
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** 실제 Keystore를 건드리지 않는 메모리 기반 SecureStoring 대역. */
class InMemorySecureStore : SecureStoring {
    val storage = mutableMapOf<String, ByteArray>()

    override fun save(data: ByteArray, key: String) {
        storage[key] = data
    }

    override fun load(key: String): ByteArray? = storage[key]

    override fun delete(key: String) {
        storage.remove(key)
    }
}

/** iOS SessionControllerTests 대응: 세션 수립·복원·해제 상태 전이. */
class SessionControllerTest {

    private val sampleSession = AuthSession(
        token = "test-token-123",
        username = "tester",
        issuedAt = 1_700_000_000_000L
    )

    @Test
    fun coldLaunchWithoutStoredSessionIsLoggedOut() {
        val controller = SessionController(InMemorySecureStore())
        assertFalse(controller.isAuthenticated)
        assertNull(controller.session.value)
    }

    @Test
    fun establishPersistsSessionInSecureStore() {
        val store = InMemorySecureStore()
        val controller = SessionController(store)

        controller.establish(sampleSession)

        assertTrue(controller.isAuthenticated)
        assertEquals(sampleSession, controller.session.value)
        val stored = store.storage[SessionController.STORAGE_KEY]!!
        val decoded = Json.decodeFromString<AuthSession>(stored.decodeToString())
        assertEquals(sampleSession, decoded)
    }

    @Test
    fun sessionIsRestoredOnNextLaunchFromSameStore() {
        val store = InMemorySecureStore()
        SessionController(store).establish(sampleSession)

        // 같은 저장소로 새 컨트롤러를 만들면(다음 실행) 세션이 복원돼야 한다.
        val relaunched = SessionController(store)
        assertTrue(relaunched.isAuthenticated)
        assertEquals(sampleSession, relaunched.session.value)
    }

    @Test
    fun logoutClearsSessionAndStore() {
        val store = InMemorySecureStore()
        val controller = SessionController(store)
        controller.establish(sampleSession)

        controller.logout()

        assertFalse(controller.isAuthenticated)
        assertNull(store.storage[SessionController.STORAGE_KEY])
        assertFalse(SessionController(store).isAuthenticated)
    }

    @Test
    fun corruptedStoredSessionFallsBackToLoggedOut() {
        val store = InMemorySecureStore()
        store.save("not-json".encodeToByteArray(), SessionController.STORAGE_KEY)

        val controller = SessionController(store)
        assertFalse(controller.isAuthenticated)
        assertNull(store.storage[SessionController.STORAGE_KEY])
    }

    @Test
    fun emptyTokenCannotEstablishSession() {
        val controller = SessionController(InMemorySecureStore())
        val invalid = AuthSession(token = "", username = "tester", issuedAt = 0L)

        try {
            controller.establish(invalid)
            fail("에러가 발생해야 합니다")
        } catch (error: AuthError.InvalidSession) {
            assertEquals("유효한 인증 세션을 만들 수 없습니다.", error.message)
        }
        assertFalse(controller.isAuthenticated)
    }

    @Test
    fun unconnectedAuthServiceNeverAuthenticates() = runTest {
        val service = UnconnectedAuthService()

        try {
            service.authenticate(username = "", password = "")
            fail("에러가 발생해야 합니다")
        } catch (error: AuthError.EmptyCredentials) {
            assertEquals("아이디와 비밀번호를 모두 입력해 주세요.", error.message)
        }

        try {
            service.authenticate(username = "anyone", password = "anything")
            fail("에러가 발생해야 합니다")
        } catch (error: AuthError.ServerNotConnected) {
            assertEquals(
                "서버 인증이 아직 연결되지 않았습니다. OMS 인증 API 연동 후 로그인할 수 있습니다.",
                error.message
            )
        }
    }
}
