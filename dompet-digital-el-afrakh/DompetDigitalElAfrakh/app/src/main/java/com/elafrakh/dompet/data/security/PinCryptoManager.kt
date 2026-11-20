package com.elafrakh.dompet.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class HashedSecret(
    val hashBase64: String,
    val saltBase64: String
)

object PinCryptoManager {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 12_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val RECOVERY_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    private val secureRandom = SecureRandom()

    fun hashSecret(secret: String): HashedSecret {
        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)
        val hash = pbkdf2(secret.toCharArray(), salt)
        return HashedSecret(
            hashBase64 = Base64.getEncoder().encodeToString(hash),
            saltBase64 = Base64.getEncoder().encodeToString(salt)
        )
    }

    fun verifySecret(candidate: String, storedHashBase64: String?, storedSaltBase64: String?): Boolean {
        if (storedHashBase64.isNullOrBlank() || storedSaltBase64.isNullOrBlank()) return false
        return runCatching {
            val salt = Base64.getDecoder().decode(storedSaltBase64)
            val expectedHash = Base64.getDecoder().decode(storedHashBase64)
            val actualHash = pbkdf2(candidate.toCharArray(), salt)
            MessageDigest.isEqual(expectedHash, actualHash)
        }.getOrDefault(false)
    }

    fun generateRecoveryCode(): String {
        val raw = buildString(8) {
            repeat(8) {
                append(RECOVERY_CHARS[secureRandom.nextInt(RECOVERY_CHARS.length)])
            }
        }
        return "${raw.substring(0, 4)}-${raw.substring(4, 8)}"
    }

    fun normalizeRecoveryCode(input: String): String =
        input.uppercase().replace("-", "").replace(" ", "").trim()

    private fun pbkdf2(chars: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(chars, salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
