package com.novelosoftware.expenses.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class CurrentUserTest {

    private final CurrentUser currentUser = new CurrentUser();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static Jwt jwtWithSubject(String subject) {
        var builder = Jwt.withTokenValue("token").header("alg", "none");
        if (subject != null) {
            builder.subject(subject);
        } else {
            builder.claim("dummy", "value");
        }
        return builder.build();
    }

    @Test
    void requireSubject_returnsSubjectFromJwt() {
        SecurityContextHolder.getContext().setAuthentication(
            new JwtAuthenticationToken(jwtWithSubject("auth0|abc123")));

        assertEquals("auth0|abc123", currentUser.requireSubject());
    }

    @Test
    void requireSubject_noAuthentication_throwsAccessDenied() {
        assertThrows(AccessDeniedException.class, currentUser::requireSubject);
    }

    @Test
    void requireSubject_principalNotJwt_throwsAccessDenied() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("user", "pw"));

        assertThrows(AccessDeniedException.class, currentUser::requireSubject);
    }

    @Test
    void requireSubject_missingSubject_throwsAccessDenied() {
        SecurityContextHolder.getContext().setAuthentication(
            new JwtAuthenticationToken(jwtWithSubject(null)));

        assertThrows(AccessDeniedException.class, currentUser::requireSubject);
    }
}
