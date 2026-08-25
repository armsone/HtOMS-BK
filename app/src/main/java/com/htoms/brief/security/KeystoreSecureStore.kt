package com.htoms.brief.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore(AES/GCM) 기반 암호화 저장소 구현.
 * 키는 하드웨어 지원 Keystore에만 존재하며 앱 밖으로 추출되지 않는다.
 * 암호문(IV 포함)은 앱 전용 SharedPreferences 파일에 보관하고,
 * 비보호 평문 저장소는 사용하지 않는다. 값은 로그로 남기지 않는다.
 */
class KeystoreSecureStore(
    context: Context,
    private val prefFileName: String = "htoms_secure_prefs",
    private val keyAlias: String = "htoms-brief-secure-store"
) : SecureStoring {

    private val sharedPreferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(prefFileName, Context.MODE_PRIVATE)

    override fun save(data: ByteArray, key: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, obtainSecretKey())
        val ciphertext = cipher.doFinal(data)
        val payload = cipher.iv + ciphertext
        sharedPreferences.edit()
            .putString(key, Base64.encodeToString(payload, Base64.NO_WRAP))
            .apply()
    }

    override fun load(key: String): ByteArray? {
        val base64 = sharedPreferences.getString(key, null) ?: return null
        return try {
            val payload = Base64.decode(base64, Base64.NO_WRAP)
            if (payload.size <= IV_LENGTH_BYTES) return null
            val iv = payload.copyOfRange(0, IV_LENGTH_BYTES)
            val ciphertext = payload.copyOfRange(IV_LENGTH_BYTES, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, obtainSecretKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            cipher.doFinal(ciphertext)
        } catch (_: Exception) {
            null
        }
    }

    override fun delete(key: String) {
        sharedPreferences.edit()
            .remove(key)
            .apply()
    }

    private fun obtainSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH_BYTES = 12
        const val TAG_LENGTH_BITS = 128
    }
}
