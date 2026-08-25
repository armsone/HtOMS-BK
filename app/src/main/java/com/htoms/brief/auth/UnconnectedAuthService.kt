package com.htoms.brief.auth

/**
 * 인증 API 연동 전 또는 UI 카탈로그 모드에서 사용하는 서비스. 어떤 입력으로도 인증되지 않으며,
 * 자격 증명을 저장하거나 로그로 남기지 않는다.
 */
class UnconnectedAuthService : AuthServicing {
    override suspend fun authenticate(username: String, password: String): AuthSession {
        if (username.isEmpty() || password.isEmpty()) {
            throw AuthError.EmptyCredentials
        }
        throw AuthError.ServerNotConnected
    }
}
