
package com.example.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CountryResponse {
    private String countryCode;
    private String name;
    private String capital;
    private String region;
    private List<String> currencies;
    private List<String> languages;
    private List<String> borders;
    private String sizeCategory;
}
