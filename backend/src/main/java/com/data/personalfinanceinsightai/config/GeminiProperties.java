package com.data.personalfinanceinsightai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gemini")
@Getter
@Setter
public class GeminiProperties {

    private String apiKey;
    private String baseUrl;
    private String model = "gemini-2.5-flash"; 
    private String fallbackModels = "gemini-2.5-flash,gemini-3.5-flash,gemini-2.5-pro";
    private int timeoutSeconds = 15;
    private int maxRetries = 2;
}
