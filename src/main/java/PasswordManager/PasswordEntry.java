package main.java.PasswordManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class PasswordEntry {
    private String place;
    private String login;
    private String password;
    private String hashPlace;
    private String hashLogin;
    private String hashPassword;
    private byte[] salt;
    private byte[] aesKey;

    public PasswordEntry(String place, String login, String password) {
        this.place = place != null && !place.isEmpty() ? place : Constants.DEFAULT_PLACE;
        this.login = login != null && !login.isEmpty() ? login : Constants.DEFAULT_LOGIN;
        this.password = password != null && !password.isEmpty() ? password : Constants.DEFAULT_PASSWORD;

        this.hashPlace = hash(this.place, "SHA3-256");
        this.hashLogin = hash(this.login, "SHA3-256");
        this.hashPassword = hash(this.password, "SHA3-256");

        this.salt = KeyDeriver.generateSalt(Constants.SALT_SIZE);
        this.aesKey = new byte[Constants.KEY_SIZE / 8]; // AES-256 требует 32 байта
        new SecureRandom().nextBytes(aesKey);
    }

    public String getPlace() { return place; }
    public String getLogin() { return login; }
    public String getPassword() { return password; }

    public String getHashPlace() { return hashPlace; }
    public String getHashLogin() { return hashLogin; }
    public String getHashPassword() { return hashPassword; }

    // Задел на будущее
    public byte[] getSalt() { return salt; }
    public byte[] getAesKey() { return aesKey; }
    public void setSalt(byte[] salt) { this.salt = salt; }
    public void setAesKey(byte[] aesKey) { this.aesKey = aesKey; }

    public void setHashPlace(String hashPlace) { this.hashPlace = hashPlace; }
    public void setHashLogin(String hashLogin) { this.hashLogin = hashLogin; }
    public void setHashPassword(String hashPassword) { this.hashPassword = hashPassword; }


    public static String hash(String data, String algorithm) {
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

    @Override
    public String toString() {
        return String.format("%s,%s,%s", place, login, password);
    }
}