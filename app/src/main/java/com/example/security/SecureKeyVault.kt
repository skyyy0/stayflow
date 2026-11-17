package com.example.security

import com.example.BuildConfig
import java.security.MessageDigest
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Runtime cryptographic key unsealer.
 * Keys are encrypted at build time with AES-256-GCM + split XOR shards so
 * no plaintext API secret is stored in BuildConfig or classes.dex.
 */
object SecureKeyVault {

    private const val VAULT_DOMAIN_TAG = "StayFlow::Sky::KeyVault::v1"
    private const val PLACEHOLDER_PREFIX = "MY_"

    @Volatile
    private var cachedApiKey: String? = null

    fun resolveXingchiApiKey(): String {
        cachedApiKey?.let { return it }

        val decrypted = unsealKey(
            cipherHex = BuildConfig.VAULT_CIPHER_HEX,
            ivHex = BuildConfig.VAULT_IV_HEX,
            shardAHex = BuildConfig.VAULT_SHARD_A_HEX,
            shardBHex = BuildConfig.VAULT_SHARD_B_HEX,
        )

        if (decrypted.isNotEmpty() && !decrypted.startsWith(PLACEHOLDER_PREFIX)) {
            cachedApiKey = decrypted
            return decrypted
        }
        return ""
    }

    internal fun unsealKey(
        cipherHex: String,
        ivHex: String,
        shardAHex: String,
        shardBHex: String,
    ): String {
        if (cipherHex.isBlank() || ivHex.isBlank() || shardAHex.isBlank() || shardBHex.isBlank()) {
            return ""
        }

        val shardA = decodeHex(shardAHex)
        val shardB = decodeHex(shardBHex)
        val iv = decodeHex(ivHex)
        val cipherBytes = decodeHex(cipherHex)

        if (shardA.size != 32 || shardB.size != 32 || iv.size != 12 || cipherBytes.isEmpty()) {
            return ""
        }

        val combined = ByteArray(32)
        val aesKeyBytes = ByteArray(32)
        return try {
            for (i in 0 until 32) {
                combined[i] = (shardA[i].toInt() xor shardB[i].toInt()).toByte()
            }
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(combined)
            digest.update(VAULT_DOMAIN_TAG.toByteArray(Charsets.UTF_8))
            val derived = digest.digest()
            System.arraycopy(derived, 0, aesKeyBytes, 0, 32)
            Arrays.fill(derived, 0.toByte())

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val secretKey = SecretKeySpec(aesKeyBytes, "AES")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val plainBytes = cipher.doFinal(cipherBytes)
            val result = String(plainBytes, Charsets.UTF_8).trim()
            Arrays.fill(plainBytes, 0.toByte())
            result
        } catch (_: Exception) {
            ""
        } finally {
            Arrays.fill(shardA, 0.toByte())
            Arrays.fill(shardB, 0.toByte())
            Arrays.fill(combined, 0.toByte())
            Arrays.fill(aesKeyBytes, 0.toByte())
        }
    }

    internal fun decodeHex(hex: String): ByteArray {
        val clean = hex.trim()
        if (clean.length % 2 != 0) return ByteArray(0)
        return try {
            ByteArray(clean.length / 2) { index ->
                val hi = Character.digit(clean[index * 2], 16)
                val lo = Character.digit(clean[index * 2 + 1], 16)
                if (hi == -1 || lo == -1) return ByteArray(0)
                ((hi shl 4) or lo).toByte()
            }
        } catch (_: Exception) {
            ByteArray(0)
        }
    }
}
