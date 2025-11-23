package com.example.deployer.tokens;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final TokenRepo repo;
    private final Path tokenFile;
    private final int tokenCount;

    public TokenService(TokenRepo repo,
            @Value("${security.tokens.count}") int tokenCount,
            @Value("${security.tokens.file}") String tokenFile) {
        this.repo = repo;
        this.tokenFile = Paths.get(tokenFile);
        this.tokenCount = tokenCount;
    }

    private List<String> getTokens() {
        return repo.findAll()
                .stream()
                .map(OneTimeToken::getToken).toList();
    }

    private void writeTokensToFile() throws IOException {
        List<String> tokens = getTokens();
        Files.write(tokenFile, tokens, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public void cleanupExpiredTokens() {
        LocalDate today = LocalDate.now();

        repo.findAll().stream()
                .filter(t -> t.getValidUntil() != null && !t.getValidUntil().equals(today))
                .collect(Collectors.toList()).forEach(repo::delete);

        long currentCount = repo.count();

        for (int i = 0; i < tokenCount - currentCount; i++) {
            OneTimeToken newToken = new OneTimeToken();
            newToken.setToken(generateRandomToken());
            newToken.setValidUntil(today);
            repo.save(newToken);
        }

        try {
            writeTokensToFile();
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Schreiben der token.txt", e);
        }
    }

    private String generateRandomToken() {
        return java.util.UUID.randomUUID().toString().substring(0, 12);
    }
}
