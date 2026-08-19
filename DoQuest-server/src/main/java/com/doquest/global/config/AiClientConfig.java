package com.doquest.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class AiClientConfig {

    @Value("${ai.service.base-url}")
    private String baseUrl;

    @Value("${ai.service.connect-timeout-ms}")
    private int connectTimeout;

    @Value("${ai.service.read-timeout-ms}")
    private int readTimeout;

    @Bean
    public RestClient aiRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        factory.setReadTimeout(Duration.ofMillis(readTimeout));

        return builder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}