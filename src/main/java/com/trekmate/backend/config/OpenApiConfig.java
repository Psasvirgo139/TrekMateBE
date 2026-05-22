package com.trekmate.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI trekmateOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TrekMate API")
                        .description("REST API for TrekMate - Trekking Tour Guide Platform")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("TrekMate Team")
                                .email("support@trekmate.com"))
                        .license(new License().name("Apache 2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8080/api").description("Local Development")));
    }
}
