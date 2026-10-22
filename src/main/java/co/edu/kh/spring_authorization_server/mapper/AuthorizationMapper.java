package co.edu.kh.spring_authorization_server.mapper;

import co.edu.kh.spring_authorization_server.entity.AuthorizationEntity;
import org.mapstruct.*;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

@Mapper(componentModel = "spring")
public abstract class AuthorizationMapper {
    private final JsonMapper jsonMapper = JsonMapper.builder()
            .addModules(SecurityJacksonModules.getModules(
                    AuthorizationMapper.class.getClassLoader()))
            .build();

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "registeredClientId", source = "registeredClientId")
    @Mapping(target = "principalName", source = "principalName")
    @Mapping(
            target = "authorizationGrantType",
            source = "authorizationGrantType.value"
    )
    @Mapping(
            target = "authorizedScopes",
            expression = "java(String.join(\",\", authorization.getAuthorizedScopes()))"
    )
    @Mapping(
            target = "attributes",
            expression = "java(writeMap(authorization.getAttributes()))"
    )
    @Mapping(
            target = "state",
            expression = "java(authorization.getAttribute(\"state\"))"
    )
    public abstract AuthorizationEntity toEntity(
            OAuth2Authorization authorization
    );

    @AfterMapping
    protected void mapTokens(
            OAuth2Authorization authorization,
            @MappingTarget AuthorizationEntity entity
    ) {
        copyToken(
                authorization.getToken(OAuth2AuthorizationCode.class),
                entity::setAuthorizationCodeValue,
                entity::setAuthorizationCodeIssuedAt,
                entity::setAuthorizationCodeExpiresAt,
                entity::setAuthorizationCodeMetadata
        );

        copyToken(
                authorization.getAccessToken(),
                entity::setAccessTokenValue,
                entity::setAccessTokenIssuedAt,
                entity::setAccessTokenExpiresAt,
                entity::setAccessTokenMetadata
        );

        if (authorization.getAccessToken() != null) {
            OAuth2AccessToken accessToken =
                    authorization.getAccessToken().getToken();

            entity.setAccessTokenType(accessToken.getTokenType().getValue());
            entity.setAccessTokenScopes(
                    String.join(",", accessToken.getScopes()));
        }

        copyToken(
                authorization.getToken(OidcIdToken.class),
                entity::setOidcIdTokenValue,
                entity::setOidcIdTokenIssuedAt,
                entity::setOidcIdTokenExpiresAt,
                entity::setOidcIdTokenMetadata
        );

        copyToken(
                authorization.getRefreshToken(),
                entity::setRefreshTokenValue,
                entity::setRefreshTokenIssuedAt,
                entity::setRefreshTokenExpiresAt,
                entity::setRefreshTokenMetadata
        );

        copyToken(
                authorization.getToken(OAuth2UserCode.class),
                entity::setUserCodeValue,
                entity::setUserCodeIssuedAt,
                entity::setUserCodeExpiresAt,
                entity::setUserCodeMetadata
        );

        copyToken(
                authorization.getToken(OAuth2DeviceCode.class),
                entity::setDeviceCodeValue,
                entity::setDeviceCodeIssuedAt,
                entity::setDeviceCodeExpiresAt,
                entity::setDeviceCodeMetadata
        );
    }

    public OAuth2Authorization toAuthorization(
            AuthorizationEntity entity,
            RegisteredClient client
    ) {
        if (entity == null) {
            return null;
        }

        Assert.notNull(client, "Registered client must exist");
        Assert.isTrue(
                client.getId().equals(entity.getRegisteredClientId()),
                "Registered client does not match authorization"
        );

        var builder = OAuth2Authorization.withRegisteredClient(client)
                .id(entity.getId())
                .principalName(entity.getPrincipalName())
                .authorizationGrantType(
                        new AuthorizationGrantType(
                                entity.getAuthorizationGrantType()))
                .authorizedScopes(
                        StringUtils.commaDelimitedListToSet(
                                entity.getAuthorizedScopes()))
                .attributes(attributes ->
                        attributes.putAll(readMap(entity.getAttributes())));

        if (StringUtils.hasText(entity.getState())) {
            builder.attribute(
                    OAuth2ParameterNames.STATE,
                    entity.getState()
            );
        }

        if (StringUtils.hasText(entity.getAuthorizationCodeValue())) {
            var code = new OAuth2AuthorizationCode(
                    entity.getAuthorizationCodeValue(),
                    entity.getAuthorizationCodeIssuedAt(),
                    entity.getAuthorizationCodeExpiresAt()
            );

            builder.token(code, metadata ->
                    metadata.putAll(
                            readMap(entity.getAuthorizationCodeMetadata())));
        }

        if (StringUtils.hasText(entity.getAccessTokenValue())) {
            var accessToken = new OAuth2AccessToken(
                    accessTokenType(entity.getAccessTokenType()),
                    entity.getAccessTokenValue(),
                    entity.getAccessTokenIssuedAt(),
                    entity.getAccessTokenExpiresAt(),
                    StringUtils.commaDelimitedListToSet(
                            entity.getAccessTokenScopes())
            );

            builder.token(accessToken, metadata ->
                    metadata.putAll(
                            readMap(entity.getAccessTokenMetadata())));
        }

        if (StringUtils.hasText(entity.getOidcIdTokenValue())) {
            Map<String, Object> tokenMetadata =
                    readMap(entity.getOidcIdTokenMetadata());

            var idToken = new OidcIdToken(
                    entity.getOidcIdTokenValue(),
                    entity.getOidcIdTokenIssuedAt(),
                    entity.getOidcIdTokenExpiresAt(),
                    idTokenClaims(tokenMetadata)
            );

            builder.token(idToken, metadata ->
                    metadata.putAll(tokenMetadata));
        }

        if (StringUtils.hasText(entity.getRefreshTokenValue())) {
            var refreshToken = new OAuth2RefreshToken(
                    entity.getRefreshTokenValue(),
                    entity.getRefreshTokenIssuedAt(),
                    entity.getRefreshTokenExpiresAt()
            );

            builder.token(refreshToken, metadata ->
                    metadata.putAll(
                            readMap(entity.getRefreshTokenMetadata())));
        }

        if (StringUtils.hasText(entity.getUserCodeValue())) {
            var userCode = new OAuth2UserCode(
                    entity.getUserCodeValue(),
                    entity.getUserCodeIssuedAt(),
                    entity.getUserCodeExpiresAt()
            );

            builder.token(userCode, metadata ->
                    metadata.putAll(
                            readMap(entity.getUserCodeMetadata())));
        }

        if (StringUtils.hasText(entity.getDeviceCodeValue())) {
            var deviceCode = new OAuth2DeviceCode(
                    entity.getDeviceCodeValue(),
                    entity.getDeviceCodeIssuedAt(),
                    entity.getDeviceCodeExpiresAt()
            );

            builder.token(deviceCode, metadata ->
                    metadata.putAll(
                            readMap(entity.getDeviceCodeMetadata())));
        }

        return builder.build();
    }

    private <T extends OAuth2Token> void copyToken(
            OAuth2Authorization.Token<T> storedToken,
            Consumer<String> valueSetter,
            Consumer<Instant> issuedAtSetter,
            Consumer<Instant> expiresAtSetter,
            Consumer<String> metadataSetter
    ) {
        if (storedToken == null) {
            return;
        }

        T token = storedToken.getToken();

        valueSetter.accept(token.getTokenValue());
        issuedAtSetter.accept(token.getIssuedAt());
        expiresAtSetter.accept(token.getExpiresAt());
        metadataSetter.accept(writeMap(storedToken.getMetadata()));
    }

    protected String writeMap(Map<String, Object> value) {
        return jsonMapper.writeValueAsString(value);
    }

    private Map<String, Object> readMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }

        var type = jsonMapper.getTypeFactory()
                .constructMapType(Map.class, String.class, Object.class);

        return jsonMapper.readValue(json, type);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> idTokenClaims(
            Map<String, Object> metadata
    ) {
        Object claims = metadata.get(
                OAuth2Authorization.Token.CLAIMS_METADATA_NAME);

        Assert.isInstanceOf(
                Map.class,
                claims,
                "ID token metadata must contain claims"
        );

        return (Map<String, Object>) claims;
    }

    private OAuth2AccessToken.TokenType accessTokenType(String value) {
        if (OAuth2AccessToken.TokenType.BEARER.getValue()
                .equalsIgnoreCase(value)) {
            return OAuth2AccessToken.TokenType.BEARER;
        }

        throw new IllegalArgumentException(
                "Unsupported access token type: " + value);
    }
}
