package com.countryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the CountryService Spring Boot application.
 *
 * This microservice exposes GET /countries/{code} to fetch enriched country data
 * from the public REST Countries v3.1 API and applies business rules
 * (e.g., population-based size categorisation).
 */
@SpringBootApplication
public class CountryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CountryServiceApplication.class, args);
    }
}
