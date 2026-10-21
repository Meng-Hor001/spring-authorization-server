# Spring authorization server

Java 25, Spring Boot 4.1.1, Spring Security 7.1, Gradle, PostgreSQL, Flyway and MapStruct.

This project follows [Spring's official authorization-server configuration](https://docs.spring.io/spring-security/reference/servlet/oauth2/authorization-server/getting-started.html).
The [original reference](https://docs.spring.io/spring-authorization-server/reference/overview.html) describes the standalone 1.x line; this Boot 4 project uses Spring Security 7's APIs.

## Project structure

Java packages under `src/main/java/co/edu/kh/spring_authorization_server`:

```text
spring_authorization_server/
├── bootstrap/       Development user initialization
├── config/          Security chains, OAuth JDBC persistence, clients and signing keys
├── controller/      Registration and admin HTTP endpoints
├── dto/
│   ├── request/     Validated API input
│   └── response/    API output without password hashes
├── entity/          JPA database entities
├── mapper/          MapStruct entity, DTO and security-principal mappings
├── repository/      Spring Data database access
├── security/        UserDetails principal
├── service/         Registration, user management and database-backed login
└── SpringAuthorizationServerApplication.java
```

Controllers call services. Services apply business rules and use repositories for persistence and mappers for conversion. Registration handles duplicate database writes in the service after the failed transaction rolls back. Configuration and development initialization wire these layers together.

Current endpoints remain `POST /api/v1/users/register` and `GET /api/admin/users`. Admin access uses HTTP Basic and requires `ROLE_ADMIN`. Flyway migrations stay in `src/main/resources/db/migration`; moving Java packages does not change database tables.

## Your flow mapped to the project

| Step | Implementation |
| --- | --- |
| 1–2 Project and dependency | `build.gradle` |
| 3 Port 9000 and issuer | `application.yaml` |
| 4 Authorization endpoints | `config/AuthorizationServerSecurityConfig.java`, chain order 1 |
| 5 Service client | `config/JdbcClientConfig.java`, `service-client`, `api.read`, `api.write` |
| 6 JWT signing | `config/SigningKeyConfig.java`, RSA and public JWK endpoint |
| 7 Client credentials | curl below; also covered by automated tests |
| 8 Login | `config/SecurityConfig.java`, `JpaUserDetailsService`, `UserMapper`, password encoder |
| 9 Login client | `login-client`, authorization code + PKCE + rotating refresh tokens |
| 10–11 Login and refresh testing | Manual flow below |
| 12 Resource server | Separate API configuration example below |
| 13 Persistence | JPA users, JDBC clients/authorizations/consents, Flyway migrations |
| 14 Deployment preparation | Environment configuration, PKCS12 signing key, production HTTPS profile |

The project already uses database-backed clients instead of the diagram's initial in-memory clients. Existing client rows are preserved on startup; changing an environment secret does not rotate an existing database client secret. Update the encoded secret through your controlled client-management process.

## Run locally

Create a local PostgreSQL database named `authorization_db` first. Flyway creates the `authorization_server` schema and tables. The account must have permission to create them.

```sh
export DB_PASSWORD='your-local-database-password'
# Optional: DB_USERNAME and DB_URL (include ?currentSchema=authorization_server).
./gradlew bootRun --args='--spring.profiles.active=dev'
```

The dev profile seeds user `user` with password `ChangeThisLocalPassword`. Override `DEMO_USER_PASSWORD` before the first run. Existing users are preserved. Dev client secrets are `service-secret` and `login-secret`. These defaults apply only to development.

Before the first local startup, create the persistent signing key:

```sh
bash scripts/setup-dev-signing-key.sh
```

The dev profile loads `.local/signing.p12` with the local-only password `local-signing-password`. The setup script creates a 3072-bit RSA key and leaves an existing file unchanged. This directory is ignored by Git. Keep the file across restarts so unexpired tokens can still be verified. Run the application from the project root. To use another key, set `SIGNING_KEY_PATH`, `SIGNING_KEY_PASSWORD`, and optionally `SIGNING_KEY_ALIAS` consistently for setup and startup. A missing or unreadable configured key fails startup instead of silently replacing it.

After switching from the previous temporary key, restart the authorization server and obtain fresh tokens once. Subsequent restarts reuse the saved key. This local setup does not configure deployment or HTTPS.

```sh
curl --fail-with-body -u service-client:service-secret \
  -d grant_type=client_credentials -d scope=api.read \
  http://localhost:9000/oauth2/token
curl --fail http://localhost:9000/.well-known/openid-configuration
curl --fail http://localhost:9000/oauth2/jwks
```

Expect an access token, Bearer token type and expiry. This grant does not issue refresh tokens. A wrong secret returns 401; an unregistered scope returns `invalid_scope`. Send credentials with HTTP Basic, as configured on both clients.

## Authorization code + PKCE, then refresh

`login-client` is a confidential server-side client. Keep its secret on the backend, never in browser or mobile code. Its exact registered callback is `http://127.0.0.1:8080/callback`; override `LOGIN_REDIRECT_URI` before its first registration if needed.

Generate a verifier, challenge and state in a terminal:

```sh
VERIFIER=$(openssl rand -base64 48 | tr '+/' '-_' | tr -d '=\n')
CHALLENGE=$(printf '%s' "$VERIFIER" | openssl dgst -sha256 -binary | openssl base64 -A | tr '+/' '-_' | tr -d '=')
STATE=$(openssl rand -hex 16)
printf 'http://localhost:9000/oauth2/authorize?response_type=code&client_id=login-client&redirect_uri=http%%3A%%2F%%2F127.0.0.1%%3A8080%%2Fcallback&scope=openid%%20profile%%20api.read&code_challenge=%s&code_challenge_method=S256&state=%s\n' "$CHALLENGE" "$STATE"
```

Open the printed URL, log in, and approve consent. For this manual test, a callback server is optional: copy `code` from the browser's redirected URL even if the callback page cannot load. Check that its `state` matches the value above. A real client must validate state automatically and handle the callback. Exchange the short-lived, single-use code in the same terminal:

```sh
CODE='paste-code-here'
curl --fail-with-body -u login-client:login-secret \
  -d grant_type=authorization_code --data-urlencode "code=$CODE" \
  --data-urlencode 'redirect_uri=http://127.0.0.1:8080/callback' \
  --data-urlencode "code_verifier=$VERIFIER" http://localhost:9000/oauth2/token

REFRESH_TOKEN='paste-refresh-token-here'
curl --fail-with-body -u login-client:login-secret \
  -d grant_type=refresh_token --data-urlencode "refresh_token=$REFRESH_TOKEN" \
  http://localhost:9000/oauth2/token
```

Keep the newly returned refresh token after each refresh; reuse is disabled. An incorrect verifier or reused code must fail. `openid` requests also produce an ID token; use the access token to call APIs.

## Connect a separate resource server

In your API project, add Boot's `spring-boot-starter-security-oauth2-resource-server` dependency. Set:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9000
```

Use this security bean in that API project (not in this authorization server):

```java
@Bean
SecurityFilterChain api(HttpSecurity http) throws Exception {
    return http
        .securityMatcher("/api/**")
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(csrf -> csrf.disable()) // This chain accepts bearer tokens, not cookie login.
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.GET, "/api/**").hasAuthority("SCOPE_api.read")
            .anyRequest().hasAuthority("SCOPE_api.write"))
        .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
        .build();
}
```

Imports are from `org.springframework.context.annotation`, `org.springframework.http`, `org.springframework.security.config`, `org.springframework.security.config.annotation.web.builders`, `org.springframework.security.config.http`, and `org.springframework.security.web`. Add an appropriate fallback chain if the API has routes outside `/api/**`.

Call an actual API route with `Authorization: Bearer <access_token>`. Verify missing/invalid tokens return 401 and insufficient scopes return 403. The resource server validates signature, expiry and issuer through discovery. For multiple API audiences, add audience claims and matching audience validation.

See [Spring's JWT resource-server reference](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).

## Production preparation

Generate and securely store a signing key outside the repository:

```sh
keytool -genkeypair -alias auth-signing -keyalg RSA -keysize 3072 \
  -storetype PKCS12 -keystore /secure/path/signing.p12 -validity 3650
```

Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVICE_CLIENT_SECRET`, `LOGIN_CLIENT_SECRET`, `LOGIN_REDIRECT_URI`, `SIGNING_KEY_PATH`, `SIGNING_KEY_PASSWORD`, and `AUTH_ISSUER` (the public HTTPS URL). The signing file must contain the private key and certificate under alias `auth-signing`, or set `SIGNING_KEY_ALIAS`. The key password must match the store password.

For the supplied direct-HTTPS production profile, also set `TLS_KEY_STORE` (for example `file:/secure/path/tls.p12`), `TLS_KEY_STORE_PASSWORD`, and optionally `TLS_KEY_ALIAS` (default `auth-server-tls`). Use a certificate trusted by your clients. Signing and HTTPS certificates have separate purposes.

```sh
./gradlew bootJar
java -jar build/libs/spring_authorization_server-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Production requires a signing key and does not seed a demo user. Provision users with the configured password encoder and `ROLE_` authorities before login. Deployment itself, user provisioning UI, key rotation with overlapping verification keys, and an external resource-server deployment are not included.

## Verification

```sh
./gradlew test
```

Tests use an isolated H2 database in PostgreSQL compatibility mode, run Flyway migrations, exercise the real HTTP token endpoint verify mapped login-principal persistence, exercise login with PKCE and refresh-token rotation, and check persistent signing-key loading. The login test uses previously granted consent. They do not contact your configured PostgreSQL server. Run the manual login/refresh and resource-server checks above against your deployment; H2 does not replace PostgreSQL deployment validation.
