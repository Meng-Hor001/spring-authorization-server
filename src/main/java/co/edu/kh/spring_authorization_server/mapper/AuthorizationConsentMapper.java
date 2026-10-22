package co.edu.kh.spring_authorization_server.mapper;

import co.edu.kh.spring_authorization_server.entity.AuthorizationConsentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface AuthorizationConsentMapper {

    @Mapping(
            target = "authorities",
            expression = "java(joinAuthorities(consent))"
    )
    AuthorizationConsentEntity toEntity(OAuth2AuthorizationConsent consent);

    default OAuth2AuthorizationConsent toConsent(
            AuthorizationConsentEntity entity
    ) {
        if (entity == null) {
            return null;
        }

        var builder = OAuth2AuthorizationConsent.withId(
                entity.getRegisteredClientId(),
                entity.getPrincipalName()
        );

        StringUtils.commaDelimitedListToSet(entity.getAuthorities())
                .forEach(authority ->
                        builder.authority(
                                new SimpleGrantedAuthority(authority)));

        return builder.build();
    }

    default String joinAuthorities(OAuth2AuthorizationConsent consent) {
        return consent.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
    }
}
