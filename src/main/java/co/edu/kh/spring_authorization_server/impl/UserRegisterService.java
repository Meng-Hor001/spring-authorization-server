package co.edu.kh.spring_authorization_server.impl;

import co.edu.kh.spring_authorization_server.entity.UserEntity;
import co.edu.kh.spring_authorization_server.mapper.UserMapper;
import co.edu.kh.spring_authorization_server.repository.UserRepository;

import co.edu.kh.spring_authorization_server.dto.request.RegisterRequest;
import co.edu.kh.spring_authorization_server.dto.response.RegisterResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;


@Service
@Validated
@RequiredArgsConstructor
public class UserRegisterService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final PlatformTransactionManager transactionManager;

    public RegisterResponse register(@Valid RegisterRequest request) {
        try {
            return new TransactionTemplate(transactionManager)
                    .execute(status -> registerInTransaction(request));
        } catch (DataIntegrityViolationException exception) {
            // Check after rollback: another request may have registered the same username.
            if (userRepository.existsByUsername(request.username())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Username already exists", exception);
            }
            throw exception;
        }
    }

    private RegisterResponse registerInTransaction(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username already exists"
            );
        }

        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must not exceed 72 UTF-8 bytes"
            );
        }
        String encodedPassword = passwordEncoder.encode(request.password());

        UserEntity user = userMapper.toEntity(request, encodedPassword);

        UserEntity savedUser = userRepository.saveAndFlush(user);

        return userMapper.toRegisterResponse(savedUser);
    }
}
