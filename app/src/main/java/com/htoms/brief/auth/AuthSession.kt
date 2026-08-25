package com.htoms.brief.auth

import kotlinx.serialization.Serializable

/**
 * 인증 성공 후 서버가 발급하는 세션. 토큰은 보안 저장소에만 보관된다.
 */
@Serializable
data class AuthSession(
    val token: String,
    val username: String,
    val issuedAt: Long
)

sealed class AuthError(override val message: String) : Exception(message) {
    data object EmptyCredentials : AuthError("아이디와 비밀번호를 모두 입력해 주세요.")
    data object InvalidSession : AuthError("유효한 인증 세션을 만들 수 없습니다.")
    data object ServerNotConnected : AuthError("서버 인증이 아직 연결되지 않았습니다. OMS 인증 API 연동 후 로그인할 수 있습니다.")
}
