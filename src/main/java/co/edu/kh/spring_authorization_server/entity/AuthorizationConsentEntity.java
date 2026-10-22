package co.edu.kh.spring_authorization_server.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@IdClass(AuthorizationConsentId.class)
@Table(
        schema = "authorization_server",
        name = "oauth2_authorization_consent"
)
public class AuthorizationConsentEntity {

    @Id
    @Column(name = "registered_client_id", nullable = false, length = 100)
    private String registeredClientId;

    @Id
    @Column(name = "principal_name", nullable = false, length = 200)
    private String principalName;

    @Column(nullable = false, length = 1000)
    private String authorities;
}
