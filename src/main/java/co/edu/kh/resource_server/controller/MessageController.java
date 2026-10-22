package co.edu.kh.resource_server.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @GetMapping
    public Map<String, String> message(
            @AuthenticationPrincipal Jwt jwt
            ){
        return Map.of("message", "You accessed a protected API!",
                "subject", jwt.getSubject()
        );
    }
}
