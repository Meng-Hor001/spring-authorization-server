package co.edu.kh.spring_authorization_server.controller;

import co.edu.kh.spring_authorization_server.impl.UserManagementService;
import co.edu.kh.spring_authorization_server.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserManagementService userManagementService;

    @GetMapping
    public List<UserResponse> getUsers() {
        return userManagementService.getUsers();
    }
}
