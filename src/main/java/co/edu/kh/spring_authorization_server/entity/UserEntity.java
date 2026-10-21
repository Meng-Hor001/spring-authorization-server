package co.edu.kh.spring_authorization_server.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(schema = "authorization_server", name = "app_users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            schema = "authorization_server",
            name = "app_user_roles",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "role", nullable = false, length = 50)
    private Set<String> roles = new HashSet<>();

    @Builder
    public UserEntity(
            String username,
            String passwordHash,
            Set<String> roles
    ) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.roles = new HashSet<>(roles);
    }
}
