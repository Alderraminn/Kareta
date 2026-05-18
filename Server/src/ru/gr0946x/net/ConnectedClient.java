package ru.gr0946x.net;

import ru.gr0946x.net.database.DatabaseManager;
import ru.gr0946x.net.database.entity.Message;
import ru.gr0946x.net.database.entity.User;

import java.io.IOException;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectedClient {
    private final Communicator communicator;
    private static final Map<String, ConnectedClient> authenticatedClients = new ConcurrentHashMap<>();
    private User user = null;
    private final DatabaseManager dbManager;
    private enum ClientState { WAITING_AUTH, AUTHENTICATED }
    private ClientState state = ClientState.WAITING_AUTH;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd.MM HH:mm");
    private static final String LINE_SEP = "␤";

    public ConnectedClient(Socket socket) throws IOException {
        this.dbManager = DatabaseManager.getInstance();
        communicator = new Communicator(socket);
        communicator.addDataListener(this::parseData);
    }

    public void start() {
        sendCommand(MessageType.REQUEST, "Введите " + MessageType.LOGIN + ":логин:пароль");
        communicator.start();
    }

    public void sendData(String data) {
        communicator.sendData(data);
    }

    private void sendCommand(MessageType type, String content) {
        sendData(type + ProtocolConstants.COMMAND_SEPARATOR + content);
    }

    private void parseData(String data) {
        if (data == null || data.isBlank()) return;
        if (state == ClientState.WAITING_AUTH) handleAuth(data);
        else handleMessage(data);
    }

    private void handleAuth(String input) {
        String[] parts = input.split(ProtocolConstants.COMMAND_SEPARATOR, 3);
        if (parts.length != 3) {
            sendCommand(MessageType.ERROR, "Неверный формат");
            return;
        }
        String action = parts[0].trim().toUpperCase();
        String login = parts[1].trim();
        String password = parts[2].trim();

        if (!login.matches("^[a-zA-Zа-яА-ЯёЁ].*")) {
            sendCommand(MessageType.ERROR, "Имя должно начинаться с буквы");
            return;
        }

        if (authenticatedClients.containsKey(login.toLowerCase())) {
            sendCommand(MessageType.ERROR, "Пользователь уже подключён");
            return;
        }

        if (action.equals(MessageType.LOGIN)) {
            var existingUser = dbManager.authenticate(login, password);
            if (existingUser.isPresent()) completeAuth(existingUser.get());
            else sendCommand(MessageType.ERROR, "Неверный логин или пароль");
        }
        else if (action.equals(MessageType.REGISTER)) {
            if (dbManager.isUserExists(login)) sendCommand(MessageType.ERROR, "Пользователь уже существует");
            else if (dbManager.registerUser(login, password)) {
                dbManager.getUserByNickname(login).ifPresent(this::completeAuth);
            } else sendCommand(MessageType.ERROR, "Ошибка регистрации");
        }
        else {
            sendCommand(MessageType.ERROR, "Неизвестная команда");
        }
    }

    private void completeAuth(User authenticatedUser) {
        this.user = authenticatedUser;
        this.state = ClientState.AUTHENTICATED;
        authenticatedClients.put(user.getNickname().toLowerCase(), this);
        sendCommand(MessageType.INFO, "Добро пожаловать, " + user.getNickname() + "!");
        broadcastUserList();
    }

    private void handleMessage(String input) {
        if (input == null || input.isBlank()) return;
        try {
            if (input.startsWith(MessageType.HISTORY_CMD)) {
                handleHistory(input.substring(MessageType.HISTORY_CMD.length()).trim());
                return;
            }
            if (input.startsWith(MessageType.SEARCH_CMD)) {
                String[] searchParts = input.substring(MessageType.SEARCH_CMD.length()).trim().split(" ", 2);
                if (searchParts.length == 2) handleSearch(searchParts[0].trim(), searchParts[1].trim());
                else sendCommand(MessageType.ERROR, "Формат ошибки");
                return;
            }
            if (input.startsWith(MessageType.MSG_CMD)) {
                String[] msgParts = input.substring(MessageType.MSG_CMD.length()).trim().split(" ", 2);
                if (msgParts.length == 2) sendPrivateMessage(msgParts[0].trim(), msgParts[1]);
                return;
            }

            String[] parts = input.split(ProtocolConstants.COMMAND_SEPARATOR, 3);
            if (parts.length >= 2) {
                String command = parts[0].trim().toUpperCase();
                if (command.equals(MessageType.MSG_ALL)) {
                    broadcastPublicMessage(parts.length > 1 ? parts[1] : "");
                    return;
                }
                if (command.equals(MessageType.MSG) && parts.length == 3) {
                    sendPrivateMessage(parts[1].trim(), parts[2]);
                    return;
                }
            }
            broadcastPublicMessage(input);
        } catch (Exception e) {
            sendCommand(MessageType.ERROR, "Внутренняя ошибка сервера");
        }
    }

    private void broadcastPublicMessage(String text) {
        dbManager.saveMessage(user, null, text);
        String formatted = user.getNickname() + ProtocolConstants.AUTHOR_SEPARATOR + text;
        broadcastToAll(MessageType.MESSAGE, formatted);
    }

    private void sendPrivateMessage(String recipientNickname, String text) {
        var recipientOpt = dbManager.getUserByNickname(recipientNickname);
        if (recipientOpt.isEmpty()) {
            sendCommand(MessageType.ERROR, "Пользователь не найден");
            return;
        }
        User recipient = recipientOpt.get();
        dbManager.saveMessage(user, recipient, text);

        ConnectedClient recipientClient = authenticatedClients.get(recipientNickname.toLowerCase());
        if (recipientClient != null && recipientClient.user != null) {
            String formatted = "ЛС от " + user.getNickname() + ProtocolConstants.AUTHOR_SEPARATOR + text;
            recipientClient.sendCommand(MessageType.MESSAGE, formatted);
        }
        sendCommand(MessageType.INFO, "Сообщение отправлено");
    }

    private void handleHistory(String targetNick) {
        var targetOpt = dbManager.getUserByNickname(targetNick);
        if (targetOpt.isEmpty()) {
            sendCommand(MessageType.ERROR, "Пользователь не найден");
            return;
        }
        User target = targetOpt.get();
        List<Message> history = dbManager.getConversationHistory(user, target, 10);
        if (history.isEmpty()) {
            sendCommand(MessageType.INFO, "HISTORY:Нет сообщений");
            return;
        }
        Collections.reverse(history);
        StringBuilder sb = new StringBuilder("HISTORY:");
        for (Message msg : history) {
            String time = msg.getTimestamp().format(TIME_FMT);
            String sender = msg.getSender().getNickname();
            sb.append("[").append(time).append("] ").append(sender).append(": ").append(msg.getText()).append(LINE_SEP);
        }
        if (sb.length() > 8) sb.setLength(sb.length() - LINE_SEP.length());
        sendCommand(MessageType.INFO, sb.toString());
    }

    private void handleSearch(String targetNick, String keyword) {
        var targetOpt = dbManager.getUserByNickname(targetNick);
        if (targetOpt.isEmpty()) {
            sendCommand(MessageType.ERROR, "Пользователь не найден");
            return;
        }
        User target = targetOpt.get();
        List<Message> results = dbManager.searchMessages(user, target, keyword);
        if (results.isEmpty()) {
            sendCommand(MessageType.INFO, "SEARCH:Ничего не найдено");
            return;
        }
        Collections.reverse(results);
        StringBuilder sb = new StringBuilder("SEARCH:");
        for (Message msg : results) {
            String time = msg.getTimestamp().format(TIME_FMT);
            String sender = msg.getSender().getNickname();
            sb.append("[").append(time).append("] ").append(sender).append(": ").append(msg.getText()).append(LINE_SEP);
        }
        if (sb.length() > 7) sb.setLength(sb.length() - LINE_SEP.length());
        sendCommand(MessageType.INFO, sb.toString());
    }

    private void broadcastToAll(MessageType type, String content) {
        String fullMessage = type + ProtocolConstants.COMMAND_SEPARATOR + content;
        List<ConnectedClient> clientsCopy = new ArrayList<>(authenticatedClients.values());
        for (ConnectedClient c : clientsCopy) {
            if (c != this && c.user != null) {
                try { c.sendData(fullMessage); } catch (Exception ignored) {}
            }
        }
    }

    private void broadcastUserList() {
        StringBuilder list = new StringBuilder();
        for (String nickname : authenticatedClients.keySet()) {
            if (list.length() > 0) list.append(",");
            list.append(nickname);
        }
        String msg = MessageType.INFO + ProtocolConstants.COMMAND_SEPARATOR + "USER_LIST:" + list;
        for (ConnectedClient c : authenticatedClients.values()) {
            if (c.user != null) c.sendData(msg);
        }
    }

    public void stop() {
        if (user != null) {
            authenticatedClients.remove(user.getNickname().toLowerCase());
            broadcastUserList();
        }
        communicator.stop();
    }

    public User getUser() { return user; }
}