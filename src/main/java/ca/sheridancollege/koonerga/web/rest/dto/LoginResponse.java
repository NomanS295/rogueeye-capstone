package ca.sheridancollege.koonerga.web.rest.dto;
import lombok.*;
@Getter @AllArgsConstructor
public class LoginResponse {
    private String token;
    private boolean mustChangePassword;
    private String piIdentifier;
    private String role;
}

