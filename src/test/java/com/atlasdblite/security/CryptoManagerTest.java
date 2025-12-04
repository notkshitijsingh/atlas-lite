package com.atlasdblite.security;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.File;

public class CryptoManagerTest {

    private CryptoManager crypto;
    private static final String TEST_KEY_FILE = "atlas.key";

    @BeforeClass
    public void setup() {
        crypto = new CryptoManager();
    }

    @Test
    public void testKeyGeneration() {
        File keyFile = new File(TEST_KEY_FILE);
        Assert.assertTrue(keyFile.exists(), "Key file should be generated on initialization");
    }

    @Test
    public void testEncryptionDecryption() {
        String originalText = "TopSecretData_12345";
        
        try {
            String encrypted = crypto.encrypt(originalText);
            Assert.assertNotEquals(encrypted, originalText, "Encrypted text should not match original");
            
            String decrypted = crypto.decrypt(encrypted);
            Assert.assertEquals(decrypted, originalText, "Decrypted text must match original");
        } catch (Exception e) {
            Assert.fail("Crypto operation failed: " + e.getMessage());
        }
    }

    @Test
    public void testComplexJsonEncryption() {
        String jsonPayload = "{\"nodes\":[{\"id\":\"1\",\"label\":\"User\"}]}";
        try {
            String encrypted = crypto.encrypt(jsonPayload);
            String decrypted = crypto.decrypt(encrypted);
            Assert.assertEquals(decrypted, jsonPayload);
        } catch (Exception e) {
            Assert.fail("JSON encryption failed");
        }
    }
}