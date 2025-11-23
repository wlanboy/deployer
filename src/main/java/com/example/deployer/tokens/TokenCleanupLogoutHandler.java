package com.example.deployer.tokens;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class TokenCleanupLogoutHandler implements LogoutHandler {

    private final TokenRepo repo;

    public TokenCleanupLogoutHandler(TokenRepo repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public void logout(HttpServletRequest request,
                       HttpServletResponse response,
                       Authentication authentication) {
        if (authentication != null) {
            String token = authentication.getName();
            repo.deleteByToken(token);
        }
    }
}
