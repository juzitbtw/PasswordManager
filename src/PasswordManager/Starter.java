package PasswordManager;

import java.util.Scanner;

public class Starter {
    private static final Scanner scanner = new Scanner(System.in);
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
                    System.exit(0);
                }
            }
        }
    }

    private static void showMainMenu() {
        while (true) {
            printMenu();
            int choice = getMenuChoice();

            switch (choice) {
                case 1:
                    addEntry();
                    break;
                case 2:
                    deleteEntry();
                    break;
                case 3:
                    displayAllEntries();
                    break;
                case 4:
                    viewSpecificEntry();
                    break;
                case 5:
                    editEntry();
                    break;
                case 6:
                    changeMasterPassword();
                    break;
                case 7:
                    exitProgram();
                    return;
                default:
                    System.out.println("Неверный выбор.");
            }
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
        System.out.println("7. Выйти");
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

    private static void addEntry() {
        System.out.print("Место: ");
        String place = scanner.nextLine().trim();
        System.out.print("Логин: ");
        String login = scanner.nextLine().trim();
        System.out.print("Пароль: ");
        String password = scanner.nextLine().trim();

        // Подставляем значения по умолчанию, если пользователь не ввёл
        place = place.isEmpty() ? "[Нет адреса]" : place;
        login = login.isEmpty() ? "[Нет логина]" : login;
        password = password.isEmpty() ? "[Нет пароля]" : password;

        manager.addEntry(place, login, password);

        try {
            manager.saveEntries(masterPassword);
            System.out.println("Запись добавлена.");
        } catch (Exception ex) {
            System.out.println("Ошибка сохранения.");
            ex.printStackTrace();
        }
    }

    private static void deleteEntry() {
        manager.displayEntries();
        System.out.print("Введите номер записи для удаления: ");

        try {
            int index = Integer.parseInt(scanner.nextLine());
            manager.removeEntry(index);
            manager.saveEntries(masterPassword);
            System.out.println("Запись удалена.");
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            System.out.println("Неверный номер записи.");
        } catch (Exception ex) {
            System.out.println("Ошибка сохранения.");
            ex.printStackTrace();
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
        System.out.print("Введите номер записи для просмотра: ");

        try {
            int index = Integer.parseInt(scanner.nextLine());
            PasswordEntry entry = manager.getEntry(index);

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
        } catch (NumberFormatException e) {
            System.out.println("Неверный номер записи.");
        }
    }

    private static void editEntry() {
        manager.displayEntries();
        System.out.print("Введите номер записи для редактирования: ");

        try {
            int index = Integer.parseInt(scanner.nextLine());
            PasswordEntry entry = manager.getEntry(index);

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
                case 1:
                    System.out.print("Новое место: ");
                    newPlace = scanner.nextLine().trim();
                    break;
                case 2:
                    System.out.print("Новый логин: ");
                    newLogin = scanner.nextLine().trim();
                    break;
                case 3:
                    System.out.print("Новый пароль: ");
                    newPassword = scanner.nextLine().trim();
                    break;
                case 4:
                    System.out.print("Новое место: ");
                    newPlace = scanner.nextLine().trim();
                    System.out.print("Новый логин: ");
                    newLogin = scanner.nextLine().trim();
                    System.out.print("Новый пароль: ");
                    newPassword = scanner.nextLine().trim();
                    break;
                default:
                    System.out.println("Неверный выбор.");
                    return;
            }

            manager.updateEntry(index, newPlace, newLogin, newPassword);
            manager.saveEntries(masterPassword);
            System.out.println("Запись обновлена.");

        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            System.out.println("Неверный номер записи.");
        } catch (Exception ex) {
            System.out.println("Ошибка: " + ex.getMessage());
        }
    }

    private static void changeMasterPassword() {
        System.out.print("Текущий мастер-пароль: ");
        String currentPass = scanner.nextLine();

        if (!currentPass.equals(masterPassword)) {
            System.out.println("Неверный мастер-пароль.");
            return;
        }

        System.out.print("Новый мастер-пароль: ");
        String newMasterPass = scanner.nextLine();
        System.out.print("Подтвердите новый мастер-пароль: ");
        String confirmNewPass = scanner.nextLine();

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
        }
    }

    private static void exitProgram() {
        System.out.println("Выход из программы.");
        scanner.close();
    }
}