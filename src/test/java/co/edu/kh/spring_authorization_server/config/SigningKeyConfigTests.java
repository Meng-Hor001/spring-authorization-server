package co.edu.kh.spring_authorization_server.config;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SigningKeyConfigTests {
    @TempDir Path directory;

    @Test void productionCannotFallBackToEphemeralKeys() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("prod", "dev");
        assertThatThrownBy(() -> new SigningKeyConfig().rsaKey("", "", "auth-signing", environment))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void loadsSameSigningKeyAcrossRestarts() throws Exception {
        Path store = directory.resolve("signing.p12");
        var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                "-genkeypair", "-alias", "auth-signing", "-keyalg", "RSA", "-keysize", "2048",
                "-storetype", "PKCS12", "-keystore", store.toString(), "-storepass", "test-password",
                "-dname", "CN=Test", "-validity", "1", "-noprompt").redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        assertThat(process.waitFor()).withFailMessage(output).isZero();
        var config = new SigningKeyConfig();
        var environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        var first = config.rsaKey(store.toString(), "test-password", "auth-signing", environment);
        var second = config.rsaKey(store.toString(), "test-password", "auth-signing", environment);
        assertThat(first.isPrivate()).isTrue();
        assertThat(first.getKeyID()).isEqualTo(second.getKeyID());
        var signature = java.security.Signature.getInstance("SHA256withRSA");
        signature.initSign(first.toRSAPrivateKey());
        signature.update("restart-test".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        byte[] signed = signature.sign();
        signature.initVerify(second.toRSAPublicKey());
        signature.update("restart-test".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(signature.verify(signed)).isTrue();

        var now = java.time.Instant.now();
        var token = new com.nimbusds.jwt.SignedJWT(
                new com.nimbusds.jose.JWSHeader.Builder(com.nimbusds.jose.JWSAlgorithm.RS256)
                        .keyID(first.getKeyID()).build(),
                new com.nimbusds.jwt.JWTClaimsSet.Builder()
                        .subject("user")
                        .issueTime(java.util.Date.from(now))
                        .expirationTime(java.util.Date.from(now.plusSeconds(300)))
                        .build());
        token.sign(new com.nimbusds.jose.crypto.RSASSASigner(first));
        assertThat(new SigningKeyConfig().jwtDecoder(second).decode(token.serialize()).getSubject())
                .isEqualTo("user");
    }
}
