package ru.gr0946x.ui.gui;

import ru.gr0946x.net.Client;
import ru.gr0946x.net.MessageType;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.IOException;

public class GuiClient extends JFrame {
    private Client client;
    private String currentUser;
    private String selectedUser = null; // текущий собеседник

    private DefaultListModel<String> listModel = new DefaultListModel<>();
    private JTextPane generalChat, personalChat;
    private StyledDocument docGen, docPers;
    private JTextField loginField, inputField;
    private JPasswordField passField;
    private JLabel statusLabel, chatTitle;
    private JTabbedPane chatTabs;


    private final Color BG = new Color(248, 249, 250);
    private final Color ACCENT = new Color(0, 122, 204);
    private final Color MY_MSG = new Color(0, 130, 80);
    private final Color OTHER_MSG = new Color(35, 35, 35);
    private final Color SYS_MSG = new Color(120, 120, 120);
    private final Font FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);

    public GuiClient() {
        setTitle("Карета");
        setSize(820, 580);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG);

        docGen = new DefaultStyledDocument();
        docPers = new DefaultStyledDocument();
        generalChat = createChatArea(docGen);
        personalChat = createChatArea(docPers);

        initAuth();
    }

    private JTextPane createChatArea(StyledDocument doc) {
        JTextPane area = new JTextPane(doc);
        area.setEditable(false);
        area.setFont(FONT);
        area.setBackground(Color.WHITE);
        area.setMargin(new Insets(12, 12, 12, 12));
        return area;
    }

    private void initAuth() {
        JPanel auth = new JPanel(new GridBagLayout());
        auth.setBackground(BG);
        auth.setBorder(new EmptyBorder(50, 70, 50, 70));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 5, 10, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; auth.add(new JLabel("Логин"), gbc);
        loginField = new JTextField(20);
        loginField.setFont(FONT);
        gbc.gridx = 1; auth.add(loginField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; auth.add(new JLabel("Пароль"), gbc);
        passField = new JPasswordField(20);
        passField.setFont(FONT);
        gbc.gridx = 1; auth.add(passField, gbc);

        JButton btnLogin = new JButton("Войти");
        JButton btnReg = new JButton("Регистрация");
        styleButton(btnLogin); styleButton(btnReg);

        gbc.gridx = 0; gbc.gridy = 2; auth.add(btnLogin, gbc);
        gbc.gridx = 1; auth.add(btnReg, gbc);

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(FONT);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        auth.add(statusLabel, gbc);

        btnLogin.addActionListener(e -> auth("LOGIN"));
        btnReg.addActionListener(e -> auth("REGISTER"));
        loginField.addActionListener(e -> auth("LOGIN"));
        passField.addActionListener(e -> auth("LOGIN"));

        getContentPane().add(auth, BorderLayout.CENTER);
    }

    private void auth(String type) {
        String login = loginField.getText().trim();
        String pass = new String(passField.getPassword()).trim();
        if (login.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("Заполните все поля"); return;
        }
        try {
            client = new Client("localhost", 9460);
            client.addDataListener(this::onMessage);
            client.start();
            client.sendData(type + ":" + login + ":" + pass);
            currentUser = login;
            statusLabel.setText("Подключение...");
        } catch (IOException ex) {
            statusLabel.setText("Ошибка сети");
        }
    }

    private void initChat() {
        getContentPane().removeAll();
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBackground(BG);
        main.setBorder(new EmptyBorder(15, 15, 15, 15));


        JList<String> userList = new JList<>(listModel);
        userList.setFont(FONT);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedUser = userList.getSelectedValue();
                if (selectedUser != null && !selectedUser.equals(currentUser)) {
                    chatTabs.setSelectedIndex(1); //
                    chatTabs.setTitleAt(1, "Личный с @" + selectedUser);
                    try {
                        docPers.remove(0, docPers.getLength());
                    } catch (javax.swing.text.BadLocationException ignored) {

                    }
                    appendSys(docPers, "Загрузка истории...");
                    client.sendData("/history " + selectedUser);
                }
            }
        });
        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBackground(Color.WHITE);
        listPanel.setBorder(BorderFactory.createTitledBorder(new LineBorder(new Color(210,210,210)), "Контакты"));
        listPanel.add(new JScrollPane(userList), BorderLayout.CENTER);
        main.add(listPanel, BorderLayout.WEST);

        // Область чатов (вкладки)
        chatTabs = new JTabbedPane();
        chatTabs.setFont(FONT_SMALL);
        chatTabs.addTab("Общий чат", new JScrollPane(generalChat));
        chatTabs.addTab(" Личный чат", new JScrollPane(personalChat));
        chatTabs.addChangeListener(e -> {
            if (chatTabs.getSelectedIndex() == 0) {
                selectedUser = null;
                chatTitle.setText(" Общий чат");
            } else {
                chatTitle.setText(" Личный чат" + (selectedUser != null ? " с @" + selectedUser : ""));
            }
        });

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setBorder(new LineBorder(new Color(210,210,210)));

        chatTitle = new JLabel(" Общий чат", SwingConstants.CENTER);
        chatTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        chatTitle.setForeground(ACCENT);
        chatTitle.setOpaque(true);
        chatTitle.setBackground(Color.WHITE);
        chatTitle.setBorder(new EmptyBorder(8, 0, 8, 0));

        centerPanel.add(chatTitle, BorderLayout.NORTH);
        centerPanel.add(chatTabs, BorderLayout.CENTER);
        main.add(centerPanel, BorderLayout.CENTER);


        JPanel inputPanel = new JPanel(new BorderLayout(8, 8));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(BorderFactory.createTitledBorder(new LineBorder(new Color(210,210,210)), "Сообщение"));
        inputField = new JTextField();
        inputField.setFont(FONT);
        inputField.setToolTipText("Введите текст. GUI сам выберет адресата по активной вкладке.");

        JButton btnSend = new JButton("Отправить");
        btnSend.addActionListener(e -> send());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(btnSend, BorderLayout.EAST);


        JLabel commandHint = new JLabel(
                " Подсказки: клик на контакт → личный чат | /history ник | /search ник слово | Enter для отправки",
                SwingConstants.CENTER
        );
        commandHint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        commandHint.setForeground(SYS_MSG);
        commandHint.setBorder(new EmptyBorder(6, 0, 4, 0));


        JPanel southContainer = new JPanel(new BorderLayout(0, 0));
        southContainer.setBackground(BG);
        southContainer.add(inputPanel, BorderLayout.CENTER);
        southContainer.add(commandHint, BorderLayout.SOUTH);

        main.add(southContainer, BorderLayout.SOUTH);

        inputField.addActionListener(e -> send());
        getContentPane().add(main, BorderLayout.CENTER);
        revalidate(); repaint();
    }

    private void onMessage(String data, MessageType type) {
        SwingUtilities.invokeLater(() -> {
            switch (type) {
                case INFO:
                    if (data.startsWith("USER_LIST:")) {
                        updateUserList(data.substring(10));
                    }
                    else if (data.startsWith("HISTORY:") || data.startsWith("SEARCH:")) {

                        StyledDocument targetDoc = (chatTabs.getSelectedIndex() == 1) ? docPers : docGen;

                        String prefix = data.startsWith("HISTORY:") ? "📜" : "";
                        appendSys(targetDoc, prefix + " Результаты:");

                        String content = data.substring(data.indexOf(':') + 1);

                        String[] lines = content.split("␤");
                        for (String line : lines) {
                            if (!line.trim().isEmpty()) {
                                appendText(targetDoc, line, SYS_MSG, StyleConstants.ALIGN_LEFT);
                            }
                        }
                    }
                    else if (data.contains("Добро пожаловать")) {
                        statusLabel.setText("✅ " + data);
                        initChat();
                    }
                    else {
                        appendSys(docGen, data);
                    }
                    break;

                case MESSAGE:
                    boolean isPrivate = data.startsWith("ЛС от ");
                    StyledDocument targetDoc = isPrivate ? docPers : docGen;

                    // Если пришло личное, а открыта вкладка общего -> переключаем
                    if (isPrivate && chatTabs.getSelectedIndex() == 0) {
                        chatTabs.setSelectedIndex(1);
                    }


                    String clean = data.replace("ЛС от ", "");
                    Color color = isPrivate ? ACCENT : OTHER_MSG;
                    int align = StyleConstants.ALIGN_LEFT;

                    appendText(targetDoc, (isPrivate ? "🔒 " : " ") + clean, color, align);
                    break;

                case ERROR:
                    JOptionPane.showMessageDialog(this, "Ошибка: " + data);
                    break;
            }

            JTextPane active = chatTabs.getSelectedIndex() == 0 ? generalChat : personalChat;
            active.setCaretPosition(active.getDocument().getLength());
        });
    }

    private void updateUserList(String csv) {
        listModel.clear();
        if (!csv.isEmpty()) {
            for (String u : csv.split(",")) {
                String nick = u.trim();
                if (!nick.equalsIgnoreCase(currentUser)) listModel.addElement(nick);
            }
        }
    }

    private void send() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;


        if (text.startsWith("/")) {
            client.sendData(text);
            inputField.setText("");
            return;
        }


        StyledDocument targetDoc = (chatTabs.getSelectedIndex() == 0) ? docGen : docPers;
        appendText(targetDoc, " [Вы] " + text, MY_MSG, StyleConstants.ALIGN_RIGHT);

        if (chatTabs.getSelectedIndex() == 0) {
            client.sendData("MSG_ALL:" + text);
        } else {
            if (selectedUser == null) {
                JOptionPane.showMessageDialog(this, "Выберите собеседника из списка слева");
                return;
            }
            client.sendData("MSG:" + selectedUser + ":" + text);
        }

        inputField.setText("");
        inputField.requestFocus();
    }

    private void appendSys(StyledDocument doc, String text) { appendText(doc, text, SYS_MSG, StyleConstants.ALIGN_CENTER); }

    private void appendText(StyledDocument doc, String text, Color color, int align) {
        try {
            SimpleAttributeSet set = new SimpleAttributeSet();
            StyleConstants.setForeground(set, color);
            StyleConstants.setAlignment(set, align);
            StyleConstants.setFontFamily(set, FONT.getFontName());
            StyleConstants.setFontSize(set, FONT.getSize());
            doc.insertString(doc.getLength(), text + "\n\n", set);
        } catch (Exception ignored) {}
    }

    private void styleButton(JButton b) {
        b.setFont(FONT); b.setBackground(ACCENT); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setBorderPainted(false); b.setOpaque(true);
    }

    public static void launch() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new GuiClient().setVisible(true));
    }
}