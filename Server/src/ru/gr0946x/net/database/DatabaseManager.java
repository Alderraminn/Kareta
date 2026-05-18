package ru.gr0946x.net.database;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.gr0946x.net.config.DatabaseConfig;
import ru.gr0946x.net.database.entity.Message;
import ru.gr0946x.net.database.entity.User;
import ru.gr0946x.net.database.repository.MessageRepository;
import ru.gr0946x.net.database.repository.UserRepository;

import java.util.List;
import java.util.Optional;

public class DatabaseManager {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private static DatabaseManager instance;

    private DatabaseManager() {

        ApplicationContext context = new AnnotationConfigApplicationContext(DatabaseConfig.class);
        this.userRepository = context.getBean(UserRepository.class);
        this.messageRepository = context.getBean(MessageRepository.class);
    }


    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public boolean registerUser(String nickname, String password) {
        if (userRepository.findByNicknameIgnoreCase(nickname).isPresent()) {
            return false;
        }

        User user = new User(nickname, password);
        userRepository.save(user);
        return true;
    }


    public Optional<User> authenticate(String nickname, String password) {
        Optional<User> user = userRepository.findByNicknameIgnoreCase(nickname);
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            return user;
        }
        return Optional.empty();
    }


    public boolean isUserExists(String nickname) {
        return userRepository.findByNicknameIgnoreCase(nickname).isPresent();
    }


    public Optional<User> getUserByNickname(String nickname) {
        return userRepository.findByNicknameIgnoreCase(nickname);
    }


    public Message saveMessage(User sender, User recipient, String text) {
        Message message = new Message(sender, recipient, text);
        return messageRepository.save(message);
    }


    public List<Message> getConversationHistory(User user1, User user2, int limit) {
        List<Message> allMessages = messageRepository.findHistoryBetweenUsers(user1, user2);

        if (allMessages.size() <= limit) {
            return allMessages;
        }
        return allMessages.subList(0, limit);
    }


    public List<Message> searchMessages(User user1, User user2, String keyword) {
        return messageRepository.searchInConversation(user1, user2, keyword);
    }


    public List<Message> getAllMessagesForUser(User user) {
        return messageRepository.findAll();
    }
}
