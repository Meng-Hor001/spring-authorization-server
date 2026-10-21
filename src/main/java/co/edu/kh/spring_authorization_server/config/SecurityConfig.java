package co.edu.kh.spring_authorization_server.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration
public class SecurityConfig {
    @Bean
    @Order(3)
    SecurityFilterChain login(HttpSecurity http) throws Exception{
        var registration = PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.POST, "/api/v1/users/register");

        return http
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(registration).permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(registration)
                )
                .formLogin(Customizer.withDefaults())
                .build();
    }


    @Bean
    @Order(2)
    SecurityFilterChain adminApi(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/admin/**")
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().hasRole("ADMIN")
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings(Environment environment){
        return AuthorizationServerSettings.builder()
                .issuer(environment.getRequiredProperty("spring.security.oauth2.authorizationserver.issuer"))
                .build();
    }
}
