package com.novelosoftware.expenses.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Resolves the identity of the authenticated caller for ownership decisions.
 *
 * <p>The canonical identity is the {@code sub} claim of the validated JWT. A resource's
 * {@code createdBy} is compared against this value to determine ownership.
 *
 * <p>This is a Spring component so services can inject it and unit tests can mock it,
 * keeping the security-context lookup out of method signatures.
 */
@Component
public class CurrentUser {

    /**
     * Returns the {@code sub} claim of the currently authenticated JWT.
     *
     * @return the caller's subject identifier
     * @throws AccessDeniedException if there is no authenticated JWT or it carries no {@code sub}
     */
    public String requireSubject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("No authenticated user");
        }
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new AccessDeniedException("Authenticated token has no subject");
        }
        return subject;
    }
}
