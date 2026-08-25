package com.htoms.brief.security

/**
 * 세션 토큰 등 비밀값 저장을 추상화한 인터페이스.
 * 실제 앱은 Android Keystore 기반 EncryptedSharedPreferences를 쓰고,
 * 테스트는 메모리 대역을 주입한다.
 */
interface SecureStoring {
    fun save(data: ByteArray, key: String)
    fun load(key: String): ByteArray?
    fun delete(key: String)
}
