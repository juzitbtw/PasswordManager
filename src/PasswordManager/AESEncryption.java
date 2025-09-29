package PasswordManager;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class AESEncryption {
    public static String encryptField(byte[] encryptionKey, String field) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[Constants.IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(Constants.GCM_TAG_LENGTH, iv);

        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), spec);
        byte[] cipherText = cipher.doFinal(field.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    public static String decryptField(byte[] encryptionKey, String encryptedField) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(encryptedField);
        ByteBuffer buffer = ByteBuffer.wrap(decoded);

        byte[] iv = new byte[Constants.IV_LENGTH];
        buffer.get(iv);
        byte[] cipherText = new byte[buffer.remaining()];
        buffer.get(cipherText);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(Constants.GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), spec);

        return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
    }

    // Объединение зашифрованных полей в одну строку
    public static String encrypt(byte[] encryptionKey, String place, String login, String password) throws Exception {
        return String.join(",",
                encryptField(encryptionKey, place),
                encryptField(encryptionKey, login),
                encryptField(encryptionKey, password)
        );
    }

    public static PasswordEntry decrypt(byte[] encryptionKey, String encryptedData) throws Exception {
        String[] parts = encryptedData.split(",");
        if (parts.length != 3) {
            throw new Exception("Некорректный формат расшифрованных данных");
        }

        String decryptedPlace = decryptField(encryptionKey, parts[0]);
        String decryptedLogin = decryptField(encryptionKey, parts[1]);
        String decryptedPassword = decryptField(encryptionKey, parts[2]);

        return new PasswordEntry(decryptedPlace, decryptedLogin, decryptedPassword);
    }
}