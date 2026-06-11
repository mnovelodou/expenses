package com.novelosoftware.expenses.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ScopeClaimConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractScopes(jwt).stream()
                .map(scope -> (GrantedAuthority) new SimpleGrantedAuthority("SCOPE_" + scope))
                .toList();
        return new JwtAuthenticationToken(jwt, authorities);
    }

    private Collection<String> extractScopes(Jwt jwt) {
        Object scp = jwt.getClaim("scp");
        if (scp instanceof String s && !s.isBlank()) {
            return Arrays.asList(s.split(" "));
        }
        if (scp instanceof Collection<?> c) {
            return c.stream().map(Object::toString).toList();
        }
        String scope = jwt.getClaim("scope");
        if (scope != null && !scope.isBlank()) {
            return Arrays.asList(scope.split(" "));
        }
        return Collections.emptyList();
    }
}
