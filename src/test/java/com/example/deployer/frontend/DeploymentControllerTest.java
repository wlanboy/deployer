package com.example.deployer.frontend;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.deployer.actions.PlaybookService;
import com.example.deployer.configuration.GitConfig;
import com.example.deployer.entities.Deployment;
import com.example.deployer.entities.DeploymentItem;
import com.example.deployer.entities.DeploymentItemRepository;
import com.example.deployer.entities.DeploymentRepository;

@SpringBootTest
@AutoConfigureMockMvc
class DeploymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeploymentRepository deploymentRepo;

    @MockitoBean
    private DeploymentItemRepository itemRepo;

    @MockitoBean
    private PlaybookService playbookService;

    @MockitoBean
    private GitConfig gitConfig;

    private GitConfig.Repo repo;
    private Deployment deployment;

    @BeforeEach
    void setUp() {
        repo = new GitConfig.Repo();
        repo.setId("repo1");
        repo.setPath("/tmp");
        repo.setPlaybooksDir("playbooks");
        repo.setInventoriesDir("inventories");

        deployment = new Deployment("dep1", "Test Deployment", "repo1");

        when(gitConfig.findById("repo1")).thenReturn(Optional.of(repo));
        when(deploymentRepo.findById("dep1")).thenReturn(Optional.of(deployment));
    }

    // -------------------------------------------------------------------------
    // Path traversal prevention in resolveFileWithYamlExtensions
    // -------------------------------------------------------------------------

    @Test
    void requiresBecome_dotDotInPlaybookName_returns400() throws Exception {
        DeploymentItem item = new DeploymentItem(1L, "dep1", "repo1",
                "../../../etc/passwd", "inventory", null, null, null);
        when(itemRepo.findByDeploymentId("dep1")).thenReturn(List.of(item));

        mockMvc.perform(get("/api/repo1/deployment/dep1/requires-become").with(user("user")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresBecome_absolutePathInPlaybookName_returns400() throws Exception {
        DeploymentItem item = new DeploymentItem(1L, "dep1", "repo1",
                "/etc/passwd", "inventory", null, null, null);
        when(itemRepo.findByDeploymentId("dep1")).thenReturn(List.of(item));

        mockMvc.perform(get("/api/repo1/deployment/dep1/requires-become").with(user("user")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresBecome_subdirSlashInPlaybookName_returns400() throws Exception {
        DeploymentItem item = new DeploymentItem(1L, "dep1", "repo1",
                "sub/playbook", "inventory", null, null, null);
        when(itemRepo.findByDeploymentId("dep1")).thenReturn(List.of(item));

        mockMvc.perform(get("/api/repo1/deployment/dep1/requires-become").with(user("user")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresBecome_validPlaybookName_returns200() throws Exception {
        DeploymentItem item = new DeploymentItem(1L, "dep1", "repo1",
                "my-playbook", "inventory", null, null, null);
        when(itemRepo.findByDeploymentId("dep1")).thenReturn(List.of(item));
        when(playbookService.requiresBecome(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/repo1/deployment/dep1/requires-become").with(user("user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiresBecome").value(false));
    }

    // -------------------------------------------------------------------------
    // Cross-repo ownership check
    // -------------------------------------------------------------------------

    @Test
    void getDeployment_wrongRepoId_returns400() throws Exception {
        // Deployment belongs to "repo1" but request uses "repo2"
        mockMvc.perform(get("/api/repo2/deployment/dep1").with(user("user")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getDeployment_correctRepoId_returns200() throws Exception {
        when(itemRepo.findByDeploymentId("dep1")).thenReturn(List.of());

        mockMvc.perform(get("/api/repo1/deployment/dep1").with(user("user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("dep1"))
                .andExpect(jsonPath("$.name").value("Test Deployment"));
    }

    // -------------------------------------------------------------------------
    // Not found handling
    // -------------------------------------------------------------------------

    @Test
    void getDeployment_unknownId_returns404() throws Exception {
        when(deploymentRepo.findById("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/repo1/deployment/unknown").with(user("user")))
                .andExpect(status().isNotFound());
    }
}
