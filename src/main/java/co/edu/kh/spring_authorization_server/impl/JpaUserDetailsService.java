package co.edu.kh.spring_authorization_server.impl;

import co.edu.kh.spring_authorization_server.entity.UserEntity;
import co.edu.kh.spring_authorization_server.mapper.UserMapper;
import co.edu.kh.spring_authorization_server.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JpaUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(()-> new UsernameNotFoundException("User not found" + username));
        return userMapper.toUserDetails(user);
    }
}
