package co.edu.kh.spring_authorization_server.impl;

import co.edu.kh.spring_authorization_server.entity.AuthorizationConsentId;
import co.edu.kh.spring_authorization_server.mapper.AuthorizationConsentMapper;
import co.edu.kh.spring_authorization_server.repository.AuthorizationConsentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Repository
@RequiredArgsConstructor
@Transactional
public class JpaOAuth2AuthorizationConsentService implements OAuth2AuthorizationConsentService {

    private final AuthorizationConsentRepository consentRepository;
    private final AuthorizationConsentMapper mapper;

    @Override
    public void save(OAuth2AuthorizationConsent consent) {
        Assert.notNull(consent, "consent cannot be null");

        consentRepository.save(mapper.toEntity(consent));
    }

    @Override
    public void remove(OAuth2AuthorizationConsent consent) {
        Assert.notNull(consent, "consent cannot be null");

        var id = new AuthorizationConsentId(
                consent.getRegisteredClientId(),
                consent.getPrincipalName()
        );

        consentRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public OAuth2AuthorizationConsent findById(
            String registeredClientId,
            String principalName
    ) {
        Assert.hasText(
                registeredClientId,
                "registeredClientId cannot be empty"
        );
        Assert.hasText(
                principalName,
                "principalName cannot be empty"
        );

        var id = new AuthorizationConsentId(
                registeredClientId,
                principalName
        );

        return consentRepository.findById(id)
                .map(mapper::toConsent)
                .orElse(null);
    }
}
