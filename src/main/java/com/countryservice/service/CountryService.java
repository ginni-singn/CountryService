package com.countryservice.service;

import com.countryservice.exception.CountryNotFoundException;
import com.countryservice.exception.InvalidCountryCodeException;
import com.countryservice.exception.UpstreamApiException;
import com.countryservice.model.CountryResponse;
import com.countryservice.model.RestCountryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Core service responsible for:
 *   1. Validating the incoming ISO country code.
 *   2. Calling the upstream REST Countries v3.1 API.
 *   3. Mapping the raw upstream payload to our {@link CountryResponse} DTO.
 *   4. Applying population-based size-categorisation business rules.
 *
 * <p>All external I/O (the RestTemplate call) is isolated in
 * {@link #fetchFromUpstream(String)} so that unit tests can mock it without
 * starting a real network connection.</p>
 */
@Service
public class CountryService {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /** Strict two-letter uppercase pattern; we normalise to uppercase before checking. */
    private static final Pattern ISO_ALPHA2_PATTERN = Pattern.compile("^[A-Za-z]{2}$");

    /** Population thresholds for size categorisation (defined in spec). */
    private static final long SMALL_MAX  = 1_000_000L;
    private static final long MEDIUM_MAX = 10_000_000L;

    // -------------------------------------------------------------------------
    // Dependencies — injected by Spring (not newed-up) so they are mockable
    // -------------------------------------------------------------------------

    private final RestTemplate restTemplate;

    /** Base URL of the upstream API; externalised so tests can override via @Value. */
    private final String restCountriesBaseUrl;

    public CountryService(RestTemplate restTemplate,
                          @Value("${rest.countries.base-url:https://restcountries.com/v3.1}")
                          String restCountriesBaseUrl) {
        this.restTemplate        = restTemplate;
        this.restCountriesBaseUrl = restCountriesBaseUrl;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Fetch enriched country data for the given ISO alpha-2 country code.
     *
     * @param code two-letter ISO 3166-1 alpha-2 code (case-insensitive)
     * @return populated {@link CountryResponse}
     * @throws InvalidCountryCodeException if {@code code} is not two letters
     * @throws CountryNotFoundException    if the upstream API returns 404
     * @throws UpstreamApiException        if the upstream API is unreachable or returns 5xx
     */
    public CountryResponse getCountryByCode(String code) {
        validateCode(code);

        // Normalise to uppercase so "us" and "US" behave identically.
        String upperCode = code.toUpperCase();

        RestCountryResponse[] upstream = fetchFromUpstream(upperCode);

        // The endpoint returns an array; we always take the first element.
        // A null/empty array from a 200 response would be a malformed upstream reply.
        if (upstream == null || upstream.length == 0) {
            throw new CountryNotFoundException(upperCode);
        }

        return mapToResponse(upstream[0], upperCode);
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    /**
     * Reject codes that are not exactly two letters.
     * We fail fast here rather than letting the upstream API return an ambiguous error.
     */
    void validateCode(String code) {
        if (code == null || !ISO_ALPHA2_PATTERN.matcher(code).matches()) {
            throw new InvalidCountryCodeException(code);
        }
    }

    // -------------------------------------------------------------------------
    // Upstream API call
    // -------------------------------------------------------------------------

    /**
     * Calls the REST Countries v3.1 /alpha/{code} endpoint.
     *
     * <p>Defensive error handling:</p>
     * <ul>
     *   <li>404 from upstream → {@link CountryNotFoundException} (our 404)</li>
     *   <li>Other 4xx/5xx     → {@link UpstreamApiException}        (our 502)</li>
     *   <li>Network timeout   → {@link UpstreamApiException}        (our 502)</li>
     * </ul>
     */
    RestCountryResponse[] fetchFromUpstream(String upperCode) {
        String url = restCountriesBaseUrl + "/alpha/" + upperCode;
        try {
            ResponseEntity<RestCountryResponse[]> response =
                    restTemplate.getForEntity(url, RestCountryResponse[].class);

            return response.getBody();

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new CountryNotFoundException(upperCode);
            }
            // Any other 4xx (e.g. 429 rate-limit) is an upstream problem from our perspective.
            throw new UpstreamApiException(
                    "Upstream API returned an unexpected status: " + ex.getStatusCode(), ex);

        } catch (ResourceAccessException ex) {
            // Covers connection refused, read timeout, DNS failure, etc.
            throw new UpstreamApiException(
                    "Unable to reach the upstream REST Countries API. Please try again later.", ex);
        }
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    /**
     * Maps a single upstream {@link RestCountryResponse} object to our clean
     * {@link CountryResponse} DTO, applying the population size-category rule.
     *
     * <p>Every upstream field access is null-safe because the public API occasionally
     * omits optional fields (e.g. island nations have no borders).</p>
     */
    CountryResponse mapToResponse(RestCountryResponse src, String upperCode) {
        CountryResponse response = new CountryResponse();

        response.setCountryCode(upperCode);

        // Prefer the official name; fall back to common name; guard against a null name object.
        if (src.getName() != null) {
            String official = src.getName().getOfficial();
            response.setName(official != null ? official : src.getName().getCommon());
        }

        // Capital is a list upstream; take the first entry when present.
        List<String> capitals = src.getCapital();
        if (capitals != null && !capitals.isEmpty()) {
            response.setCapital(capitals.get(0));
        }

        response.setRegion(src.getRegion());

        // Extract currency codes from the map keys (e.g. {"USD": {...}} → ["USD"]).
        response.setCurrencies(extractCurrencyCodes(src.getCurrencies()));

        // Extract language names from the map values (e.g. {"eng": "English"} → ["English"]).
        response.setLanguages(extractLanguageNames(src.getLanguages()));

        // Borders may be null for island nations — return an empty list in that case.
        response.setBorders(src.getBorders() != null ? src.getBorders() : Collections.emptyList());

        response.setSizeCategory(determineSizeCategory(src.getPopulation()));

        return response;
    }

    // -------------------------------------------------------------------------
    // Business rules
    // -------------------------------------------------------------------------

    /**
     * Classifies a country by population into SMALL, MEDIUM, or LARGE.
     *
     * Rule (from spec):
     *   < 1,000,000          → SMALL
     *   1,000,000–10,000,000 → MEDIUM
     *   > 10,000,000         → LARGE
     */
    String determineSizeCategory(long population) {
        if (population < SMALL_MAX) {
            return "SMALL";
        } else if (population <= MEDIUM_MAX) {
            return "MEDIUM";
        } else {
            return "LARGE";
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Returns currency codes (map keys) or an empty list when the field is absent. */
    private List<String> extractCurrencyCodes(Map<String, Object> currenciesMap) {
        if (currenciesMap == null || currenciesMap.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(currenciesMap.keySet());
    }

    /** Returns language names (map values) or an empty list when the field is absent. */
    private List<String> extractLanguageNames(Map<String, String> languagesMap) {
        if (languagesMap == null || languagesMap.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(languagesMap.values());
    }
}
