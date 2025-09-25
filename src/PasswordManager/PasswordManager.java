package PasswordManager;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class PasswordManager {
    private static final String FILE_NAME = "mineSecurePasswords.txt";
    List<PasswordEntry> entries;
    private byte[] salt;
    private byte[] encryptionKey;

    public PasswordManager() {
        entries = new ArrayList<>();
        salt = null;
        encryptionKey = null;
    }

    public void loadEntries(String masterPassword) throws Exception {
        if (!Files.exists(Paths.get(FILE_NAME))) {
            Files.createFile(Paths.get(FILE_NAME));
            salt = KeyDeriver.generateSalt(16);
            entries = new ArrayList<>();
            return;
        }

        List<String> lines = Files.readAllLines(Paths.get(FILE_NAME));
        if (lines.isEmpty()) {
            salt = KeyDeriver.generateSalt(16);
            entries = new ArrayList<>();
            return;
        }

        // Первая строка — это соль
        String saltBase64 = lines.get(0);
        salt = Base64.getDecoder().decode(saltBase64);

        this.encryptionKey = KeyDeriver.deriveKey(masterPassword, salt, 65536, 256);

        entries = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String encryptedLine = lines.get(i);
            PasswordEntry entry = AESEncryption.decrypt(this.encryptionKey, encryptedLine);
            entries.add(entry);
        }
    }

    public void saveEntries(String masterPassword) throws Exception {
        boolean isNewFile = !Files.exists(Paths.get(FILE_NAME));

        byte[] saltToUse = isNewFile || salt == null ? KeyDeriver.generateSalt(16) : salt;
        this.encryptionKey = KeyDeriver.deriveKey(masterPassword, saltToUse, 65536, 256);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME))) {
            // Сохраняем соль в начале файла
            String saltBase64 = Base64.getEncoder().encodeToString(saltToUse);
            writer.write(saltBase64);
            writer.newLine();

            for (PasswordEntry entry : entries) {
                try {
                    String encrypted = AESEncryption.encrypt(this.encryptionKey, entry.toString());
                    //byte[] encryptedBytes = encrypted.getBytes(StandardCharsets.UTF_8);
                    writer.write(encrypted);
                    writer.newLine();
                } catch (Exception e) {
                    System.err.println("Ошибка шифрования записи: " + entry.toString());
                    e.printStackTrace();
                    throw e;
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка записи в файл: " + FILE_NAME);
            e.printStackTrace();
            throw e;
        }

        this.salt = saltToUse;
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

        // Если новые значения пустые — оставляем старые
        String place = newPlace.isEmpty() ? editEntry.getPlace() : newPlace;
        String login = newLogin.isEmpty() ? editEntry.getLogin() : newLogin;
        String password = newPassword.isEmpty() ? editEntry.getPassword() : newPassword;

        entries.set(index, new PasswordEntry(place, login, password));
    }

    public void reencryptWithNewMasterPassword(String currentMasterPassword, String newMasterPassword) throws Exception {
        String tempFile = FILE_NAME + ".tmp";
        Files.deleteIfExists(Paths.get(tempFile));

        List<String> lines = Files.readAllLines(Paths.get(FILE_NAME));
        if (lines.isEmpty()) return;

        String oldSaltBase64 = lines.get(0);
        byte[] oldSalt = Base64.getDecoder().decode(oldSaltBase64);

        // Генерируем ключ на основе СТАРОГО мастер-пароля
        byte[] oldEncryptionKey = KeyDeriver.deriveKey(currentMasterPassword, oldSalt, 65536, 256);

        byte[] newSalt = KeyDeriver.generateSalt(16);
        byte[] newEncryptionKey = KeyDeriver.deriveKey(newMasterPassword, newSalt, 65536, 256);

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
                        entry.getPlace(), entry.getLogin(), entry.getPassword());
                writer.write(newEncrypted);
                writer.newLine();
            }
        }

        Files.move(Paths.get(tempFile), Paths.get(FILE_NAME), StandardCopyOption.REPLACE_EXISTING);

        entries.clear();
        salt = newSalt;
        encryptionKey = newEncryptionKey;

        try {
            loadEntries(newMasterPassword);
        } catch (Exception ex) {
            System.err.println("Не удалось загрузить данные после изменения мастер-пароля.");
            throw ex;
        }
    }
}