package com.pulsewatch.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsewatch.dto.LoginRequest;
import com.pulsewatch.dto.MonitorCreateRequest;
import com.pulsewatch.entity.MonitorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class MonitorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        LoginRequest loginReq = new LoginRequest("user@pulsewatch.io", "User@123456");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        userToken = json.get("data").get("accessToken").asText();
    }

    @Test
    @DisplayName("Create Monitor -> Pause -> Run Test Check -> Fetch Results")
    void testMonitorLifecycle() throws Exception {
        // 1. Create Monitor
        MonitorCreateRequest createReq = new MonitorCreateRequest();
        createReq.setName("Integration Probe");
        createReq.setUrl("https://api.github.com");
        createReq.setType(MonitorType.HTTPS);
        createReq.setIntervalSeconds(30);
        createReq.setTimeoutMs(5000);
        createReq.setExpectedStatusCodes("200,301,302");

        MvcResult createResult = mockMvc.perform(post("/api/v1/monitors")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn();

        JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long monitorId = createJson.get("data").get("id").asLong();

        // 2. Pause Monitor
        mockMvc.perform(post("/api/v1/monitors/" + monitorId + "/pause")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.currentStatus").value("PAUSED"));

        // 3. Resume Monitor
        mockMvc.perform(post("/api/v1/monitors/" + monitorId + "/resume")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));

        // 4. Test Check Execution
        mockMvc.perform(post("/api/v1/monitors/" + monitorId + "/test-check")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.responseTimeMs").isNumber());

        // 5. Check Results History
        mockMvc.perform(get("/api/v1/monitors/" + monitorId + "/results")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
