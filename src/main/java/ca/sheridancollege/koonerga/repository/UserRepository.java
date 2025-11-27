package ca.sheridancollege.koonerga.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ca.sheridancollege.koonerga.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
