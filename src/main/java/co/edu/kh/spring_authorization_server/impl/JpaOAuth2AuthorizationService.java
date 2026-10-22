package co.edu.kh.spring_authorization_server.impl;

import co.edu.kh.spring_authorization_server.entity.AuthorizationEntity;
import co.edu.kh.spring_authorization_server.mapper.AuthorizationMapper;
import co.edu.kh.spring_authorization_server.repository.AuthorizationRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class JpaOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private final AuthorizationRepository authorizationRepository;
    private final RegisteredClientRepository registeredClientRepository;
    private final AuthorizationMapper mapper;
    @Override
    public void save(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");

        authorizationRepository.save(mapper.toEntity(authorization));
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");

        authorizationRepository.deleteById(authorization.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public OAuth2Authorization findById(String id) {
        Assert.hasText(id, "id cannot be empty");

        return authorizationRepository.findById(id)
                .map(this::toAuthorization)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public OAuth2Authorization findByToken(
            String token,
            OAuth2TokenType tokenType
    ) {
        Assert.hasText(token, "token cannot be empty");

        Optional<AuthorizationEntity> authorization;

        if (tokenType == null) {
            authorization = authorizationRepository.findByToken(token);
        } else {
            authorization = switch (tokenType.getValue()) {
                case "state" ->
                        authorizationRepository.findByState(token);
                case "code" ->
                        authorizationRepository.findByAuthorizationCodeValue(token);
                case "access_token" ->
                        authorizationRepository.findByAccessTokenValue(token);
                case "refresh_token" ->
                        authorizationRepository.findByRefreshTokenValue(token);
                case "id_token" ->
                        authorizationRepository.findByOidcIdTokenValue(token);
                case "user_code" ->
                        authorizationRepository.findByUserCodeValue(token);
                case "device_code" ->
                        authorizationRepository.findByDeviceCodeValue(token);
                default -> Optional.empty();
            };
        }

        return authorization
                .map(this::toAuthorization)
                .orElse(null);
    }

    private OAuth2Authorization toAuthorization(
            AuthorizationEntity entity
    ) {
        var client = registeredClientRepository.findById(
                entity.getRegisteredClientId());

        if (client == null) {
            throw new DataRetrievalFailureException(
                    "Registered client not found: "
                            + entity.getRegisteredClientId());
        }

        return mapper.toAuthorization(entity, client);
    }
}
