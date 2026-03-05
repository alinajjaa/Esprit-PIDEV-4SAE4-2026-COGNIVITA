package com.alzheimer.user;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

  private final UserRepository repository;

  public UserService(UserRepository repository) {
    this.repository = repository;
  }

  public User save(User user) {
    return repository.save(user);
  }

  public List<User> findAll() {
    return repository.findAll();
  }

  public Optional<User> findById(Long id) {
    return repository.findById(id);
  }

  public User findByEmail(String email) {
    return repository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
  }

  public List<User> findActiveUsers() {
    return repository.findByActive(true);
  }

  public List<User> findUsersByRole(UserRole role) {
    return repository.findByRole(role);
  }

  public long countAllUsers() {
    return repository.count();
  }

  public long countActiveUsers() {
    return repository.countByActive(true);
  }

  public long countUsersByRole(UserRole role) {
    return repository.countByRole(role);
  }

  public User update(Long id, User updatedUser) {
    User existing = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("User not found"));

    existing.setEmail(updatedUser.getEmail());
    existing.setFirstName(updatedUser.getFirstName());
    existing.setLastName(updatedUser.getLastName());
    existing.setPassword(updatedUser.getPassword());
    existing.setPhone(updatedUser.getPhone());
    existing.setRole(updatedUser.getRole());
    existing.setActive(updatedUser.getActive());

    return repository.save(existing);
  }

  public void delete(Long id) {
    repository.deleteById(id);
  }
}

