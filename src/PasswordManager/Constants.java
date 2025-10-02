package PasswordManager;

public class Constants {
    protected static final String FILE_NAME = "mineSecurePasswords.txt";
    protected static final int SALT_SIZE = 16;
    protected static final int ITERATIONS = 500_000;
    protected static final int KEY_SIZE = 256;
    protected static final int IV_LENGTH = 12;
    protected static final int GCM_TAG_LENGTH = 128;
    protected static final int MAX_ATTEMPTS = 3;
    protected static final String DEFAULT_PLACE = "[Нет адреса]";
    protected static final String DEFAULT_LOGIN = "[Нет логина]";
    protected static final String DEFAULT_PASSWORD = "[Нет пароля]";
}
