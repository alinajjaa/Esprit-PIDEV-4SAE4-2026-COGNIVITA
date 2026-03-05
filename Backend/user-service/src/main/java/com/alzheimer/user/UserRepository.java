package com.alzheimer.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  List<User> findByActive(Boolean active);

  List<User> findByRole(UserRole role);

  long countByActive(Boolean active);

  long countByRole(UserRole role);
}

