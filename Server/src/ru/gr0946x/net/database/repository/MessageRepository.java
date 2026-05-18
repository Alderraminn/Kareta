package ru.gr0946x.net.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.gr0946x.net.database.entity.Message;
import ru.gr0946x.net.database.entity.User;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Integer> {


    @Query("SELECT m FROM Message m " +
            "JOIN FETCH m.sender " +
            "LEFT JOIN FETCH m.recipient " +
            "WHERE ((m.sender = :user1 AND m.recipient = :user2) " +
            "OR (m.sender = :user2 AND m.recipient = :user1)) " +
            "ORDER BY m.timestamp DESC")
    List<Message> findHistoryBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);



    @Query("SELECT m FROM Message m " +
            "JOIN FETCH m.sender " +
            "LEFT JOIN FETCH m.recipient " +
            "WHERE ((m.sender = :user1 AND m.recipient = :user2) " +
            "OR (m.sender = :user2 AND m.recipient = :user1)) " +
            "AND LOWER(m.text) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY m.timestamp DESC")
    List<Message> searchInConversation(@Param("user1") User user1,
                                       @Param("user2") User user2,
                                       @Param("keyword") String keyword);
}