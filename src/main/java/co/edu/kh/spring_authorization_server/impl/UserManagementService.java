package co.edu.kh.spring_authorization_server.impl;

import co.edu.kh.spring_authorization_server.mapper.UserMapper;
import co.edu.kh.spring_authorization_server.repository.UserRepository;

import co.edu.kh.spring_authorization_server.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManagementService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toUserResponse)
                .toList();
    }
}
