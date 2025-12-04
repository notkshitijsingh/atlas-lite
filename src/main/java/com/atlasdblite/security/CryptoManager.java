package com.atlasdblite.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Manages encryption and decryption for the database.
 * This class handles the loading or generation of a secret key and provides
 * simple methods for encrypting and decrypting data using AES.
 */
public class CryptoManager {
    private static final String ALGORITHM = "AES";
    private static final String KEY_FILE = "atlas.key";
    private SecretKey secretKey;

    /**
     * Initializes the CryptoManager by loading an existing key or generating a new one.
     */
    public CryptoManager() {
        try {
            this.secretKey = loadOrGenerateKey();
        } catch (Exception e) {
            // If the security layer fails to initialize, it's a critical failure.
            throw new RuntimeException("FATAL: Failed to initialize security layer. " + e.getMessage());
        }
    }

    /**
     * Encrypts a plain-text string.
     * @param data The string to encrypt.
     * @return A Base64-encoded representation of the encrypted data.
     * @throws Exception If the encryption process fails.
     */
    public String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Decrypts a Base64-encoded string.
     * @param encryptedData The encrypted, Base64-encoded string.
     * @return The original plain-text string.
     * @throws Exception If the decryption process fails.
     */
    public String decrypt(String encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
        return new String(cipher.doFinal(decodedBytes));
    }

    /**
     * Loads the secret key from the {@code atlas.key} file if it exists.
     * If not, it generates a new 256-bit AES key and saves it to the file for future use.
     * @return The loaded or newly generated {@link SecretKey}.
     * @throws Exception If key loading or generation fails.
     */
    private SecretKey loadOrGenerateKey() throws Exception {
        File keyFile = new File(KEY_FILE);
        if (keyFile.exists()) {
            // Load existing key from file.
            byte[] encodedKey = Files.readAllBytes(Paths.get(KEY_FILE));
            return new SecretKeySpec(encodedKey, ALGORITHM);
        } else {
            // Generate a new key if one doesn't exist.
            System.out.println(" [SECURITY] No key file found. Generating new AES-256 encryption key...");
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(256); // 256-bit AES
            SecretKey key = keyGen.generateKey();
            // Save the new key to the file so it can be reused.
            Files.write(Paths.get(KEY_FILE), key.getEncoded());
            return key;
        }
    }
}