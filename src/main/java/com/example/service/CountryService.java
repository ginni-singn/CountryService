
package com.example.service;

import com.example.exception.*;
import com.example.model.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class CountryService {

    private final RestTemplate restTemplate = new RestTemplate();

    public CountryResponse getCountry(String code) {

        if (code == null || !code.matches("^[A-Za-z]{2}$")) {
            throw new BadRequestException("Invalid country code");
        }

        String url = "https://restcountries.com/v3.1/alpha/" + code;

        ExternalCountryResponse[] response;
        try {
            response = restTemplate.getForObject(url, ExternalCountryResponse[].class);
        } catch (Exception e) {
            throw new ExternalServiceException("External API failure");
        }

        if (response == null || response.length == 0) {
            throw new NotFoundException("Country not found");
        }

        ExternalCountryResponse c = response[0];

        return CountryResponse.builder()
                .countryCode(code.toUpperCase())
                .name(c.getName().getCommon())
                .capital(c.getCapital() != null ? c.getCapital().get(0) : null)
                .region(c.getRegion())
                .currencies(new ArrayList<>(c.getCurrencies().keySet()))
                .languages(new ArrayList<>(c.getLanguages().values()))
                .borders(c.getBorders())
                .sizeCategory(size(c.getPopulation()))
                .build();
    }

    private String size(long p) {
        if (p < 1000000) return "SMALL";
        if (p <= 10000000) return "MEDIUM";
        return "LARGE";
    }
}
