package com.example.deployer.frontend;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.deployer.configuration.GitConfig;

@RestController
@RequestMapping("/git")
public class GitController {

    private final GitConfig gitConfig;

    public GitController(GitConfig gitConfig) {
        this.gitConfig = gitConfig;
    }

    @GetMapping("/{repoId}/status")
    public Map<String, String> gitStatus(@PathVariable String repoId) {
        GitConfig.Repo repo = gitConfig.findById(repoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repo not found"));
        File gitDir = new File(repo.getPath(), ".git");
        if (!gitDir.exists()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kein Git-Repository gefunden");
        }
        String output = runGitCommand(repo.getPath(), "git", "status");
        return Map.of("status", output);
    }

    @PostMapping("/{repoId}/checkout/{branch}")
    public Map<String, String> gitCheckout(@PathVariable String repoId, @PathVariable String branch) {
        GitConfig.Repo repo = gitConfig.findById(repoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repo not found"));
        File gitDir = new File(repo.getPath(), ".git");
        if (!gitDir.exists()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kein Git-Repository gefunden");
        }
        String fetchOutput = runGitCommand(repo.getPath(), "git", "fetch");
        String checkoutOutput = runGitCommand(repo.getPath(), "git", "checkout", branch);
        return Map.of("fetch", fetchOutput, "checkout", checkoutOutput);
    }

    @PostMapping("/{repoId}/fetch")
    public Map<String, String> gitFetch(@PathVariable String repoId) {
        GitConfig.Repo repo = gitConfig.findById(repoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repo not found"));
        String output = runGitCommand(repo.getPath(), "git", "fetch");
        return Map.of("fetch", output);
    }

    @PostMapping("/{repoId}/pull")
    public Map<String, String> gitPull(@PathVariable String repoId) {
        GitConfig.Repo repo = gitConfig.findById(repoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repo not found"));
        String output = runGitCommand(repo.getPath(), "git", "pull");
        return Map.of("pull", output);
    }

    private String runGitCommand(String repoPath, String... args) {
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(new File(repoPath));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fehler beim Ausführen von Git", e);
        }
    }
}
