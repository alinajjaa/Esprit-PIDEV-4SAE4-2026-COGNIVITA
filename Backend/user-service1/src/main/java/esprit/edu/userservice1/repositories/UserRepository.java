package esprit.edu.userservice1.repositories;

import esprit.edu.userservice1.entities.user;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<user, Long> {

    Optional<user> findByEmail(String email);

    boolean existsByEmail(String email);
    user findByResetToken(String resetToken);

}
