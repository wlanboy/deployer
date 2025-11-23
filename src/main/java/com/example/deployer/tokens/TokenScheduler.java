package com.example.deployer.tokens;

import java.io.IOException;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TokenScheduler {

    private final TokenService tokenService;

    public TokenScheduler(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    /** Beim Start */
    @EventListener(ApplicationReadyEvent.class)
    public void initTokens() throws IOException {
        tokenService.cleanupExpiredTokens();
    }

    /** Jede Stunde */
    @Scheduled(cron = "0 0 * * * *") 
    public void refreshTokens() throws IOException {
        tokenService.cleanupExpiredTokens();
    }
}
