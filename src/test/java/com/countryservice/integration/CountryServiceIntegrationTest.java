package com.countryservice.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the full HTTP request → service → external API → response cycle.
 *
 * WireMock acts as a stub for the REST Countries API so:
 *   - Tests run offline (no real network calls).
 *   - We can simulate 404 and 5xx responses deterministically.
 *   - The real RestTemplate, ObjectMapper, and Spring MVC stack are exercised end-to-end.
 *
 * {@code @SpringBootTest} loads the full application context.
 * {@code @DynamicPropertySource} overrides {@code rest.countries.base-url} at runtime
 * so the service talks to WireMock instead of the real API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class CountryServiceIntegrationTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private MockMvc mockMvc;

    // -------------------------------------------------------------------------
    // WireMock lifecycle
    // -------------------------------------------------------------------------

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    /**
     * Override the base-url property so CountryService talks to WireMock, not the real API.
     * DynamicPropertySource runs before the Spring context is created.
     */
    @DynamicPropertySource
    static void overrideRestCountriesUrl(DynamicPropertyRegistry registry) {
        // Supplier form: evaluated after WireMock has started and the port is known.
        registry.add("rest.countries.base-url",
                () -> "http://localhost:" + wireMockServer.port());
    }

    // -------------------------------------------------------------------------
    // Stub helpers (inline JSON keeps the tests self-contained)
    // -------------------------------------------------------------------------

    /** Minimal valid response from the real REST Countries v3.1 API for "US". */
    private static final String US_STUB_RESPONSE = """
            [
              {
                "name": {
                  "common": "United States",
                  "official": "United States of America"
                },
                "cca2": "US",
                "capital": ["Washington, D.C."],
                "region": "Americas",
                "population": 331000000,
                "currencies": {
                  "USD": { "name": "United States dollar", "symbol": "$" }
                },
                "languages": { "eng": "English" },
                "borders": ["CAN", "MEX"]
              }
            ]
            """;

    /** Minimal valid response for Malta (island nation: no borders, small population). */
    private static final String MT_STUB_RESPONSE = """
            [
              {
                "name": {
                  "common": "Malta",
                  "official": "Republic of Malta"
                },
                "cca2": "MT",
                "capital": ["Valletta"],
                "region": "Europe",
                "population": 500000,
                "currencies": {
                  "EUR": { "name": "Euro", "symbol": "€" }
                },
                "languages": { "mlt": "Maltese", "eng": "English" },
                "borders": []
              }
            ]
            """;

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /countries/US → 200 with correct mapped fields")
    void getUS_returns200WithMappedFields() throws Exception {
        wireMockServer.stubFor(get(urlEqualTo("/alpha/US"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(US_STUB_RESPONSE)));

        mockMvc.perform(get("/countries/US").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode").value("US"))
                .andExpect(jsonPath("$.name").value("United States of America"))
                .andExpect(jsonPath("$.capital").value("Washington, D.C."))
                .andExpect(jsonPath("$.region").value("Americas"))
                .andExpect(jsonPath("$.currencies[0]").value("USD"))
                .andExpect(jsonPath("$.languages[0]").value("English"))
                .andExpect(jsonPath("$.sizeCategory").value("LARGE"));
    }

    @Test
    @DisplayName("Lowercase code 'us' is normalised and returns 200")
    void getLowercaseCode_normalises_returns200() throws Exception {
        wireMockServer.stubFor(get(urlEqualTo("/alpha/US"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(US_STUB_RESPONSE)));

        mockMvc.perform(get("/countries/us"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode").value("US"));
    }

    @Test
    @DisplayName("Island nation (MT) with no borders returns empty borders list")
    void getMalta_returnsEmptyBorders() throws Exception {
        wireMockServer.stubFor(get(urlEqualTo("/alpha/MT"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(MT_STUB_RESPONSE)));

        mockMvc.perform(get("/countries/MT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sizeCategory").value("SMALL"))
                .andExpect(jsonPath("$.borders").isArray())
                .andExpect(jsonPath("$.borders").isEmpty());
    }

    @Test
    @DisplayName("Upstream 404 → our 404 Not Found")
    void unknownCode_returns404() throws Exception {
        wireMockServer.stubFor(get(urlEqualTo("/alpha/ZZ"))
                .willReturn(aResponse().withStatus(404)));

        mockMvc.perform(get("/countries/ZZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Upstream 500 → our 502 Bad Gateway")
    void upstreamError_returns502() throws Exception {
        wireMockServer.stubFor(get(urlEqualTo("/alpha/DE"))
                .willReturn(aResponse().withStatus(500)));

        mockMvc.perform(get("/countries/DE"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    @Test
    @DisplayName("Three-letter code → 400 Bad Request (never reaches upstream)")
    void threeLetterCode_returns400() throws Exception {
        // No stub needed: validation rejects it before any HTTP call is made.
        mockMvc.perform(get("/countries/USA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Upstream connection refused → 502 Bad Gateway")
    void upstreamDown_returns502() throws Exception {
        // Override property to point at a port nothing is listening on.
        wireMockServer.stubFor(get(urlEqualTo("/alpha/FR"))
                .willReturn(aResponse()
                        .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

        mockMvc.perform(get("/countries/FR"))
                .andExpect(status().isBadGateway());
    }
}
