package com.alzheimer.medicalrecords.user;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository repository;
    private final RestTemplate restTemplate;

    @Value("${user-service.base-url:http://localhost:9090}")
    private String userServiceBaseUrl;

    public UserService(UserRepository repository, RestTemplate restTemplate) {
        this.repository = repository;
        this.restTemplate = restTemplate;
    }

    public User save(User user) {
        return repository.save(user);
    }

    public List<User> findAll() {
        List<User> local = repository.findAll();
        if (!local.isEmpty()) return local;
        // Local DB is empty — try to pull all users from user-service and sync them
        try {
            User[] remote = restTemplate.getForObject(userServiceBaseUrl + "/api/users", User[].class);
            if (remote != null && remote.length > 0) {
                for (User u : remote) {
                    if (repository.findById(u.getId()).isEmpty()) {
                        repository.save(u);
                    }
                }
                return repository.findAll();
            }
        } catch (Exception e) {
            log.warn("Could not sync users from user-service at {}: {}", userServiceBaseUrl, e.getMessage());
        }
        return local;
    }

    public Optional<User> findById(Long id) {
        Optional<User> local = repository.findById(id);
        if (local.isPresent()) return local;
        try {
            User remote = restTemplate.getForObject(
                userServiceBaseUrl + "/api/users/" + id, User.class);
            if (remote != null) {
                remote.setId(id);
                // A row with this email may already exist under a different ID —
                // reuse it instead of attempting a duplicate INSERT.
                if (remote.getEmail() != null) {
                    Optional<User> byEmail = repository.findByEmail(remote.getEmail());
                    if (byEmail.isPresent()) return byEmail;
                }
                return Optional.of(repository.save(remote));
            }
        } catch (Exception e) {
            log.warn("Could not fetch user id={} from user-service at {}: {}", id, userServiceBaseUrl, e.getMessage());
        }
        return Optional.empty();
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
