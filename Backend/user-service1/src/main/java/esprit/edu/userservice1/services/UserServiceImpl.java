package esprit.edu.userservice1.services;

import esprit.edu.userservice1.entities.role;
import esprit.edu.userservice1.entities.user;
import esprit.edu.userservice1.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository repo;

    public UserServiceImpl(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public user create(user u) {
        if (repo.existsByEmail(u.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        return repo.save(u);
    }

    @Override
    public user getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public user getByEmail(String email) {
        return repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public user findByEmail(String email) {
        return repo.findByEmail(email).orElse(null);
    }

    @Override
    public List<user> getAll() {
        return repo.findAll();
    }

    @Override
    public user update(Long id, user updated) {
        user existing = getById(id);

        if (updated.getFullName() != null) existing.setFullName(updated.getFullName());
        if (updated.getEmail() != null) existing.setEmail(updated.getEmail());
        if (updated.getPassword() != null) existing.setPassword(updated.getPassword());
        if (updated.getRole() != null) existing.setRole(updated.getRole());
        if (updated.getPhotoUrl() != null) existing.setPhotoUrl(updated.getPhotoUrl());

        // ✅ Toujours mettre à jour le champ blocked
        existing.setBlocked(updated.isBlocked());

        return repo.save(existing);
    }

    @Override
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        repo.deleteById(id);
    }

    @Override
    public user updateRole(Long id, String newRole) {
        user u = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        u.setRole(role.valueOf(newRole.toUpperCase()));
        return repo.save(u);
    }

    @Override
    public user findByResetToken(String token) {
        return repo.findByResetToken(token);
    }
}