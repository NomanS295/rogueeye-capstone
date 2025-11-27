package ca.sheridancollege.koonerga.config;

import ca.sheridancollege.koonerga.domain.User;
import ca.sheridancollege.koonerga.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AuthDataLoader implements CommandLineRunner {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        seed("gagan", "pass123", "PI-GAGAN-01");
        seed("alex",  "pass123", "PI-ALEX-02");
        seed("noman", "pass123", "PI-NOMAN-03");
        seed("ava",   "pass123", "PI-AVA-04");
    }
    
    private void seed(String username, String rawPass, String piId) {
        var existingUser = repo.findByUsername(username).orElse(null);

        if (existingUser != null) {
            existingUser.setRole("USER");
            existingUser.setPiIdentifier(piId);
            repo.save(existingUser);
            return;
        }

        User user = User.builder()
                .username(username)
                .password(encoder.encode(rawPass))
                .mustChangePassword(true)
                .role("USER")
                .piIdentifier(piId)
                .build();

        repo.save(user);
    }

}
