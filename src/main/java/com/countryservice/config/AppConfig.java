package com.countryservice.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Application-level Spring configuration.
 *
 * RestTemplate is declared here as a singleton bean rather than instantiated with
 * 'new' inside service classes. This makes the HTTP client injectable, swappable,
 * and — critically — mockable in unit and integration tests without spinning up a
 * real network connection.
 *
 * Timeouts are configured defensively so a slow upstream (restcountries.com) cannot
 * block threads indefinitely.
 */
@Configuration
public class AppConfig {

    /** Maximum time (ms) to wait for the TCP connection to the upstream API. */
    private static final int CONNECT_TIMEOUT_MS = 3_000;

    /** Maximum time (ms) to wait for data after the connection is established. */
    private static final int READ_TIMEOUT_MS    = 5_000;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS))
                .setReadTimeout(Duration.ofMillis(READ_TIMEOUT_MS))
                .build();
    }
}
