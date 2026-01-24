package com.zaphira.transaction.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI transactionServiceOpenAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("Transaction Service API")
                        .description("API de gestion des transactions pour Zaphira")
                        .version("1.0.0"));
    }
}


