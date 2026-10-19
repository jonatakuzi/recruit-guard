package com.jonatakuzi.recruitguard.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Title and description shown on the interactive API docs page (/docs). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI recruitGuardApi() {
        return new OpenAPI().info(new Info()
                .title("Recruit Guard API")
                .version("1.1.1")
                .description("Scores recruiter emails for job-scam red flags: look-alike domains, "
                        + "requests for sensitive data, payment schemes, and failed email authentication.")
                .license(new License().name("MIT")));
    }
}
