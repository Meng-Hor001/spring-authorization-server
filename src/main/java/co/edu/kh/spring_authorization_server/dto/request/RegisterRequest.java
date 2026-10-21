package co.edu.kh.spring_authorization_server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 100)
        String username,

        @NotBlank
        @Size(min = 12, max = 64)
        String password
) {
}
