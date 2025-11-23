package com.example.deployer.tokens;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/token")
public class TokenController {

    private final TokenScheduler tokenScheduler;

    public TokenController(TokenScheduler tokenScheduler) {
        this.tokenScheduler = tokenScheduler;
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshTokens() throws IOException {
        tokenScheduler.refreshTokens();
        return ResponseEntity.ok("Tokens wurden bereinigt.");
    }
}
