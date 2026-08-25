package com.htoms.brief.auth

/**
 * 인증 서비스 인터페이스.
 */
interface AuthServicing {
    suspend fun authenticate(username: String, password: String): AuthSession
}
