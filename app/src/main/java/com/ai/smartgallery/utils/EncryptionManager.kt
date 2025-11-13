package com.ai.smartgallery.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encryption manager for securing vault photos and sensitive data
 * Uses AES-256-GCM encryption with Android Keystore
 */
@Singleton
class EncryptionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val VAULT_KEY_ALIAS = "smart_gallery_vault_key"
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    init {
        // Generate encryption key if it doesn't exist
        if (!keyStore.containsAlias(VAULT_KEY_ALIAS)) {
            generateEncryptionKey()
        }
    }

    /**
     * Generate AES-256 encryption key in Android Keystore
     */
    private fun generateEncryptionKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            VAULT_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false) // Can require biometric auth
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    /**
     * Get encryption key from keystore
     */
    private fun getKey(): SecretKey {
        return keyStore.getKey(VAULT_KEY_ALIAS, null) as SecretKey
    }

    /**
     * Encrypt a file
     * @param inputFile Source file
     * @param outputFile Encrypted output file
     */
    fun encryptFile(inputFile: File, outputFile: File) {
        try {
            val cipher = Cipher.getInstance(AES_MODE)
            cipher.init(Cipher.ENCRYPT_MODE, getKey())

            val iv = cipher.iv

            FileOutputStream(outputFile).use { fos ->
                // Write IV to beginning of file
                fos.write(iv.size)
                fos.write(iv)

                FileInputStream(inputFile).use { fis ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        val encrypted = cipher.update(buffer, 0, bytesRead)
                        if (encrypted != null) {
                            fos.write(encrypted)
                        }
                    }

                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null) {
                        fos.write(finalBytes)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw SecurityException("Failed to encrypt file", e)
        }
    }

    /**
     * Decrypt a file
     * @param inputFile Encrypted file
     * @param outputFile Decrypted output file
     */
    fun decryptFile(inputFile: File, outputFile: File) {
        try {
            FileInputStream(inputFile).use { fis ->
                // Read IV from beginning of file
                val ivSize = fis.read()
                val iv = ByteArray(ivSize)
                fis.read(iv)

                val cipher = Cipher.getInstance(AES_MODE)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)

                FileOutputStream(outputFile).use { fos ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        val decrypted = cipher.update(buffer, 0, bytesRead)
                        if (decrypted != null) {
                            fos.write(decrypted)
                        }
                    }

                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null) {
                        fos.write(finalBytes)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw SecurityException("Failed to decrypt file", e)
        }
    }

    /**
     * Securely delete a file by overwriting with random data
     */
    fun secureDelete(file: File) {
        try {
            if (file.exists()) {
                val length = file.length()
                val randomData = ByteArray(8192)

                FileOutputStream(file).use { fos ->
                    var remaining = length
                    while (remaining > 0) {
                        val toWrite = minOf(remaining, randomData.size.toLong()).toInt()
                        java.security.SecureRandom().nextBytes(randomData)
                        fos.write(randomData, 0, toWrite)
                        remaining -= toWrite
                    }
                }

                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            file.delete() // Fallback to regular delete
        }
    }

    /**
     * Check if encryption key exists
     */
    fun hasEncryptionKey(): Boolean {
        return keyStore.containsAlias(VAULT_KEY_ALIAS)
    }

    /**
     * Delete encryption key (use with caution)
     */
    fun deleteEncryptionKey() {
        keyStore.deleteEntry(VAULT_KEY_ALIAS)
    }
}
