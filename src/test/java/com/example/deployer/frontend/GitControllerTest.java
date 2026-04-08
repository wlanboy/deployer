package com.example.deployer.frontend;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.deployer.configuration.GitConfig;

@SpringBootTest
@AutoConfigureMockMvc
class GitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GitConfig gitConfig;

    // -------------------------------------------------------------------------
    // Invalid branch names → 400, gitConfig must never be consulted
    // -------------------------------------------------------------------------

    @Test
    void checkout_branchStartingWithDash_returns400() throws Exception {
        mockMvc.perform(post("/git/repo1/checkout/-injected-option")
                        .with(csrf()).with(user("user")))
                .andExpect(status().isBadRequest());

        verify(gitConfig, never()).findById("repo1");
    }

    @Test
    void checkout_branchWithDoubleDot_returns400() throws Exception {
        mockMvc.perform(post("/git/repo1/checkout/feat..escape")
                        .with(csrf()).with(user("user")))
                .andExpect(status().isBadRequest());

        verify(gitConfig, never()).findById("repo1");
    }

    @Test
    void checkout_branchStartingWithDot_returns400() throws Exception {
        // Leading dot is not \w → regex rejects it
        mockMvc.perform(post("/git/repo1/checkout/.hidden")
                        .with(csrf()).with(user("user")))
                .andExpect(status().isBadRequest());

        verify(gitConfig, never()).findById("repo1");
    }

    // -------------------------------------------------------------------------
    // Valid branch names → validation passes (404 comes from unknown repo)
    // -------------------------------------------------------------------------

    @Test
    void checkout_simpleBranchName_validationPasses() throws Exception {
        // Mock returns empty Optional by default → 404; we verify it's not 400
        mockMvc.perform(post("/git/repo1/checkout/main")
                        .with(csrf()).with(user("user")))
                .andExpect(status().isNotFound());
    }

    @Test
    void checkout_branchWithHyphenAndDot_validationPasses() throws Exception {
        mockMvc.perform(post("/git/repo1/checkout/release-1.0")
                        .with(csrf()).with(user("user")))
                .andExpect(status().isNotFound());
    }
}
