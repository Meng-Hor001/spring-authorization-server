package co.edu.kh.spring_authorization_server.mapper;

import co.edu.kh.spring_authorization_server.entity.UserEntity;
import co.edu.kh.spring_authorization_server.security.SecurityUser;

import co.edu.kh.spring_authorization_server.dto.request.RegisterRequest;
import co.edu.kh.spring_authorization_server.dto.response.RegisterResponse;
import co.edu.kh.spring_authorization_server.dto.response.UserResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.security.core.authority.SimpleGrantedAuthority;



@Mapper(
        componentModel = "spring",
        imports = SimpleGrantedAuthority.class
)
public interface UserMapper {
    @Mapping(target = "password", source = "passwordHash")
    @Mapping(
            target = "authorities",
            expression = "java(entity.getRoles().stream()" +
                    ".map(SimpleGrantedAuthority::new).toList())"
    )
    SecurityUser toSecurityUser(UserEntity entity);


    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "username", source = "request.username")
    @Mapping(target = "passwordHash", source = "encodedPassword")
    @Mapping(
            target = "roles",
            expression = "java(java.util.Set.of(\"ROLE_USER\"))"
    )
    UserEntity toEntity(RegisterRequest request, String encodedPassword);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "username", source = "username")
    RegisterResponse toRegisterResponse(UserEntity entity);


    UserResponse toUserResponse(UserEntity entity);

    default org.springframework.security.core.userdetails.User toUserDetails(UserEntity entity) {
        SecurityUser user = toSecurityUser(entity);
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), user.isEnabled(),
                true, true, true, user.getAuthorities());
    }
}
