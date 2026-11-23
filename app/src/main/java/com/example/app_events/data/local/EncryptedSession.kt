package com.example.app_events.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.app_events.data.remote.CredentialsDto
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SessionId(val value: UUID = UUID.randomUUID())

class SessionTokens(
    val id: SessionId,
    val access: String,
    val refresh: String
)

@Singleton
class EncryptedSession @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("evently_session", Context.MODE_PRIVATE)
    private val current = MutableStateFlow<SessionTokens?>(null)
    val tokens = current.asStateFlow()

    @Synchronized
    fun read(): SessionTokens? {
        val stored = prefs.getString("credentials", null) ?: return null
        return try {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, 12))
            val parts = cipher.doFinal(bytes, 12, bytes.size - 12).decodeToString().split('\n', limit = 2)
            SessionTokens(SessionId(), parts[0], parts[1])
        } catch (_: Exception) {
            clear()
            null
        }
    }

    @Synchronized
    fun restore(tokens: SessionTokens) {
        current.value = tokens
    }

    @Synchronized
    fun start(credentials: CredentialsDto) = save(SessionId(), credentials)

    @Synchronized
    fun rotate(previous: SessionTokens, credentials: CredentialsDto): SessionTokens? {
        if (current.value !== previous) return null
        return save(previous.id, credentials)
    }

    @Synchronized
    fun clear(): SessionTokens? {
        check(prefs.edit().clear().commit())
        val previous = current.value
        current.value = null
        return previous
    }

    private fun save(id: SessionId, credentials: CredentialsDto): SessionTokens {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val payload = (credentials.accessToken + "\n" + credentials.refreshToken).encodeToByteArray()
        val encrypted = Base64.encodeToString(cipher.iv + cipher.doFinal(payload), Base64.NO_WRAP)
        check(prefs.edit().putString("credentials", encrypted).commit())
        return SessionTokens(id, credentials.accessToken, credentials.refreshToken).also { current.value = it }
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = "evently_session_aes"
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val spec = KeyGenParameterSpec.Builder(
            alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply { init(spec) }.generateKey()
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
