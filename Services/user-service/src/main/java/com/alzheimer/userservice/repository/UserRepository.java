package com.alzheimer.userservice.repository;

import com.alzheimer.userservice.entity.User;
import com.alzheimer.userservice.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByActive(Boolean active);
    List<User> findByRole(UserRole role);
}
