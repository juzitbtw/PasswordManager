package PasswordManager;

public class PasswordEntry {
    private String place;
    private String login;
    private String password;
    private static final String DEFAULT_PLACE = "[Нет адреса]";
    private static final String DEFAULT_LOGIN = "[Нет логина]";
    private static final String DEFAULT_PASSWORD = "[Нет пароля]";

    public PasswordEntry(String place, String login, String password) {
        this.place = place != null && !place.isEmpty() ? place : DEFAULT_PLACE;
        this.login = login != null && !login.isEmpty() ? login : DEFAULT_LOGIN;
        this.password = password != null && !password.isEmpty() ? password : DEFAULT_PASSWORD;
    }

    public String getPlace() { return place; }
    public String getLogin() { return login; }
    public String getPassword() { return password; }


    @Override
    public String toString() {
        return String.format("%s,%s,%s",
                place,
                login,
                password);
    }
}