
package com.example.model;

import lombok.Data;
import java.util.*;

@Data
public class ExternalCountryResponse {
    private Name name;
    private List<String> capital;
    private String region;
    private Map<String, Object> currencies;
    private Map<String, String> languages;
    private List<String> borders;
    private long population;

    @Data
    public static class Name {
        private String common;
    }
}
