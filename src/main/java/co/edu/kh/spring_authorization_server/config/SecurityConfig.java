package co.edu.kh.spring_authorization_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration
public class SecurityConfig {
    @Bean
    @Order(2)
    SecurityFilterChain login(HttpSecurity http) throws Exception{
        http.authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()
        ).formLogin(Customizer.withDefaults());
        return http.build();
    }



    @Bean
    AuthorizationServerSettings authorizationServerSettings(Environment environment){
        return AuthorizationServerSettings.builder()
                .issuer(environment.getRequiredProperty("app.issuer"))
                .build();
    }
}
