package co.edu.kh.spring_authorization_server;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.ActiveProfiles;
import co.edu.kh.spring_authorization_server.entity.UserEntity;
import co.edu.kh.spring_authorization_server.mapper.UserMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SpringAuthorizationServerApplicationTests {
    @LocalServerPort int port;
    @Autowired RegisteredClientRepository clients;
    @Autowired OAuth2AuthorizationService authorizations;
    @Autowired UserMapper mapper;

    private HttpResponse<String> token(String secret, String scope) throws Exception {
        String credentials = Base64.getEncoder().encodeToString(
                ("service-client:" + secret).getBytes(StandardCharsets.UTF_8));
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/oauth2/token"))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials&scope=" + scope))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void issuesTokenAndRejectsInvalidCredentialsAndScopes() throws Exception {
        var response = token("test-service-secret", "api.read");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("access_token", "api.read").doesNotContain("refresh_token");
        assertThat(token("wrong", "api.read").statusCode()).isEqualTo(401);
        var invalidScope = token("test-service-secret", "admin");
        assertThat(invalidScope.statusCode()).isEqualTo(400);
        assertThat(invalidScope.body()).contains("invalid_scope");
    }

    @Test void mappedLoginPrincipalSurvivesJdbcRoundTrip() {
        var user = mapper.toUserDetails(UserEntity.builder().username("alice")
                .passwordHash("{noop}test").roles(Set.of("ROLE_USER")).build());
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getPassword()).isEqualTo("{noop}test");
        assertThat(user.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        var client = clients.findByClientId("login-client");
        assertThat(client.getClientSettings().isRequireProofKey()).isTrue();
        assertThat(client.getTokenSettings().isReuseRefreshTokens()).isFalse();
        var principal = UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
        var authorization = OAuth2Authorization.withRegisteredClient(client)
                .principalName(user.getUsername()).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .attribute(Principal.class.getName(), principal).build();
        authorizations.save(authorization);
        var restored = authorizations.findById(authorization.getId());
        assertThat(restored).isNotNull();
        Principal restoredPrincipal = restored.getAttribute(Principal.class.getName());
        assertThat(restoredPrincipal.getName()).isEqualTo("alice");
    }
    @Autowired co.edu.kh.spring_authorization_server.repository.UserRepository users;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;
    @Autowired org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService consents;

    @Test void registrationPersistsMappedUserAndRejectsInvalidRequests() throws Exception {
        var client = HttpClient.newHttpClient();
        var request = registration("registered-user", "RegistrationPassword123!");
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(201);
        var json = new tools.jackson.databind.json.JsonMapper().readTree(response.body());
        var saved = users.findByUsername("registered-user").orElseThrow();
        assertThat(json.get("id").asText()).isEqualTo(saved.getId().toString());
        assertThat(json.get("username").asText()).isEqualTo(saved.getUsername());
        assertThat(response.body()).doesNotContain("password", "passwordHash");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getRoles()).containsExactly("ROLE_USER");
        assertThat(passwords.matches("RegistrationPassword123!", saved.getPasswordHash())).isTrue();
        assertThat(client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(409);
        assertThat(client.send(registration("invalid-user", "short"),
                HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(400);
        assertThat(client.send(registration("invalid-user", "ខ".repeat(30)),
                HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(400);
        assertThat(users.existsByUsername("invalid-user")).isFalse();
    }

    @Test void concurrentRegistrationReturnsOneCreatedAndOneConflict() throws Exception {
        var client = HttpClient.newHttpClient();
        var request = registration("concurrent-user", "RegistrationPassword123!");
        var first = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        var second = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        assertThat(java.util.List.of(first.get().statusCode(), second.get().statusCode()))
                .containsExactlyInAnyOrder(201, 409);
        assertThat(users.findAll().stream().filter(user -> user.getUsername().equals("concurrent-user")))
                .hasSize(1);
    }

    @Test void adminUserListRequiresAdminAndDoesNotExposePasswords() throws Exception {
        users.save(UserEntity.builder().username("list-admin")
                .passwordHash(passwords.encode("AdminPassword123!"))
                .roles(Set.of("ROLE_ADMIN", "ROLE_USER")).build());
        users.save(UserEntity.builder().username("list-member")
                .passwordHash(passwords.encode("MemberPassword123!"))
                .roles(Set.of("ROLE_USER")).build());
        assertThat(adminUsers(null).statusCode()).isEqualTo(401);
        assertThat(adminUsers("list-member:MemberPassword123!").statusCode()).isEqualTo(403);
        var response = adminUsers("list-admin:AdminPassword123!");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("list-admin", "list-member", "ROLE_ADMIN")
                .doesNotContain("password", "passwordHash", "AdminPassword123!");
    }

    private HttpRequest registration(String username, String password) {
        String json = new tools.jackson.databind.json.JsonMapper().writeValueAsString(
                java.util.Map.of("username", username, "password", password));
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/users/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
    }

    private HttpResponse<String> adminUsers(String credentials) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/admin/users"));
        if (credentials != null) {
            request.header("Authorization", "Basic " + Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        }
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void loginPkceAndRotatingRefreshTokens() throws Exception {
        users.save(UserEntity.builder().username("flow-user").passwordHash(passwords.encode("flow-password"))
                .roles(Set.of("ROLE_USER")).build());
        // Model a user who already granted consent; browser consent UI is covered by the manual guide.
        consents.save(org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent
                .withId("login-client", "flow-user").scope("openid").scope("profile").scope("api.read").build());
        var browser = HttpClient.newBuilder().cookieHandler(new java.net.CookieManager()).build();
        String base = "http://localhost:" + port;
        String verifier = "test-verifier-with-at-least-forty-three-characters-123456789";
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                java.security.MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        String authorize = base + "/oauth2/authorize?response_type=code&client_id=login-client"
                + "&redirect_uri=http%3A%2F%2F127.0.0.1%3A8080%2Fcallback&scope=openid%20profile%20api.read"
                + "&state=test-state&code_challenge_method=S256&code_challenge=" + challenge;
        assertThat(browser.send(HttpRequest.newBuilder(URI.create(authorize)).header("Accept", "text/html")
                .build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(302);
        var login = browser.send(HttpRequest.newBuilder(URI.create(base + "/login")).build(),
                HttpResponse.BodyHandlers.ofString());
        var csrf = java.util.regex.Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(login.body());
        assertThat(csrf.find()).isTrue();
        var loggedIn = browser.send(HttpRequest.newBuilder(URI.create(base + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("username=flow-user&password=flow-password&_csrf=" + csrf.group(1)))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(loggedIn.statusCode()).isEqualTo(302);
        var callback = browser.send(HttpRequest.newBuilder(URI.create(authorize)).build(), HttpResponse.BodyHandlers.ofString());
        String location = callback.headers().firstValue("location").orElseThrow();
        assertThat(location).startsWith("http://127.0.0.1:8080/callback?").contains("state=test-state");
        var codeMatcher = java.util.regex.Pattern.compile("[?&]code=([^&]+)").matcher(location);
        assertThat(codeMatcher.find()).isTrue();
        String body = "grant_type=authorization_code&code=" + codeMatcher.group(1)
                + "&redirect_uri=http%3A%2F%2F127.0.0.1%3A8080%2Fcallback&code_verifier=" + verifier;
        var tokens = loginToken(base, body);
        assertThat(tokens.statusCode()).isEqualTo(200);
        var json = new tools.jackson.databind.json.JsonMapper().readTree(tokens.body());
        assertThat(json.has("id_token")).isTrue();
        String refresh = json.get("refresh_token").asText();
        var refreshed = loginToken(base, "grant_type=refresh_token&refresh_token=" + refresh);
        assertThat(refreshed.statusCode()).isEqualTo(200);
        assertThat(new tools.jackson.databind.json.JsonMapper().readTree(refreshed.body())
                .get("refresh_token").asText()).isNotEqualTo(refresh);
        assertThat(loginToken(base, "grant_type=refresh_token&refresh_token=" + refresh).statusCode()).isEqualTo(400);
        assertThat(loginToken(base, body).statusCode()).isEqualTo(400);
    }

    private HttpResponse<String> loginToken(String base, String body) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base + "/oauth2/token"))
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                        "login-client:test-login-secret".getBytes(StandardCharsets.UTF_8)))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
