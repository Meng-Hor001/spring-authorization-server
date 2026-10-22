package co.edu.kh.spring_authorization_server.repository;

import co.edu.kh.spring_authorization_server.entity.AuthorizationConsentEntity;
import co.edu.kh.spring_authorization_server.entity.AuthorizationConsentId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorizationConsentRepository extends JpaRepository<AuthorizationConsentEntity, AuthorizationConsentId> {
}
