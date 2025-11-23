package com.example.deployer.tokens;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRepo extends JpaRepository<OneTimeToken, Long> {
    Optional<OneTimeToken> findByToken(String token);
    void deleteByToken(String token);
}

