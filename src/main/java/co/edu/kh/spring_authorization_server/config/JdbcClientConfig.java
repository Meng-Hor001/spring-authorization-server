package co.edu.kh.spring_authorization_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;

@Configuration
@DependsOnDatabaseInitialization
public class JdbcClientConfig {

    @Bean
    public RegisteredClientRepository registeredClientRepository(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${app.clients.service-secret}") String serviceSecret,
            @Value("${app.clients.login-secret}") String loginSecret,
            @Value("${app.clients.redirect-uri}") String redirectUri
    ) {
        var repository = new JdbcRegisteredClientRepository(jdbcTemplate);

        RegisteredClient serviceClient = RegisteredClient
                .withId("service-client")
                .clientId("service-client")
                .clientName("Service Client")
                .clientSecret(passwordEncoder.encode(serviceSecret))
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC
                )
                .authorizationGrantType(
                        AuthorizationGrantType.CLIENT_CREDENTIALS
                )
                .scope("api.read")
                .scope("api.write")
                .build();

        RegisteredClient loginClient = RegisteredClient
                .withId("login-client")
                .clientId("login-client")
                .clientName("Login Client")
                .clientSecret(passwordEncoder.encode(loginSecret))
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC
                )
                .authorizationGrantType(
                        AuthorizationGrantType.AUTHORIZATION_CODE
                )
                .authorizationGrantType(
                        AuthorizationGrantType.REFRESH_TOKEN
                )
                .redirectUri(redirectUri)
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("api.read")
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(true)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(5))
                        .refreshTokenTimeToLive(Duration.ofDays(1))
                        .reuseRefreshTokens(false)
                        .build())
                .build();

        if (repository.findByClientId(serviceClient.getClientId()) == null) {
            repository.save(serviceClient);
        }

        if (repository.findByClientId(loginClient.getClientId()) == null) {
            repository.save(loginClient);
        }

        return repository;
    }
}
