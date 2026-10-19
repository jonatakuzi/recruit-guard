package com.jonatakuzi.recruitguard.web;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Tests the HTTP layer: routes, JSON shape, and validation errors. */
@SpringBootTest
@AutoConfigureMockMvc
class ScanControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void scanReturnsScoredReport() throws Exception {
        String body = """
                {"sender": "recruiter@0cient.com",
                 "body": "Please send your bank account and routing number today."}
                """;
        mvc.perform(post("/api/v1/scan").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(60))
                .andExpect(jsonPath("$.verdict").value("LIKELY_SCAM"))
                .andExpect(jsonPath("$.findings[*].ruleId").value(hasItem("lookalike-characters")));
    }

    @Test
    void scanRejectsMissingFields() throws Exception {
        mvc.perform(post("/api/v1/scan").contentType(MediaType.APPLICATION_JSON).content("{\"sender\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void domainCheckFlagsLookalike() throws Exception {
        mvc.perform(get("/api/v1/domains/check").param("domain", "hr@ocient.co"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.domain").value("ocient.co"))
                .andExpect(jsonPath("$.officialCompanyDomain").value(false))
                .andExpect(jsonPath("$.findings[0].ruleId").value("suffix-swap"));
    }

    @Test
    void domainCheckRecognizesOfficialDomain() throws Exception {
        mvc.perform(get("/api/v1/domains/check").param("domain", "careers.ocient.com"))
                .andExpect(jsonPath("$.officialCompanyDomain").value(true))
                .andExpect(jsonPath("$.findings").isEmpty());
    }

    @Test
    void domainCheckRequiresParameter() throws Exception {
        mvc.perform(get("/api/v1/domains/check")).andExpect(status().isBadRequest());
    }

    @Test
    void listsKnownCompanies() throws Exception {
        mvc.perform(get("/api/v1/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasItem("Northrop Grumman")));
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(jsonPath("$.status").value("UP"));
    }
}
