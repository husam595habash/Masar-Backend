package com.masar.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * Spring Boot does not create a RestClient.Builder for us here, so we provide one.
     * It is prototype: a builder is mutable, so every class that asks gets its own fresh one.
     */
    @Bean
    @Scope("prototype")
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
