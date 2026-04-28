package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sky.ai")
public class AiProperties {

    private String apiKey;
    private String apiUrl;
    private String model;
    private Double temperature = 0.7;
    private Integer maxTokens = 1000;
    private Integer timeout = 60;
}
