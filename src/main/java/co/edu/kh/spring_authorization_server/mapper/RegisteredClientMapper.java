package co.edu.kh.spring_authorization_server.mapper;

import co.edu.kh.spring_authorization_server.entity.RegisteredClientEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Mapper(componentModel = "spring")
public abstract class RegisteredClientMapper {
    private final JsonMapper jsonMapper = JsonMapper.builder()
            .addModules(SecurityJacksonModules.getModules(
                    RegisteredClientMapper.class.getClassLoader()))
            .build();

    @Mapping(
            target = "clientIdIssuedAt",
            expression = "java(issuedAt(client))"
    )
    @Mapping(
            target = "clientAuthenticationMethods",
            expression = "java(authenticationMethods(client))"
    )
    @Mapping(
            target = "authorizationGrantTypes",
            expression = "java(grantTypes(client))"
    )
    @Mapping(
            target = "redirectUris",
            expression = "java(String.join(\",\", client.getRedirectUris()))"
    )
    @Mapping(
            target = "postLogoutRedirectUris",
            expression = "java(String.join(\",\", client.getPostLogoutRedirectUris()))"
    )
    @Mapping(
            target = "scopes",
            expression = "java(String.join(\",\", client.getScopes()))"
    )
    @Mapping(
            target = "clientSettings",
            expression = "java(writeSettings(client.getClientSettings().getSettings()))"
    )
    @Mapping(
            target = "tokenSettings",
            expression = "java(writeSettings(client.getTokenSettings().getSettings()))"
    )
    public abstract RegisteredClientEntity toEntity(RegisteredClient client);

    public RegisteredClient toRegisteredClient(RegisteredClientEntity entity) {
        if (entity == null) {
            return null;
        }
        return RegisteredClient.withId(entity.getId())
                .clientId(entity.getClientId())
                .clientIdIssuedAt(entity.getClientIdIssuedAt())
                .clientSecret(entity.getClientSecret())
                .clientSecretExpiresAt(entity.getClientSecretExpiresAt())
                .clientName(entity.getClientName())
                .clientAuthenticationMethods(methods ->
                        split(entity.getClientAuthenticationMethods())
                                .forEach(value -> methods.add(
                                        new ClientAuthenticationMethod(value))))
                .authorizationGrantTypes(grants ->
                        split(entity.getAuthorizationGrantTypes())
                                .forEach(value -> grants.add(
                                        new AuthorizationGrantType(value))))
                .redirectUris(uris ->
                        uris.addAll(split(entity.getRedirectUris())))
                .postLogoutRedirectUris(uris ->
                        uris.addAll(split(entity.getPostLogoutRedirectUris())))
                .scopes(scopes ->
                        scopes.addAll(split(entity.getScopes())))
                .clientSettings(
                        ClientSettings.withSettings(
                                        readSettings(entity.getClientSettings()))
                                .build())
                .tokenSettings(
                        TokenSettings.withSettings(
                                        readSettings(entity.getTokenSettings()))
                                .build())
                .build();
    }
    protected Instant issuedAt(RegisteredClient client) {
        return client.getClientIdIssuedAt() != null
                ? client.getClientIdIssuedAt()
                : Instant.now();
    }

    protected String authenticationMethods(RegisteredClient client) {
        return client.getClientAuthenticationMethods().stream()
                .map(ClientAuthenticationMethod::getValue)
                .collect(Collectors.joining(","));
    }

    protected String grantTypes(RegisteredClient client) {
        return client.getAuthorizationGrantTypes().stream()
                .map(AuthorizationGrantType::getValue)
                .collect(Collectors.joining(","));
    }
    protected String writeSettings(Map<String, Object> settings) {
        return jsonMapper.writeValueAsString(settings);
    }

    private Map<String, Object> readSettings(String json) {
        var type = jsonMapper.getTypeFactory()
                .constructMapType(Map.class, String.class, Object.class);

        return jsonMapper.readValue(json, type);
    }

    private Set<String> split(String value) {
        return StringUtils.commaDelimitedListToSet(value);
    }
}
