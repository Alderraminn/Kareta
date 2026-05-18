package ru.gr0946x.ui;

import ru.gr0946x.net.MessageType;
import ru.gr0946x.net.ProtocolConstants;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

public class ConsoleUi implements Ui {

    private final List<Consumer<String>> listeners = new ArrayList<>();
    private boolean authenticated = false;

    public void start() {
        var scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        new Thread(() -> {
            while (true) {
                var userData = scanner.nextLine();
                for (var listener : listeners) {
                    listener.accept(userData);
                }
            }
        }).start();
    }

    @Override
    public void showInfo(String data, MessageType type) {
        switch (type) {
            case MESSAGE:
                handleMessage(data);
                break;
            case INFO:
                handleInfo(data);
                break;
            case ERROR:
                System.err.println(" Ошибка: " + data);
                break;
            case REQUEST:
                System.out.println("🔐 " + data);
                if (!authenticated) {
                    System.out.println(" Введите: логин:пароль (например, alice:secret123)");
                }
                break;
            default:
                System.out.println("[" + type + "] " + data);
        }
    }

    private void handleMessage(String data) {
        if (data.startsWith("ЛС от ")) {
            var parts = data.split(ProtocolConstants.AUTHOR_SEPARATOR, 2);
            System.out.println(" [Личное] " + (parts.length > 1 ? parts[1] : data));
        } else {
            var parts = data.split(ProtocolConstants.AUTHOR_SEPARATOR, 2);
            if (parts.length == 2) {
                System.out.println(" [" + parts[0] + "] " + parts[1]);
            } else {
                System.out.println(" " + data);
            }
        }
    }

    private void handleInfo(String data) {

        if (data.startsWith("HISTORY:")) {
            System.out.println("\n  ИСТОРИЯ СООБЩЕНИЙ ");
            String content = data.substring(8);
            if (content.startsWith("Нет")) {
                System.out.println("   " + content);
            } else {

                String[] lines = content.split("\n");
                for (String line : lines) {
                    System.out.println("   " + line);
                }
            }
            System.out.println("===========================\n");
            return;
        }


        if (data.startsWith("SEARCH:")) {
            System.out.println("\n  РЕЗУЛЬТАТЫ ПОИСКА ");
            String content = data.substring(7);
            if (content.contains("не найдены")) {
                System.out.println("   " + content);
            } else {
                String[] lines = content.split("\n");
                for (String line : lines) {
                    System.out.println("   " + line);
                }
            }
            System.out.println("===========================\n");
            return;
        }


        if (data.startsWith("Добро пожаловать,")) {
            authenticated = true;
            System.out.println("✅ " + data);
            System.out.println(" Форматы сообщений:");
            System.out.println("   MSG_ALL:текст     — сообщение всем");
            System.out.println("   MSG:ник:текст     — личное сообщение");
            System.out.println("   /msg ник текст    — альтернатива");
            System.out.println("   /quit             — выход");
        }

        else if (data.startsWith("USER_LIST:")) {
            String users = data.substring(10);
            System.out.println("👥 Онлайн: [" + (users.isEmpty() ? "нет" : users) + "]");
        }

        else if (data.contains("подключился") || data.contains("отключился")) {
            System.out.println("🔄 " + data);
        }

        else {
            System.out.println("ℹ️ " + data);
        }
    }

    @Override
    public void addUserDataListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeUserDataListener(Consumer<String> listener) {
        listeners.remove(listener);
    }
}