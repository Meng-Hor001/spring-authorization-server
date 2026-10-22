package co.edu.kh.spring_authorization_server.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AuthorizationConsentId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String registeredClientId;
    private String principalName;


}
