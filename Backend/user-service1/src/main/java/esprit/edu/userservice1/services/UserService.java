package esprit.edu.userservice1.services;

import esprit.edu.userservice1.entities.user;
import java.util.List;
public interface UserService {

    user create(user u);

    user getById(Long id);

    // throws if not found
    user getByEmail(String email);

    // returns null if not found (for register)
    user findByEmail(String email);

    List<user> getAll();

    user update(Long id, user u);

    void delete(Long id);
    user updateRole(Long id, String role); // ← ajoute cette ligne
    user findByResetToken(String token);

}
