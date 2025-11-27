package ca.sheridancollege.koonerga.service;

import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import ca.sheridancollege.koonerga.repository.UserRepository;
import ca.sheridancollege.koonerga.security.JwtService;
import ca.sheridancollege.koonerga.web.rest.dto.LoginRequest;
import ca.sheridancollege.koonerga.web.rest.dto.LoginResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public ca.sheridancollege.koonerga.domain.User getByUsername(String username) {
        return repo.findByUsername(username).orElse(null);
    }

    public LoginResponse login(LoginRequest req) {
        var user = repo.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!encoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // Generate token
        String token = jwtService.generateToken(user, Map.of());

        // Build response
        return new LoginResponse(
                token,
                user.isMustChangePassword(),
                user.getPiIdentifier(),
                user.getRole()
        );
    }


    public void changePassword(String username, String oldRaw, String newRaw) {
        var u = repo.findByUsername(username)
                     .orElseThrow(() -> new IllegalArgumentException(
                         "User with username '" + username + "' not found"));

        if (!encoder.matches(oldRaw, u.getPassword())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }

        u.setPassword(encoder.encode(newRaw));
        u.setMustChangePassword(false);
        repo.save(u);
    }
}
