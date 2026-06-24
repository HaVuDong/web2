package com.example.boardinghouse.repository;

import com.example.boardinghouse.domain.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

/**
 * Repository (DAO) để thao tác với collection User (Người dùng hệ thống) trong MongoDB.
 */
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
