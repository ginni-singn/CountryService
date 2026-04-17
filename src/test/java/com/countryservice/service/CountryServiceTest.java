package com.countryservice.service;

import com.countryservice.exception.CountryNotFoundException;
import com.countryservice.exception.InvalidCountryCodeException;
import com.countryservice.exception.UpstreamApiException;
import com.countryservice.model.CountryResponse;
import com.countryservice.model.RestCountryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CountryService}.
 *
 * RestTemplate is mocked — no real HTTP traffic is made.
 * Tests are grouped by concern: validation, mapping, business rules, and error handling.
 */
@ExtendWith(MockitoExtension.class)
class CountryServiceTest {

    @Mock
    private RestTemplate restTemplate;

    /** Direct construction so we can inject the mock RestTemplate and a test base URL. */
    private CountryService countryService;

    @BeforeEach
    void setUp() {
        countryService = new CountryService(restTemplate, "https://restcountries.com/v3.1");
    }

    // =========================================================================
    // Validation tests
    // =========================================================================

    @Nested
    @DisplayName("Input validation")
    class ValidationTests {

        @ParameterizedTest(name = "code=''{0}'' should be rejected")
        @ValueSource(strings = {"", "U", "USA", "1A", "A1", "  ", "u$"})
        @DisplayName("Invalid codes (wrong length or non-alpha) throw InvalidCountryCodeException")
        void invalidCodes_throwInvalidCountryCodeException(String code) {
            assertThatThrownBy(() -> countryService.validateCode(code))
                    .isInstanceOf(InvalidCountryCodeException.class)
                    .hasMessageContaining(code);
        }

        @Test
        @DisplayName("Null code throws InvalidCountryCodeException")
        void nullCode_throwsInvalidCountryCodeException() {
            assertThatThrownBy(() -> countryService.validateCode(null))
                    .isInstanceOf(InvalidCountryCodeException.class);
        }

        @ParameterizedTest(name = "code=''{0}'' should be accepted")
        @ValueSource(strings = {"US", "DE", "us", "de", "GB"})
        @DisplayName("Valid two-letter codes (any case) pass validation")
        void validCodes_doNotThrow(String code) {
            // Should not throw
            countryService.validateCode(code);
        }
    }

    // =========================================================================
    // Size-category business-rule tests
    // =========================================================================

    @Nested
    @DisplayName("Size category business rules")
    class SizeCategoryTests {

        @Test
        @DisplayName("Population 0 → SMALL")
        void population_zero_isSmall() {
            assertThat(countryService.determineSizeCategory(0)).isEqualTo("SMALL");
        }

        @Test
        @DisplayName("Population 999,999 → SMALL")
        void population_justBelowSmallThreshold_isSmall() {
            assertThat(countryService.determineSizeCategory(999_999)).isEqualTo("SMALL");
        }

        @Test
        @DisplayName("Population 1,000,000 → MEDIUM (boundary)")
        void population_atSmallThreshold_isMedium() {
            assertThat(countryService.determineSizeCategory(1_000_000)).isEqualTo("MEDIUM");
        }

        @Test
        @DisplayName("Population 5,000,000 → MEDIUM")
        void population_midMedium_isMedium() {
            assertThat(countryService.determineSizeCategory(5_000_000)).isEqualTo("MEDIUM");
        }

        @Test
        @DisplayName("Population 10,000,000 → MEDIUM (upper boundary)")
        void population_atMediumThreshold_isMedium() {
            assertThat(countryService.determineSizeCategory(10_000_000)).isEqualTo("MEDIUM");
        }

        @Test
        @DisplayName("Population 10,000,001 → LARGE")
        void population_justAboveMediumThreshold_isLarge() {
            assertThat(countryService.determineSizeCategory(10_000_001)).isEqualTo("LARGE");
        }

        @Test
        @DisplayName("Population 331,000,000 (US) → LARGE")
        void population_largeNation_isLarge() {
            assertThat(countryService.determineSizeCategory(331_000_000)).isEqualTo("LARGE");
        }
    }

    // =========================================================================
    // Mapping tests
    // =========================================================================

    @Nested
    @DisplayName("Mapping upstream response → CountryResponse")
    class MappingTests {

        @Test
        @DisplayName("Full upstream payload maps to all fields correctly")
        void fullPayload_mapsCorrectly() {
            RestCountryResponse src = buildFullRestCountryResponse();

            CountryResponse result = countryService.mapToResponse(src, "US");

            assertThat(result.getCountryCode()).isEqualTo("US");
            assertThat(result.getName()).isEqualTo("United States of America");
            assertThat(result.getCapital()).isEqualTo("Washington, D.C.");
            assertThat(result.getRegion()).isEqualTo("Americas");
            assertThat(result.getCurrencies()).containsExactly("USD");
            assertThat(result.getLanguages()).containsExactly("English");
            assertThat(result.getBorders()).containsExactlyInAnyOrder("CAN", "MEX");
            assertThat(result.getSizeCategory()).isEqualTo("LARGE");
        }

