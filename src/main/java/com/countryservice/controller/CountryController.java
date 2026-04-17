package com.countryservice.controller;

import com.countryservice.model.CountryResponse;
import com.countryservice.service.CountryService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the Country data API.
 *
 * The controller layer is intentionally thin: it handles HTTP concerns only
 * (routing, status codes, serialisation) and delegates all business logic to
 * {@link CountryService}. This separation keeps both layers independently
 * testable and makes it straightforward to add new transports (e.g. GraphQL)
 * without touching business logic.
 */
@RestController
@RequestMapping(value = "/countries", produces = MediaType.APPLICATION_JSON_VALUE)
public class CountryController {

    private final CountryService countryService;

    public CountryController(CountryService countryService) {
        this.countryService = countryService;
    }

    /**
     * Fetch enriched country data for a given ISO 3166-1 alpha-2 country code.
     *
     * <p>Error mapping (handled by {@link com.countryservice.exception.GlobalExceptionHandler}):</p>
     * <ul>
     *   <li>400 – code is not exactly two letters</li>
     *   <li>404 – code is valid but no country exists for it</li>
     *   <li>502 – upstream REST Countries API is unavailable</li>
     * </ul>
     *
     * @param code two-letter ISO country code, case-insensitive (e.g. "US", "de")
     * @return 200 with {@link CountryResponse} body
     */
    @GetMapping("/{code}")
    public ResponseEntity<CountryResponse> getCountry(@PathVariable String code) {
        CountryResponse response = countryService.getCountryByCode(code);
        return ResponseEntity.ok(response);
    }
}
