package main.java.PasswordManager;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;

public class KeyDeriver {
    public static byte[] generateSalt(int length) {
        byte[] salt = new byte[length];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public static byte[] deriveKey(String password, byte[] salt, int iterations, int keyLength) throws Exception {
        if (salt == null) {
            throw new IllegalArgumentException("Salt must not be null");
        }

        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
        // Используем PBKDF2 с HMAC-SHA3-512 через Bouncy Castle
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA3-512", "BC");
        return skf.generateSecret(spec).getEncoded();
    }
}