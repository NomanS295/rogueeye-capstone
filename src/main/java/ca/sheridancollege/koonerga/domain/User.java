package ca.sheridancollege.koonerga.domain;



import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=64)
    private String username;

    @Column(nullable=false, length=120)
    private String password; // BCrypt hash

    @Column(nullable=false)
    private boolean mustChangePassword = true;

    @Column(nullable=false, length=32)
    private String role = "USER"; // or ADMIN

    @Column(length=128)
    private String piIdentifier; // which Pi belongs to this user
   
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security expects a list of roles as "ROLE_X"
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // no expiry logic for now
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // no lock logic for now
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // password never expires for now
    }

    @Override
    public boolean isEnabled() {
        return true; // always enabled
    }

}

