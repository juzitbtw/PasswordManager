package PasswordManager;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;

public class Main {
    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());

        PasswordManager manager = new PasswordManager();
        PasswordManagerUI ui = new PasswordManagerUI(manager);

        ui.start();
    }
}
