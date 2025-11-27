
package ca.sheridancollege.koonerga.web.rest;

import ca.sheridancollege.koonerga.web.rest.dto.LoginRequest;
import ca.sheridancollege.koonerga.web.rest.dto.LoginResponse;
import ca.sheridancollege.koonerga.security.JwtService;
import ca.sheridancollege.koonerga.service.UserService;
import ca.sheridancollege.koonerga.web.rest.dto.ChangePasswordRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // 🔐 later restrict to frontend URL only
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
		this.jwtService = jwtService;
    }

    @PostMapping("/login" )
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(userService.login(req));
    }
    
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ChangePasswordRequest req) {

        String token = authHeader.substring(7); // remove "Bearer "
        String username = jwtService.extractUsername(token);

        userService.changePassword(username, req.getOldPassword(), req.getNewPassword());
        return ResponseEntity.ok().build();
    }
}

