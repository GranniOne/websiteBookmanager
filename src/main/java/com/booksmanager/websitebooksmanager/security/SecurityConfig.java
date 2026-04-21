package com.booksmanager.websitebooksmanager.security;

import com.booksmanager.websitebooksmanager.views.LoginView;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> {configurer.loginView(LoginView.class);});
        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers(
                    "/VAADIN/**",
                    "/frontend/**",
                    "/icons/**",
                    "/images/**",
                    "/*.css",
                    "/*.js",
                    "/book.jpg",
                    "/api/**"
            ).permitAll();





        });
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails userDetails = User.withDefaultPasswordEncoder()
                .username("appUser")
                .password("Vaadin#Secure42")
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(userDetails);
    }

}