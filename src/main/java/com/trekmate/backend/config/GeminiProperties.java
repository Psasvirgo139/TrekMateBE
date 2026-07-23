package com.trekmate.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cau hinh ket noi Google Gemini API.
 * Doc tu application.yml voi prefix "gemini".
 *
 * Cau hinh trong application-dev.yml:
 *   gemini:
 *     api-key: ${GEMINI_API_KEY:}
 *     model: gemini-2.0-flash
 */
@Data
@Component
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    /** API key cua Google Gemini (lay tu env GEMINI_API_KEY) */
    private String apiKey;

    /** Ten model Gemini su dung (mac dinh: gemini-2.0-flash) */
    private String model = "gemini-2.0-flash";
}
