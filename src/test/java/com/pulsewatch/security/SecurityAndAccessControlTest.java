package com.pulsewatch.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsewatch.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityAndAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        // User login
        LoginRequest userLogin = new LoginRequest("user@pulsewatch.io", "User@123456");
        MvcResult uRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userLogin)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode uJson = objectMapper.readTree(uRes.getResponse().getContentAsString());
        userToken = uJson.get("data").get("accessToken").asText();

        // Admin login
        LoginRequest adminLogin = new LoginRequest("admin@pulsewatch.io", "Admin@123456");
        MvcResult aRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode aJson = objectMapper.readTree(aRes.getResponse().getContentAsString());
        adminToken = aJson.get("data").get("accessToken").asText();
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint returns 401 Unauthorized")
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/monitors"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Regular USER accessing ADMIN endpoint returns 403 Forbidden")
    void testPrivilegeEscalationPrevention() throws Exception {
        mockMvc.perform(get("/api/v1/admin/overview")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN role access to ADMIN endpoint succeeds")
    void testAdminAccessAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/admin/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").isNumber());
    }

    @Test
    @DisplayName("Public status page endpoint accessible without any authentication")
    void testPublicEndpointAccess() throws Exception {
        mockMvc.perform(get("/api/v1/public/status/system-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").isNotEmpty());
    }
}
