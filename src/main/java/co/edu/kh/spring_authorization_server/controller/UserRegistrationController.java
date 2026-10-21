package co.edu.kh.spring_authorization_server.controller;

import co.edu.kh.spring_authorization_server.impl.UserRegisterService;
import co.edu.kh.spring_authorization_server.dto.request.RegisterRequest;
import co.edu.kh.spring_authorization_server.dto.response.RegisterResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserRegistrationController {

    private final UserRegisterService userRegisterService;


    @PostMapping(
            value = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return userRegisterService.register(request);
    }
}
