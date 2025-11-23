package com.example.deployer.tokens;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class TokenAuthenticationProvider implements AuthenticationProvider {

    private final TokenRepo repo;

    public TokenAuthenticationProvider(TokenRepo repo) {
        this.repo = repo;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String token = authentication.getName(); // Eingabe aus Login-Feld

        OneTimeToken ott = repo.findByToken(token)
            .orElseThrow(() -> new BadCredentialsException("Token ungültig"));

        LocalDate today = LocalDate.now();

        if (ott.getValidUntil() == null) {
            // Erstes Einlösen → gültig bis heute
            ott.setValidUntil(today);
            repo.save(ott);
        }

        if (!today.equals(ott.getValidUntil())) {
            throw new BadCredentialsException("Token abgelaufen");
        }

        return new UsernamePasswordAuthenticationToken(token, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

}

