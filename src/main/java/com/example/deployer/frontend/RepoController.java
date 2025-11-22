package com.example.deployer.frontend;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.deployer.configuration.GitConfig;

@RestController
@RequestMapping("/repos")
public class RepoController {

    private final GitConfig gitConfig;

    public RepoController(GitConfig gitConfig) {
        this.gitConfig = gitConfig;
    }

    @GetMapping
    public List<GitConfig.Repo> listRepos() {
        return gitConfig.getRepos();
    }
}

