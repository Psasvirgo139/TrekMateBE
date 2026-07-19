package com.trekmate.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableJpaAuditing
public class AppConfig {

    /**
     * RestTemplate dùng để gọi external APIs (Open-Meteo, Gemini).
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
