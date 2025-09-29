package PasswordManager;

import java.util.Scanner;
import java.util.logging.Logger;

public class Starter {
    private static final Scanner scanner = new Scanner(System.in);
    private static final Logger logger = Logger.getLogger(Starter.class.getName());
    private static String masterPassword;
    private static PasswordManager manager = new PasswordManager();

    public static void main(String[] args) {
        initializeApplication();
        showMainMenu();
    }

    private static void initializeApplication() {
        final int MAX_ATTEMPTS = 3;

        for (int attempt = 0; attempt <= MAX_ATTEMPTS; attempt++) {
            if (attempt == 0) {
                System.out.print("Введите мастер-пароль: ");
            } else {
                System.out.printf("Ошибка: Проверьте пароль. У вас осталось %d попыток.\n", MAX_ATTEMPTS - attempt + 1);
                System.out.print("Повторите ввод: ");
            }

            String inputPass = scanner.nextLine();

            try {
                manager.loadEntries(inputPass);
                masterPassword = inputPass;
                return;
            } catch (Exception e) {
                if (attempt == MAX_ATTEMPTS) {
                    System.out.println("Ошибка: Проверьте пароль. Попытки истекли.");
                    return;
                }
            }
        }
    }

    private static void showMainMenu() {
        while (true) {
            printMenu();
            int choice = getMenuChoice();

            switch (choice) {
                case 1 -> addEntry();
                case 2 -> deleteEntry();
                case 3 -> displayAllEntries();
                case 4 -> viewSpecificEntry();
                case 5 -> editEntry();
                case 6 -> changeMasterPassword();
                case 7 -> verifyDataIntegrity();
                case 8 -> {
                    return;
                }
                default -> System.out.println("Неверный выбор.");
            }
        }
    }

    private static void verifyDataIntegrity() {
        try {
            manager.verifyIntegrity();
            System.out.println("Целостность данных подтверждена.");
        } catch (Exception e) {
            System.err.println("Ошибка при проверке целостности: " + e.getMessage());
        }
    }

    private static void printMenu() {
        System.out.println("\nМенеджер паролей:");
        System.out.println("1. Добавить запись");
        System.out.println("2. Удалить запись");
        System.out.println("3. Просмотреть все записи");
        System.out.println("4. Получить конкретную запись");
        System.out.println("5. Редактировать запись");
        System.out.println("6. Изменить мастер-пароль");
        System.out.println("7. Проверить целостность данных");
        System.out.println("8. Выйти");
        System.out.print("Выберите действие: ");
    }

