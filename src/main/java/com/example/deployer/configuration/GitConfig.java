package com.example.deployer.configuration;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "git")
@Data
public class GitConfig {
    private List<Repo> repos;

    @Data
    public static class Repo {
        private String id;
        private String path;
        private String playbooksDir;
        private String inventoriesDir;
    }

    public Optional<Repo> findById(String id) {
        return repos.stream().filter(r -> r.getId().equals(id)).findFirst();
    }
}
