package com.novelosoftware.expenses.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        // Account-scoped expense listing must precede /accounts/{id}
                        .requestMatchers(HttpMethod.GET, "/accounts/*/expenses").hasAuthority("SCOPE_read:expenses")
                        // Expense read operations
                        .requestMatchers(HttpMethod.GET, "/expenses", "/expenses/{id}").hasAuthority("SCOPE_read:expenses")
                        // Expense write operations
                        .requestMatchers(HttpMethod.POST, "/expenses", "/expenses/bulk").hasAuthority("SCOPE_write:expenses")
                        .requestMatchers(HttpMethod.PUT, "/expenses/{id}").hasAuthority("SCOPE_write:expenses")
                        .requestMatchers(HttpMethod.DELETE, "/expenses/{id}").hasAuthority("SCOPE_write:expenses")
                        // Account read operations
                        .requestMatchers(HttpMethod.GET, "/accounts/{id}", "/accounts/user/{userId}").hasAuthority("SCOPE_read:accounts")
                        // Account write operations
                        .requestMatchers(HttpMethod.POST, "/accounts").hasAuthority("SCOPE_write:accounts")
                        .requestMatchers(HttpMethod.PUT, "/accounts/{id}").hasAuthority("SCOPE_write:accounts")
                        .requestMatchers(HttpMethod.DELETE, "/accounts/{id}").hasAuthority("SCOPE_write:accounts")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new ScopeClaimConverter()))
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .csrf(csrf -> csrf.disable())
                .build();
    }
}
