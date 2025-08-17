package com.bxt.picturebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

@Configuration
@ConfigurationProperties(prefix = "aliyun.ai")
@Service
public class AliyunAiConfig {

    /**
     * yml 中的 aliyun.ai.apiKey
     */
    private String apiKey;

    // getter & setter
    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