        @Test
        @DisplayName("Null borders (island nation) maps to empty list, not null")
        void nullBorders_mapToEmptyList() {
            RestCountryResponse src = buildFullRestCountryResponse();
            src.setBorders(null);

            CountryResponse result = countryService.mapToResponse(src, "AU");

            assertThat(result.getBorders()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Null currencies map to empty list, not null")
        void nullCurrencies_mapToEmptyList() {
            RestCountryResponse src = buildFullRestCountryResponse();
            src.setCurrencies(null);

            CountryResponse result = countryService.mapToResponse(src, "XX");

            assertThat(result.getCurrencies()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Null languages map to empty list, not null")
        void nullLanguages_mapToEmptyList() {
            RestCountryResponse src = buildFullRestCountryResponse();
            src.setLanguages(null);

            CountryResponse result = countryService.mapToResponse(src, "XX");

            assertThat(result.getLanguages()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Null name object does not cause NPE — name field left null")
        void nullNameObject_doesNotThrowNPE() {
            RestCountryResponse src = buildFullRestCountryResponse();
            src.setName(null);

            CountryResponse result = countryService.mapToResponse(src, "XX");

            assertThat(result.getName()).isNull();
        }

        @Test
        @DisplayName("When official name is null, common name is used as fallback")
        void nullOfficialName_fallsBackToCommonName() {
            RestCountryResponse src = buildFullRestCountryResponse();
            src.getName().setOfficial(null);
            src.getName().setCommon("United States");

            CountryResponse result = countryService.mapToResponse(src, "US");

            assertThat(result.getName()).isEqualTo("United States");
        }

        @Test
        @DisplayName("Empty capital list maps to null capital field")
        void emptyCapitalList_mapsToNullCapital() {
            RestCountryResponse src = buildFullRestCountryResponse();
            src.setCapital(Collections.emptyList());

            CountryResponse result = countryService.mapToResponse(src, "US");

            assertThat(result.getCapital()).isNull();
        }
    }

    // =========================================================================
    // Error-handling tests (upstream API failures)
    // =========================================================================

    @Nested
    @DisplayName("Upstream API error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Upstream 404 → CountryNotFoundException")
        void upstream404_throwsCountryNotFoundException() {
            when(restTemplate.getForEntity(anyString(), eq(RestCountryResponse[].class)))
                    .thenThrow(HttpClientErrorException.create(
                            HttpStatus.NOT_FOUND, "Not Found", null, null, null));

            assertThatThrownBy(() -> countryService.getCountryByCode("XX"))
                    .isInstanceOf(CountryNotFoundException.class);
        }

        @Test
        @DisplayName("Upstream 500 → UpstreamApiException")
        void upstream500_throwsUpstreamApiException() {
            when(restTemplate.getForEntity(anyString(), eq(RestCountryResponse[].class)))
                    .thenThrow(HttpClientErrorException.create(
                            HttpStatus.INTERNAL_SERVER_ERROR, "Server Error", null, null, null));

            assertThatThrownBy(() -> countryService.getCountryByCode("US"))
                    .isInstanceOf(UpstreamApiException.class);
        }

        @Test
        @DisplayName("Network timeout → UpstreamApiException")
        void networkTimeout_throwsUpstreamApiException() {
            when(restTemplate.getForEntity(anyString(), eq(RestCountryResponse[].class)))
                    .thenThrow(new ResourceAccessException("Connection timed out"));

            assertThatThrownBy(() -> countryService.getCountryByCode("DE"))
                    .isInstanceOf(UpstreamApiException.class)
                    .hasMessageContaining("Unable to reach");
        }

        @Test
        @DisplayName("Empty array response → CountryNotFoundException")
        void emptyArrayResponse_throwsCountryNotFoundException() {
            when(restTemplate.getForEntity(anyString(), eq(RestCountryResponse[].class)))
                    .thenReturn(ResponseEntity.ok(new RestCountryResponse[0]));

            assertThatThrownBy(() -> countryService.getCountryByCode("ZZ"))
                    .isInstanceOf(CountryNotFoundException.class);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Builds a fully-populated RestCountryResponse representing the USA. */
    private RestCountryResponse buildFullRestCountryResponse() {
        RestCountryResponse src = new RestCountryResponse();

        RestCountryResponse.NameObject nameObj = new RestCountryResponse.NameObject();
        nameObj.setOfficial("United States of America");
        nameObj.setCommon("United States");
        src.setName(nameObj);

        src.setCca2("US");
        src.setCapital(List.of("Washington, D.C."));
        src.setRegion("Americas");
        src.setPopulation(331_000_000L);
        src.setCurrencies(Map.of("USD", Map.of("name", "United States dollar", "symbol", "$")));
        src.setLanguages(Map.of("eng", "English"));
        src.setBorders(List.of("CAN", "MEX"));

        return src;
    }
}
