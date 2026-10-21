package co.edu.kh.spring_authorization_server.dto.response;

import java.util.Set;
import java.util.UUID;

public record UserResponse (
        UUID id,
        String username,
        boolean enabled,
        Set<String> roles
){
}
