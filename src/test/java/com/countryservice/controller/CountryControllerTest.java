package com.countryservice.controller;

import com.countryservice.exception.CountryNotFoundException;
import com.countryservice.exception.GlobalExceptionHandler;
import com.countryservice.exception.InvalidCountryCodeException;
import com.countryservice.exception.UpstreamApiException;
import com.countryservice.model.CountryResponse;
import com.countryservice.service.CountryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for {@link CountryController}.
 *
 * {@code @WebMvcTest} loads only the web layer (controller + exception handler),
 * so the service is mocked. This keeps these tests fast and focused purely on
 * HTTP behaviour (routing, status codes, response shape).
 */
@WebMvcTest(CountryController.class)
@Import(GlobalExceptionHandler.class)
class CountryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CountryService countryService;

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /countries/US → 200 with correct JSON body")
    void getCountry_validCode_returns200() throws Exception {
        CountryResponse response = new CountryResponse(
                "US", "United States of America", "Washington, D.C.",
                "Americas", List.of("USD"), List.of("English"),
                List.of("CAN", "MEX"), "LARGE");

        when(countryService.getCountryByCode("US")).thenReturn(response);

        mockMvc.perform(get("/countries/US").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.countryCode").value("US"))
                .andExpect(jsonPath("$.name").value("United States of America"))
                .andExpect(jsonPath("$.capital").value("Washington, D.C."))
                .andExpect(jsonPath("$.region").value("Americas"))
                .andExpect(jsonPath("$.currencies[0]").value("USD"))
                .andExpect(jsonPath("$.languages[0]").value("English"))
                .andExpect(jsonPath("$.borders[0]").value("CAN"))
                .andExpect(jsonPath("$.sizeCategory").value("LARGE"));
    }

    // -------------------------------------------------------------------------
    // Error paths
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /countries/USA (3 letters) → 400")
    void getCountry_invalidCode_returns400() throws Exception {
        when(countryService.getCountryByCode("USA"))
                .thenThrow(new InvalidCountryCodeException("USA"));

        mockMvc.perform(get("/countries/USA").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /countries/ZZ (unknown code) → 404")
    void getCountry_unknownCode_returns404() throws Exception {
        when(countryService.getCountryByCode("ZZ"))
                .thenThrow(new CountryNotFoundException("ZZ"));

        mockMvc.perform(get("/countries/ZZ").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Upstream failure → 502")
    void getCountry_upstreamFailure_returns502() throws Exception {
        when(countryService.getCountryByCode("DE"))
                .thenThrow(new UpstreamApiException("Upstream unavailable"));

        mockMvc.perform(get("/countries/DE").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    @Test
    @DisplayName("Lowercase code is passed through to the service unchanged")
    void getCountry_lowercaseCode_delegatesToService() throws Exception {
        CountryResponse response = new CountryResponse(
                "DE", "Germany", "Berlin", "Europe",
                List.of("EUR"), List.of("German"), List.of(), "LARGE");

        when(countryService.getCountryByCode("de")).thenReturn(response);

        mockMvc.perform(get("/countries/de").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode").value("DE"));
    }
}
