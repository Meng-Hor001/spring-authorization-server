package co.edu.kh.spring_authorization_server.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Configuration
public class SigningKeyConfig {

    @Bean
    public RSAKey rsaKey(
            @Value("${app.signing-key.path:}") String path,
            @Value("${app.signing-key.password:}") String password,
            @Value("${app.signing-key.alias:auth-signing}") String alias,
            Environment environment) throws Exception {
        if (!path.isBlank()) {
            KeyStore store = KeyStore.getInstance("PKCS12");
            try (var input = Files.newInputStream(Path.of(path))) {
                store.load(input, password.toCharArray());
            }
            RSAKey key = RSAKey.load(store, alias, password.toCharArray());
            if (key == null || !key.isPrivate() || key.size() < 2048) {
                throw new IllegalStateException("Signing alias must contain an RSA private key of at least 2048 bits");
            }
            return key;
        }
        if (!environment.acceptsProfiles(Profiles.of("dev", "test"))
                || environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("Configure SIGNING_KEY_PATH and SIGNING_KEY_PASSWORD; ephemeral keys are only for dev/test");
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);

        KeyPair keyPair = generator.generateKeyPair();

        return new RSAKey.Builder(
                (RSAPublicKey) keyPair.getPublic()
        ).privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    @Bean
    public JWKSource<SecurityContext> jwtSource(RSAKey rsaKey){
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaKey) throws Exception{
        return NimbusJwtDecoder
                .withPublicKey(rsaKey.toRSAPublicKey())
                .build();
    }
}
