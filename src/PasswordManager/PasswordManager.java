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

        salt = Base64.getDecoder().decode(lines.get(0));

        if (lines.size() > 1) {
            try {
                String firstEncryptedLine = lines.get(1);
                String decryptedLine = AESEncryption.decryptWithUniqueKeys(masterPassword, firstEncryptedLine);
            } catch (Exception e) {
                throw new Exception("Неверный мастер-пароль");
            }
        }

        entries = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String encryptedLine = lines.get(i);

            try {
                String decryptedLine = AESEncryption.decryptWithUniqueKeys(masterPassword, encryptedLine);
                String[] fields = decryptedLine.split(",");
                if (fields.length != 3) continue;

                PasswordEntry entry = new PasswordEntry(fields[0], fields[1], fields[2]);
                entries.add(entry);
            } catch (Exception e) {
                System.err.println("Ошибка при загрузке записи: " + e.getMessage());
            }
        }

        verifyIntegrity();
    }

    public void saveEntries(String masterPassword) throws Exception {
        boolean isNewFile = !Files.exists(Paths.get(Constants.FILE_NAME));
        byte[] saltToUse = isNewFile || salt == null ? KeyDeriver.generateSalt(Constants.SALT_SIZE) : salt;
        this.encryptionKey = KeyDeriver.deriveKey(masterPassword, saltToUse, Constants.ITERATIONS, Constants.KEY_SIZE);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(Constants.FILE_NAME))) {
            writer.write(Base64.getEncoder().encodeToString(saltToUse));
            writer.newLine();

            for (PasswordEntry entry : entries) {
                String line = AESEncryption.encryptWithUniqueKeys(
                        masterPassword,
                        entry.getPlace(),
                        entry.getLogin(),
                        entry.getPassword()
                );
                writer.write(line);
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

        byte[] newSalt = KeyDeriver.generateSalt(Constants.SALT_SIZE);
        byte[] newEncryptionKey = KeyDeriver.deriveKey(newMasterPassword, newSalt, Constants.ITERATIONS,
                Constants.KEY_SIZE);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write(Base64.getEncoder().encodeToString(newSalt));
            writer.newLine();

            for (int i = 1; i < lines.size(); i++) {
                String encryptedLine = lines.get(i);
                String decryptedLine = AESEncryption.decryptWithUniqueKeys(currentMasterPassword, encryptedLine);
                String[] fields = decryptedLine.split(",");
                if (fields.length != 3) continue;

                String newEncryptedLine = AESEncryption.encryptWithUniqueKeys(newMasterPassword, fields[0], fields[1], fields[2]);
                writer.write(newEncryptedLine);
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
            System.out.println((i+1) + ": " + entries.get(i).getPlace() + " - " + entries.get(i).getLogin());
        }
    }

    public void updateEntry(int index, String newPlace, String newLogin, String newPassword) {
        if (index < 0 || index >= entries.size()) {
            throw new IllegalArgumentException("Неверный индекс: " + index);
        }

        PasswordEntry oldEntry = entries.get(index);

        String place = newPlace != null && !newPlace.isEmpty() ? newPlace : oldEntry.getPlace();
        String login = newLogin != null && !newLogin.isEmpty() ? newLogin : oldEntry.getLogin();
        String password = newPassword != null && !newPassword.isEmpty() ? newPassword : oldEntry.getPassword();

        PasswordEntry newEntry = new PasswordEntry(
                place.isEmpty() ? Constants.DEFAULT_PLACE : place,
                login.isEmpty() ? Constants.DEFAULT_LOGIN : login,
                password.isEmpty() ? Constants.DEFAULT_PASSWORD : password
        );

        newEntry.setHashPlace(oldEntry.getHashPlace());
        newEntry.setHashLogin(oldEntry.getHashLogin());
        newEntry.setHashPassword(oldEntry.getHashPassword());

        entries.set(index, newEntry);
    }

    public void verifyIntegrity() {
        boolean hasErrors = false;

        for (PasswordEntry entry : entries) {
            String currentPlaceHash = hash(entry.getPlace(), "SHA3-256");
            String currentLoginHash = hash(entry.getLogin(), "SHA3-256");
            String currentPasswordHash = hash(entry.getPassword(), "SHA3-256");

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
            System.out.println("Все хэши совпадают.");
        }
    }

    private String hash(String data, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Ошибка хэширования с алгоритмом " + algorithm, e);
        }
    }
}