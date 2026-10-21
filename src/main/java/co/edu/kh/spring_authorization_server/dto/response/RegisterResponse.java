package co.edu.kh.spring_authorization_server.dto.response;

import java.util.UUID;

public record RegisterResponse (
        UUID id,
        String username
){
}
