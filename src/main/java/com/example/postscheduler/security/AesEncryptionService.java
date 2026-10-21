package com.example.postscheduler.security;

import com.example.postscheduler.exception.CryptoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256 in GCM mode.
 *  - A fresh random 12-byte IV is generated for every encryption, so encrypting the same text twice
 *    gives two different ciphertexts.
 *  - GCM also adds an authentication tag, so any change to the stored ciphertext is detected on decrypt.
 *  - Stored format:  v1:Base64( IV || ciphertext || tag ). The "v1" prefix allows the key or algorithm
 *    to be changed in future (key rotation) while still recognising old data.
 * The key comes from configuration (environment variable ENCRYPTION_KEY), never from the database.
 */
@Service
public class AesEncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final String VERSION_PREFIX = "v1:";

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesEncryptionService(@Value("${app.crypto.secret-key}") String base64Key) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("app.crypto.secret-key must be a Base64 string");
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "app.crypto.secret-key must decode to exactly 32 bytes (AES-256), but was " + keyBytes.length);
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv).put(cipherText);
            return VERSION_PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException ex) {
            throw new CryptoException("Encryption failed", ex);
        }
    }

    public String decrypt(String stored) {
        if (stored == null || !stored.startsWith(VERSION_PREFIX)) {
            throw new CryptoException("Unrecognised ciphertext format", null);
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(VERSION_PREFIX.length()));
            if (all.length <= IV_LENGTH_BYTES) {
                throw new CryptoException("Ciphertext is too short", null);
            }
            ByteBuffer buffer = ByteBuffer.wrap(all);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            // Wrong key, or the data was corrupted / tampered with (GCM tag mismatch)
            throw new CryptoException("Decryption failed", ex);
        }
    }
}
