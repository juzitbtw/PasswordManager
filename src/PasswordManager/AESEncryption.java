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
        if (field == null || field.isEmpty()) {
            throw new IllegalArgumentException("Поле не должно быть пустым.");
        }

        try {
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
        } catch (Exception e) {
            System.err.println("Ошибка шифрования поля: " + e.getMessage());
            throw e;
        }
    }

    public static String decryptField(byte[] encryptionKey, String encryptedField) throws Exception {
        if (encryptedField == null || encryptedField.isEmpty()) {
            throw new IllegalArgumentException("Зашифрованное поле не должно быть пустым.");
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedField);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[Constants.IV_LENGTH];
            if (buffer.remaining() < Constants.IV_LENGTH) {
                throw new IllegalArgumentException("Недостаточно данных для извлечения IV.");
            }
            buffer.get(iv);

            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(Constants.GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), spec);

            byte[] decryptedBytes = cipher.doFinal(cipherText);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Ошибка дешифрования поля: " + e.getMessage());
            throw e;
        }
    }

    public static byte[] encryptKey(String masterPassword, byte[] key) throws Exception {
        // Генерируем IV
        byte[] iv = new byte[Constants.IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        // Генерируем salt
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        // Генерируем ключ из мастер-пароля
        byte[] derivedKey = KeyDeriver.deriveKey(masterPassword, salt, Constants.ITERATIONS, Constants.KEY_SIZE);

        // Инициализируем шифр AES/GCM
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(Constants.GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(derivedKey, "AES"), spec);

        // Шифруем ключ
        byte[] encrypted = cipher.doFinal(key);

        // Объединяем IV и зашифрованный ключ
        byte[] combined = new byte[iv.length + salt.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(salt, 0, combined, iv.length, salt.length);
        System.arraycopy(encrypted, 0, combined, iv.length + salt.length, encrypted.length);

        return combined;
    }

    public static byte[] decryptKey(String masterPassword, byte[] encryptedKey) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(encryptedKey);
        byte[] iv = new byte[Constants.IV_LENGTH];
        buffer.get(iv);

        byte[] salt = new byte[16];
        buffer.get(salt);

        byte[] cipherText = new byte[buffer.remaining()];
        buffer.get(cipherText);

        // Генерируем ключ из мастер-пароля
        byte[] derivedKey = KeyDeriver.deriveKey(masterPassword, salt, Constants.ITERATIONS, Constants.KEY_SIZE);

        // Инициализируем шифр AES/GCM
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(Constants.GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(derivedKey, "AES"), spec);

        return cipher.doFinal(cipherText);
    }

    public static String encryptWithUniqueKeys(String masterPassword, String place, String login, String password) throws Exception {
        // Генерируем первый AES-ключ (для полей)
        byte[] aesKey = new byte[32];
        new SecureRandom().nextBytes(aesKey);

        // Шифруем поля
        String encryptedPlace = encryptField(aesKey, place);
        String encryptedLogin = encryptField(aesKey, login);
        String encryptedPassword = encryptField(aesKey, password);

        // Добавляем хэши
        String hashPlace = encryptField(aesKey, PasswordEntry.hash(place, "SHA3-256"));
        String hashLogin = encryptField(aesKey, PasswordEntry.hash(login, "SHA3-256"));
        String hashPassword = encryptField(aesKey, PasswordEntry.hash(password, "SHA3-256"));

        // Объединяем поля и хэши в одну строку
        String combinedFields = String.join(",", encryptedPlace, encryptedLogin, encryptedPassword, hashPlace, hashLogin, hashPassword);

        // Генерируем второй AES-ключ (для всей строки)
        byte[] stringKey = new byte[32];
        new SecureRandom().nextBytes(stringKey);

        // Шифруем всю строку
        String encryptedFullData = encryptField(stringKey, combinedFields);

        // Шифруем оба ключа мастер-паролем
        byte[] encryptedAesKey = encryptKey(masterPassword, aesKey);
        byte[] encryptedStringKey = encryptKey(masterPassword, stringKey);

        return Base64.getEncoder().encodeToString(encryptedFullData.getBytes(StandardCharsets.UTF_8)) + "," +
                Base64.getEncoder().encodeToString(encryptedAesKey) + "," +
                Base64.getEncoder().encodeToString(encryptedStringKey);
    }

    public static String decryptWithUniqueKeys(String masterPassword, String encryptedLine) throws Exception {
        String[] parts = encryptedLine.split(",");
        if (parts.length != 3) {
            throw new Exception("Некорректный формат строки.");
        }

        byte[] encryptedFullData = Base64.getDecoder().decode(parts[0]);
        byte[] encryptedAesKey = Base64.getDecoder().decode(parts[1]);
        byte[] encryptedStringKey = Base64.getDecoder().decode(parts[2]);

        // Расшифровываем ключи
        byte[] aesKey = decryptKey(masterPassword, encryptedAesKey);
        byte[] stringKey = decryptKey(masterPassword, encryptedStringKey);

        // Расшифровываем всю строку
        String decodedEncryptedFullData = new String(encryptedFullData, StandardCharsets.UTF_8);
        String decryptedFullData = decryptField(stringKey, decodedEncryptedFullData);

        // Разделяем на поля и хэши
        String[] fields = decryptedFullData.split(",");
        if (fields.length < 6) {
            throw new Exception("Некорректный формат расшифрованных данных.");
        }

        String place = decryptField(aesKey, fields[0]);
        String login = decryptField(aesKey, fields[1]);
        String password = decryptField(aesKey, fields[2]);

        String hashPlace = decryptField(aesKey, fields[3]);
        String hashLogin = decryptField(aesKey, fields[4]);
        String hashPassword = decryptField(aesKey, fields[5]);

        // Проверка хэшей
        if (!hashPlace.equals(PasswordEntry.hash(place, "SHA3-256"))) {
            throw new Exception("Хэш места не совпадает.");
        }
        if (!hashLogin.equals(PasswordEntry.hash(login, "SHA3-256"))) {
            throw new Exception("Хэш логина не совпадает.");
        }
        if (!hashPassword.equals(PasswordEntry.hash(password, "SHA3-256"))) {
            throw new Exception("Хэш пароля не совпадает.");
        }

        return String.join(",", place, login, password);
    }
}