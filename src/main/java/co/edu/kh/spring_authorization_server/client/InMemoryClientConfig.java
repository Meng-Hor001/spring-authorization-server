package co.edu.kh.spring_authorization_server.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

@Configuration
public class InMemoryClientConfig {

    @Bean
    public RegisteredClientRepository registeredClientRepository(
            PasswordEncoder passwordEncoder
    ){
        RegisteredClient serviceClient = RegisteredClient
                .withId("service-client")
                .clientId("service-client")
                .clientSecret(passwordEncoder.encode("service-secret"))
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC
                )
                .authorizationGrantType(
                        AuthorizationGrantType.CLIENT_CREDENTIALS
                )
                .scope("api.read")
                .scope("api.write")
                .build();
        return new InMemoryRegisteredClientRepository(serviceClient);
    }
}
