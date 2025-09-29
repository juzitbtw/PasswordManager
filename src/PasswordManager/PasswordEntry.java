package PasswordManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordEntry {
    private String place;
    private String login;
    private String password;
    private String hashPlace;
    private String hashLogin;
    private String hashPassword;
    private static final String DEFAULT_PLACE = "[Нет адреса]";
    private static final String DEFAULT_LOGIN = "[Нет логина]";
    private static final String DEFAULT_PASSWORD = "[Нет пароля]";

    public PasswordEntry(String place, String login, String password) {
        this.place = place != null && !place.isEmpty() ? place : DEFAULT_PLACE;
        this.login = login != null && !login.isEmpty() ? login : DEFAULT_LOGIN;
        this.password = password != null && !password.isEmpty() ? password : DEFAULT_PASSWORD;

        this.hashPlace = hash(this.place);
        this.hashLogin = hash(this.login);
        this.hashPassword = hash(this.password);
    }

    public String getPlace() { return place; }
    public String getLogin() { return login; }
    public String getPassword() { return password; }

    public String getHashPlace() { return hashPlace; }
    public String getHashLogin() { return hashLogin; }
    public String getHashPassword() { return hashPassword; }

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

    @Override
    public String toString() {
        return String.format("%s,%s,%s", place, login, password);
    }
}