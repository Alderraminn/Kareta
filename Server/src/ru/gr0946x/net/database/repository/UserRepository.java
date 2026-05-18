package ru.gr0946x.net.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.gr0946x.net.database.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {


    @Query("SELECT u FROM User u WHERE LOWER(u.nickname) = LOWER(:nickname)")
    Optional<User> findByNicknameIgnoreCase(@Param("nickname") String nickname);
}