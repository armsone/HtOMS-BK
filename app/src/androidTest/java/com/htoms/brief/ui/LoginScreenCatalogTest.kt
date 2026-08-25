package com.htoms.brief.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.htoms.brief.auth.SessionController
import com.htoms.brief.auth.UnconnectedAuthService
import com.htoms.brief.security.SecureStoring
import com.htoms.brief.theme.HtOMSBriefTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UnconnectedAuthService 기반 결정적 로그인 카탈로그 상태 검증(네트워크 없음). */
@RunWith(AndroidJUnit4::class)
class LoginScreenCatalogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class InMemoryStore : SecureStoring {
        private val storage = mutableMapOf<String, ByteArray>()
        override fun save(data: ByteArray, key: String) { storage[key] = data }
        override fun load(key: String): ByteArray? = storage[key]
        override fun delete(key: String) { storage.remove(key) }
    }

    private fun setLoginContent() {
        composeRule.setContent {
            HtOMSBriefTheme {
                LoginScreen(
                    controller = SessionController(InMemoryStore()),
                    authService = UnconnectedAuthService()
                )
            }
        }
    }

    @Test
    fun rendersSplitFlapHeaderStatusBoardAndCredentialFields() {
        setLoginContent()
        composeRule.onNodeWithText("운영 브리핑 정보 보드").assertIsDisplayed()
        composeRule.onNodeWithTag("login-status-board").assertIsDisplayed()
        composeRule.onNodeWithTag("login-email").assertIsDisplayed()
        composeRule.onNodeWithTag("login-password").assertIsDisplayed()
        composeRule.onNodeWithTag("login-submit").assertIsDisplayed()
        composeRule.onNodeWithText("사내 계정 인증").assertIsDisplayed()
        composeRule.onNodeWithText("이 기기 Keystore 보관").assertIsDisplayed()
    }

    @Test
    fun submitWithUnconnectedServiceShowsErrorRowAndKeepsUsername() {
        setLoginContent()
        composeRule.onNodeWithTag("login-email").performTextInput("ceo@example.com")
        composeRule.onNodeWithTag("login-password").performTextInput("secret")
        composeRule.onNodeWithTag("login-submit").performClick()

        composeRule.onNodeWithTag("login-error").assertIsDisplayed()
        composeRule
            .onNodeWithText("서버 인증이 아직 연결되지 않았습니다. OMS 인증 API 연동 후 로그인할 수 있습니다.")
            .assertIsDisplayed()
    }
}
