/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto.encryption

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
class EncryptionHelper() {

    companion object {
        const val ITERATIONS = 943786
        const val ENCRYPTION_ALG = "AES/CBC/PKCS5Padding"
        const val KEY_TYPE = "PBKDF2WithHmacSHA256"
        const val KEY_LENGTH = 128
        const val IV_LENGTH = 16
    }

    private var key: SecretKey? = null

    init {
        /*
        val savedKey = sharedPrefs.getString(PREFERENCE_KEY, null)
        if (savedKey != null) {
            val keyBytes = savedKey.fromHex()
            this.key = SecretKeySpec(keyBytes, ENCRYPTION_ALG)
        }
        */
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
            /*
            val editor = sharedPrefs.edit()
            editor.putString(PREFERENCE_KEY, key.encoded.toHex())
            editor.apply()
            */

            // Set the key
            this.key = key
        }
    }

    fun getKey () = key?.encoded?.toHex()

    /**
     * Encrypted the given plaintext with the current key in the following format:
     * 16-byte IV + ciphertext
     */
    fun encrypt(plaintext: String): String {
        val encodedKey = this.key?.encoded ?: throw KeyNotSetException()
        val keySpec = SecretKeySpec(encodedKey, "AES")

        val iv = generateIv()
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(ENCRYPTION_ALG)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

        val cipherText = cipher.doFinal(plaintext.toByteArray(Charset.forName("UTF-8")))
        return iv.toHex() + cipherText.toHex()
    }

    /**
     * Decrypts the ciphertext in the following format with the current key
     * 16-byte IV + ciphertext
     */
    fun decrypt(ciphertext: String): String {
        val encodedKey = this.key?.encoded ?: throw KeyNotSetException()
        val keySpec = SecretKeySpec(encodedKey, "AES")

        // Split ivBytes from cipherBytes
        val ivBytes = ciphertext.substring(0, IV_LENGTH * 2).fromHex()
        val cipherBytes = ciphertext.substring(IV_LENGTH * 2).fromHex()

        val ivSpec = IvParameterSpec(ivBytes)
        val cipher = Cipher.getInstance(ENCRYPTION_ALG)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

        val plainBytes = cipher.doFinal(cipherBytes)
        return plainBytes.toString(Charset.forName("UTF-8"))
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