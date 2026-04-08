package com.example.deployer;

import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    // -------------------------------------------------------------------------
    // Public endpoints (no authentication required)
    // -------------------------------------------------------------------------

    @Test
    void actuatorHealth_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorInfo_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorMetrics_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerUiIndex_isPublic() throws Exception {
        // Springdoc redirects /swagger-ui.html → /swagger-ui/index.html
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void openApiDocs_isPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void loginPage_isPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // Protected endpoints (unauthenticated → redirect to /login)
    // -------------------------------------------------------------------------

    @Test
    void repos_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/repos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void apiDeployments_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/api/repo1/deployments"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void gitStatus_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/git/repo1/status"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // -------------------------------------------------------------------------
    // Security headers
    // -------------------------------------------------------------------------

    @Test
    void response_hasXFrameOptionsDeny() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void response_hasXContentTypeOptionsNosniff() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    // -------------------------------------------------------------------------
    // CSRF: POST without token → 403
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser
    void post_withoutCsrfToken_returns403() throws Exception {
        mockMvc.perform(post("/git/repo1/fetch"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void post_withCsrfToken_passesCsrfCheck() throws Exception {
        // Request still fails (repo not found → 400/404), but not because of CSRF
        mockMvc.perform(post("/git/repo1/fetch").with(csrf()))
                .andExpect(status().is(not(403)));
    }
}