    private static int getMenuChoice() {
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Пожалуйста, введите число от 1 до 7.");
            return 0;
        }
    }

    private record EntryData(String place, String login, String password) {}

    private static EntryData readPasswordEntry() {
        System.out.print("Место: ");
        var place = scanner.nextLine().trim();
        System.out.print("Логин: ");
        var login = scanner.nextLine().trim();
        System.out.print("Пароль: ");
        var password = scanner.nextLine().trim();

        return new EntryData(
                place.isEmpty() ? "[Нет адреса]" : place,
                login.isEmpty() ? "[Нет логина]" : login,
                password.isEmpty() ? "[Нет пароля]" : password
        );
    }

    private static int validateIndex(String prompt) {
        System.out.print(prompt);
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Неверный номер записи.");
            return -1;
        }
    }

    private static void addEntry() {
        var data = readPasswordEntry();
        manager.addEntry(data.place(), data.login(), data.password());

        try {
            manager.saveEntries(masterPassword);
            System.out.println("Запись добавлена.");
        } catch (Exception ex) {
            System.out.println("Ошибка сохранения.");
            logger.severe("Не удалось сохранить запись: " + ex.getMessage());
        }
    }

    private static void deleteEntry() {
        manager.displayEntries();
        int index = validateIndex("Введите номер записи для удаления: ");
        if (index < 0) return;

        try {
            manager.removeEntry(index);
            manager.saveEntries(masterPassword);
            System.out.println("Запись удалена.");
        } catch (Exception ex) {
            System.out.println("Ошибка при удалении.");
            logger.severe("Не удалось удалить запись: " + ex.getMessage());
        }
    }

    private static void displayAllEntries() {
        System.out.println("-".repeat(20));
        System.out.println("Все записи:");
        manager.displayEntries();
        System.out.println("-".repeat(20));
    }

    private static void viewSpecificEntry() {
        manager.displayEntries();
        int index = validateIndex("Введите номер записи для просмотра: ");
        if (index < 0) return;

        var entry = manager.getEntry(index);
        if (entry != null) {
            System.out.println("-".repeat(20));
            System.out.println("Место: " + entry.getPlace());
            System.out.println("Логин: " + entry.getLogin());
            System.out.println("Пароль: " +
                    (entry.getPassword() == null || entry.getPassword().isEmpty()
                            ? "[не указан]" : entry.getPassword()));
            System.out.println("-".repeat(20));
        } else {
            System.out.println("Неверный номер записи.");
        }
    }

    private static void editEntry() {
        manager.displayEntries();
        int index = validateIndex("Введите номер записи для редактирования: ");
        if (index < 0) return;

        var entry = manager.getEntry(index);
        if (entry == null) {
            System.out.println("Неверный номер записи.");
            return;
        }

        System.out.println("\nВыберите, что хотите изменить:");
        System.out.println("1. Место");
        System.out.println("2. Логин");
        System.out.println("3. Пароль");
        System.out.println("4. Все поля");
        System.out.print("Ваш выбор: ");
        int choice = scanner.nextInt();
        scanner.nextLine();

        String newPlace = entry.getPlace();
        String newLogin = entry.getLogin();
        String newPassword = entry.getPassword();

        switch (choice) {
            case 1 -> {
                System.out.print("Новое место: ");
                newPlace = scanner.nextLine().trim();
            }
            case 2 -> {
                System.out.print("Новый логин: ");
                newLogin = scanner.nextLine().trim();
            }
            case 3 -> {
                System.out.print("Новый пароль: ");
                newPassword = scanner.nextLine().trim();
            }
            case 4 -> {
                System.out.print("Новое место: ");
                newPlace = scanner.nextLine().trim();
                System.out.print("Новый логин: ");
                newLogin = scanner.nextLine().trim();
                System.out.print("Новый пароль: ");
                newPassword = scanner.nextLine().trim();
            }
            default -> {
                System.out.println("Неверный выбор.");
                return;
            }
        }

        manager.updateEntry(index, newPlace, newLogin, newPassword);
        try {
            manager.saveEntries(masterPassword);
            System.out.println("Запись обновлена.");
        } catch (Exception ex) {
            System.out.println("Ошибка при обновлении.");
            logger.severe("Не удалось обновить запись: " + ex.getMessage());
        }
    }

    private static void changeMasterPassword() {
        System.out.print("Текущий мастер-пароль: ");
        var currentPass = scanner.nextLine();

        if (!currentPass.equals(masterPassword)) {
            System.out.println("Неверный мастер-пароль.");
            return;
        }

        System.out.print("Новый мастер-пароль: ");
        var newMasterPass = scanner.nextLine();
        System.out.print("Подтвердите новый мастер-пароль: ");
        var confirmNewPass = scanner.nextLine();

        if (!newMasterPass.equals(confirmNewPass)) {
            System.out.println("Пароли не совпадают.");
            return;
        }

        masterPassword = newMasterPass;

        try {
            manager.reencryptWithNewMasterPassword(currentPass, newMasterPass);
            System.out.println("Мастер-пароль успешно изменён.");
        } catch (Exception ex) {
            System.out.println("Ошибка при изменении мастер-пароля: " + ex.getMessage());
            logger.severe("Не удалось изменить мастер-пароль: " + ex.getMessage());
        }
    }

    private static void exitProgram() {
        System.out.println("Выход из программы.");
        scanner.close();
    }
}