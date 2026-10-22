package co.edu.kh.spring_authorization_server.repository;

import co.edu.kh.spring_authorization_server.entity.AuthorizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthorizationRepository extends JpaRepository<AuthorizationEntity, String> {
    Optional<AuthorizationEntity> findByState(String state);

    Optional<AuthorizationEntity> findByAuthorizationCodeValue(String code);

    Optional<AuthorizationEntity> findByAccessTokenValue(String token);

    Optional<AuthorizationEntity> findByRefreshTokenValue(String token);

    Optional<AuthorizationEntity> findByOidcIdTokenValue(String token);

    Optional<AuthorizationEntity> findByUserCodeValue(String code);

    Optional<AuthorizationEntity> findByDeviceCodeValue(String code);

    @Query("""
            select authorization
            from AuthorizationEntity authorization
            where authorization.state = :token
               or authorization.authorizationCodeValue = :token
               or authorization.accessTokenValue = :token
               or authorization.refreshTokenValue = :token
               or authorization.oidcIdTokenValue = :token
               or authorization.userCodeValue = :token
               or authorization.deviceCodeValue = :token
            """)
    Optional<AuthorizationEntity> findByToken(@Param("token") String token);
}
