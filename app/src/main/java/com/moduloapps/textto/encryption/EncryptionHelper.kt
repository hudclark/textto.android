/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto.encryption

import com.moduloapps.textto.persistance.Persistence
import com.moduloapps.textto.utils.fromBase64
import com.moduloapps.textto.utils.toBase64
import java.nio.charset.Charset
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Created by hudson on 3/7/18.
 */
class EncryptionHelper(private val persistence: Persistence) {

    companion object {
        const val ITERATIONS = 243786
        const val ENCRYPTION_ALG = "AES/CBC/PKCS5Padding"
        const val KEY_TYPE = "PBKDF2WithHmacSHA1"
        const val KEY_LENGTH = 128
        const val IV_LENGTH = 16

        const val PREFERENCE_KEY = "eKey"
    }

    private var keySpec: SecretKey? = null

    init {
        val savedKey = persistence.getString(PREFERENCE_KEY, null)
        if (savedKey != null) {
            val keyBytes = savedKey.fromHex()
            this.keySpec = SecretKeySpec(keyBytes, ENCRYPTION_ALG)
        }
        else keySpec = null
    }

    fun enabled() = this.keySpec != null

    fun disable() {
        synchronized(this) {
            this.keySpec = null
            persistence.putString(PREFERENCE_KEY, null)
        }
    }

    /**
     * Must be called in a background thread
     */
    fun setPassword(password: String, salt: String) {
        synchronized(this) {
            val keySpec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), ITERATIONS, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance(KEY_TYPE)
            val key = factory.generateSecret(keySpec)

            // Save the key
            persistence.putString(PREFERENCE_KEY, key.encoded.toHex())

            // set the key
            this.keySpec = SecretKeySpec(key.encoded, "AES")
        }
    }

    fun getKey () = keySpec?.encoded?.toHex()

    /**
     * Encrypted the given plaintext with the current key in the following format:
     * 16-byte IV + ciphertext
     */
    fun encrypt(plaintext: String): String {
        val cipherText = encrypt(plaintext.toByteArray(Charset.forName("UTF-8")))
        return cipherText.toBase64()
    }

    fun encrypt(bytes: ByteArray): ByteArray {
        if (this.keySpec == null) throw KeyNotSetException()

        val iv = generateIv()
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(ENCRYPTION_ALG)
        cipher.init(Cipher.ENCRYPT_MODE, this.keySpec, ivSpec)

        val cipherText = cipher.doFinal(bytes)

        return (iv + cipherText)
    }

    /**
     * Decrypts the ciphertext in the following format with the current key
     * 16-byte IV + ciphertext
     */
    fun decrypt(ciphertext: String): String {
        val bytes = ciphertext.fromBase64()
        val plainBytes = decrypt(bytes)
        return plainBytes.toString(Charset.forName("UTF-8"))
    }

    fun decrypt(bytes: ByteArray): ByteArray {
        if (this.keySpec == null) throw KeyNotSetException()

        // Split ivBytes from cipherBytes
        val ivBytes = bytes.copyOfRange(0, IV_LENGTH)
        val cipherBytes = bytes.copyOfRange(IV_LENGTH, bytes.size)

        val ivSpec = IvParameterSpec(ivBytes)
        val cipher = Cipher.getInstance(ENCRYPTION_ALG)
        cipher.init(Cipher.DECRYPT_MODE, this.keySpec, ivSpec)

        return cipher.doFinal(cipherBytes)

    }

    /**
     * Generate a new 16-byte IV
     */
    private fun generateIv(): ByteArray {
        return SecureRandom.getSeed(16)
    }


    // TODO this might be too slow
    private fun ByteArray.toHex(): String {
        val formatter = Formatter()
        this.forEach { formatter.format("%02x", it) }
        return formatter.toString()
    }

    private fun String.fromHex(): ByteArray {
        val len = this.length
        val bytes = ByteArray(len / 2)

        var i = 0
        while (i < len) {
            bytes[i / 2] = ((Character.digit(this[i], 16) shl 4) + Character.digit(this[i + 1], 16)).toByte()
            i += 2
        }

        return bytes
    }

    class KeyNotSetException : RuntimeException("Encryption key not set")

}