package com.alzheimer.user;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

  private final UserService service;

  public UserController(UserService service) {
    this.service = service;
  }

  @PostMapping
  public User createUser(@RequestBody User user) {
    return service.save(user);
  }

  @GetMapping
  public List<User> getAllUsers() {
    return service.findAll();
  }

  @GetMapping("/{id}")
  public Optional<User> getUserById(@PathVariable Long id) {
    return service.findById(id);
  }

  @GetMapping("/email/{email}")
  public User getUserByEmail(@PathVariable String email) {
    return service.findByEmail(email);
  }

  @GetMapping("/active")
  public List<User> getActiveUsers() {
    return service.findActiveUsers();
  }

  @GetMapping("/count")
  public long getUsersCount() {
    return service.countAllUsers();
  }

  @GetMapping("/active/count")
  public long getActiveUsersCount() {
    return service.countActiveUsers();
  }

  @GetMapping("/role/{role}/count")
  public long getUsersCountByRole(@PathVariable UserRole role) {
    return service.countUsersByRole(role);
  }

  @GetMapping("/role/{role}")
  public List<User> getUsersByRole(@PathVariable UserRole role) {
    return service.findUsersByRole(role);
  }

  @PutMapping("/{id}")
  public User updateUser(@PathVariable Long id, @RequestBody User user) {
    return service.update(id, user);
  }

  @DeleteMapping("/{id}")
  public void deleteUser(@PathVariable Long id) {
    service.delete(id);
  }
}

