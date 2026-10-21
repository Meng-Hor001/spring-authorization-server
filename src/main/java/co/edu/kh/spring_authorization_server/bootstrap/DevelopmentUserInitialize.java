package co.edu.kh.spring_authorization_server.bootstrap;

import co.edu.kh.spring_authorization_server.entity.UserEntity;
import co.edu.kh.spring_authorization_server.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Profile("dev")
public class DevelopmentUserInitialize implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public DevelopmentUserInitialize(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.demo-user-password}") String demoPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername("user")) {
            return;
        }

        UserEntity user = UserEntity.builder()
                .username("user")
                .passwordHash(passwordEncoder.encode(demoPassword))
                .roles(Set.of("ROLE_USER"))
                .build();

        userRepository.save(user);
    }
}
