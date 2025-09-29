package PasswordManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class PasswordManager {
    private List<PasswordEntry> entries;
    private byte[] salt;
    private byte[] encryptionKey;

    public PasswordManager() {
        entries = new ArrayList<>();
        salt = null;
        encryptionKey = null;
    }

    public void loadEntries(String masterPassword) throws Exception {
        if (!Files.exists(Paths.get(Constants.FILE_NAME))) {
            Files.createFile(Paths.get(Constants.FILE_NAME));
            salt = KeyDeriver.generateSalt(Constants.SALT_SIZE);
            return;
        }

        List<String> lines = Files.readAllLines(Paths.get(Constants.FILE_NAME));
        if (lines.isEmpty()) {
            salt = KeyDeriver.generateSalt(Constants.SALT_SIZE);
            return;
        }

        String saltBase64 = lines.get(0);
        salt = Base64.getDecoder().decode(saltBase64);

        this.encryptionKey = KeyDeriver.deriveKey(masterPassword, salt, Constants.ITERATIONS, Constants.KEY_SIZE);

        entries = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String encryptedLine = lines.get(i);
            PasswordEntry entry = AESEncryption.decrypt(this.encryptionKey, encryptedLine);
            entries.add(entry);
        }

        verifyIntegrity();
    }

    public void saveEntries(String masterPassword) throws Exception {
        boolean isNewFile = !Files.exists(Paths.get(Constants.FILE_NAME));

        byte[] saltToUse = isNewFile || salt == null ? KeyDeriver.generateSalt(Constants.SALT_SIZE) : salt;
        this.encryptionKey = KeyDeriver.deriveKey(masterPassword, saltToUse, Constants.ITERATIONS, Constants.KEY_SIZE);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(Constants.FILE_NAME))) {
            String saltBase64 = Base64.getEncoder().encodeToString(saltToUse);
            writer.write(saltBase64);
            writer.newLine();

            for (PasswordEntry entry : entries) {
                String encrypted = AESEncryption.encrypt(
                        this.encryptionKey,
                        entry.getPlace(),
                        entry.getLogin(),
                        entry.getPassword()
                );
                writer.write(encrypted);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка записи в файл", e);
        }

        this.salt = saltToUse;
    }

    public void reencryptWithNewMasterPassword(String currentMasterPassword, String newMasterPassword)
            throws Exception {
        String tempFile = Constants.FILE_NAME + ".tmp";
        Files.deleteIfExists(Paths.get(tempFile));

        List<String> lines = Files.readAllLines(Paths.get(Constants.FILE_NAME));
        if (lines.isEmpty()) return;

        String oldSaltBase64 = lines.get(0);
        byte[] oldSalt = Base64.getDecoder().decode(oldSaltBase64);

        byte[] oldEncryptionKey = KeyDeriver.deriveKey(currentMasterPassword, oldSalt, Constants.ITERATIONS,
                Constants.KEY_SIZE);
        byte[] newSalt = KeyDeriver.generateSalt(Constants.SALT_SIZE);
        byte[] newEncryptionKey = KeyDeriver.deriveKey(newMasterPassword, newSalt, Constants.ITERATIONS,
                Constants.KEY_SIZE);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write(Base64.getEncoder().encodeToString(newSalt));
            writer.newLine();

            for (int i = 1; i < lines.size(); i++) {
                String encryptedLine = lines.get(i);
                PasswordEntry entry = AESEncryption.decrypt(
                        oldEncryptionKey,
                        encryptedLine
                );
                String newEncrypted = AESEncryption.encrypt(newEncryptionKey,
                        entry.getPlace(),
                        entry.getLogin(),
                        entry.getPassword()
                );
                writer.write(newEncrypted);
                writer.newLine();
            }
        }

        Files.move(Paths.get(tempFile), Paths.get(Constants.FILE_NAME), StandardCopyOption.REPLACE_EXISTING);

        entries.clear();
        salt = newSalt;
        encryptionKey = newEncryptionKey;
    }

    public void addEntry(String place, String login, String password) {
        entries.add(new PasswordEntry(place, login, password));
    }

    public void removeEntry(int index) {
        if (index >= 0 && index < entries.size()) {
            entries.remove(index);
        }
    }

    public PasswordEntry getEntry(int index) {
        return (index >= 0 && index < entries.size()) ? entries.get(index) : null;
    }

    public void displayEntries() {
        for (int i = 0; i < entries.size(); i++) {
            System.out.println(i + ": " + entries.get(i).getPlace() + " - " + entries.get(i).getLogin());
        }
    }

    public void updateEntry(int index, String newPlace, String newLogin, String newPassword) {
        if (index < 0 || index >= entries.size()) {
            throw new IllegalArgumentException("Неверный индекс: " + index);
        }

        PasswordEntry editEntry = entries.get(index);

        String place = newPlace.isEmpty() ? editEntry.getPlace() : newPlace;
        String login = newLogin.isEmpty() ? editEntry.getLogin() : newLogin;
        String password = newPassword.isEmpty() ? editEntry.getPassword() : newPassword;

        entries.set(index, new PasswordEntry(place, login, password));
    }

    public void verifyIntegrity() {
        boolean hasErrors = false;

        for (PasswordEntry entry : entries) {
            String currentPlaceHash = hash(entry.getPlace());
            String currentLoginHash = hash(entry.getLogin());
            String currentPasswordHash = hash(entry.getPassword());

            if (!entry.getHashPlace().equals(currentPlaceHash)) {
                System.err.println("Хэш места не совпадает для записи: " + entry.getPlace());
                hasErrors = true;
            }
            if (!entry.getHashLogin().equals(currentLoginHash)) {
                System.err.println("Хэш логина не совпадает для записи: " + entry.getLogin());
                hasErrors = true;
            }
            if (!entry.getHashPassword().equals(currentPasswordHash)) {
                System.err.println("Хэш пароля не совпадает для записи: " + entry.getPassword());
                hasErrors = true;
            }
        }

        if (!hasErrors) {
            System.out.println("Все хэши совпадают. Целостность данных подтверждена.");
        }
    }

    private String hash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Ошибка хэширования", e);
        }
    }
}