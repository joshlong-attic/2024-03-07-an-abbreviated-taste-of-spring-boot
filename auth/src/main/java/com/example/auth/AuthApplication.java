package com.example.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Set;

import static org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer.authorizationServer;

@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }

    @Bean
    PasswordEncoder myPasswordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    InMemoryUserDetailsManager myInMemoryUserDetailsManager(PasswordEncoder pw) {
        var users = Set.of(User
                .withUsername("jlong")
                .password(pw.encode("pw"))
                .roles("USER")
                .build());
        return new InMemoryUserDetailsManager(users);
    }

    @Bean
    SecurityFilterChain mySpringSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .with(authorizationServer(), as -> as.oidc(Customizer.withDefaults()))
                .authorizeHttpRequests(ae -> ae.anyRequest().authenticated())
                .formLogin(Customizer.withDefaults())
//				.oneTimeTokenLogin( c->c.tokenGenerationSuccessHandler())
//				.webAuthn(Customizer.withDefaults())
                .build();
    }


}
