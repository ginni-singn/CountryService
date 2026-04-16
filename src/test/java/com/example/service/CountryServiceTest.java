package com.example.service;
import com.example.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class CountryServiceTest {
        @Test
        void testSizeCategoryLarge() {

            RestTemplate rt = mock(RestTemplate.class);
            CountryService service = new CountryService(rt);//rt as the argument for default constructor

            ExternalCountryResponse res = new ExternalCountryResponse();
            ExternalCountryResponse.Name name = new ExternalCountryResponse.Name();
            name.setCommon("USA");

            res.setName(name);
            res.setCapital(List.of("Washington"));
            res.setRegion("Americas");
            res.setPopulation(50000000);
            res.setCurrencies(Map.of("USD", new Object()));
            res.setLanguages(Map.of("en","English"));

            when(rt.getForObject(anyString(), eq(ExternalCountryResponse[].class)))
                    .thenReturn(new ExternalCountryResponse[]{res});

            CountryResponse response = service.getCountry("US");

            assertEquals("LARGE", response.getSizeCategory());
        }
    }

