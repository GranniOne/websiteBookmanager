package com.booksmanager.websitebooksmanager.security;

import com.booksmanager.websitebooksmanager.views.LoginView;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity



public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // 1. Let Vaadin inject its view config securely
        http.with(VaadinSecurityConfigurer.vaadin(), configurer ->
                configurer.loginView(LoginView.class)
        );

        // 2. Map structural assets explicitly
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/VAADIN/**",
                        "/frontend/**",
                        "/icons/**",
                        "/images/**",
                        "/*.css",
                        "/*.js"
                ).permitAll()
                .requestMatchers("/api/**").authenticated()

                // FIX: Using "/**" instead of anyRequest() keeps the builder open
                // and allows Vaadin's custom configuration to merge without crashing!
                .requestMatchers("/**").permitAll()
        );

        http.headers(headers ->
                headers.frameOptions(frame -> frame.sameOrigin())
        );

        return http.build();
    }


    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails userDetails = User
                .withUsername("appUser")
                .password("$2a$12$f9Rvu07Oog7kUM7MLTlWl.5gV6ANrO8VgAJjU1lh83bKbJ9SXmb0K") // Vaadin#Secure42
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(userDetails);
    }

    @Bean
    PasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

}