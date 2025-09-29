package PasswordManager;

public class Constants {
    public static final String FILE_NAME = "mineSecurePasswords.txt";
    public static final int SALT_SIZE = 16;
    public static final int ITERATIONS = 65536;
    public static final int KEY_SIZE = 256;
    public static final int IV_LENGTH = 12;
    public static final int GCM_TAG_LENGTH = 128;
}
