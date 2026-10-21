package co.edu.kh.spring_authorization_server.repository;

import co.edu.kh.spring_authorization_server.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID>{

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);
}
