package com.countryservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Internal model that mirrors the shape of a single country object returned by
 * https://restcountries.com/v3.1/alpha/{code}.
 *
 * Fields that are not needed by our response are intentionally omitted;
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)} prevents Jackson from
 * throwing when it encounters extra fields — important because the upstream API
 * may add fields at any time.
 *
 * This class is intentionally package-private to the model layer; callers should
 * work with {@link CountryResponse} only.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestCountryResponse {

    /**
     * The upstream API nests official/common names inside a "name" object:
     * { "name": { "common": "...", "official": "..." } }
     */
    @JsonProperty("name")
    private NameObject name;

    /** Two-letter ISO code provided by the upstream API. */
    @JsonProperty("cca2")
    private String cca2;

    /**
     * Capital is an array in v3.1 (a country can have multiple capitals).
     * We take the first element; null-safe.
     */
    @JsonProperty("capital")
    private List<String> capital;

    @JsonProperty("region")
    private String region;

    /** Population as a long to safely handle large nations. */
    @JsonProperty("population")
    private long population;

    /**
     * Currencies is a map keyed by ISO 4217 code:
     * { "USD": { "name": "United States dollar", "symbol": "$" } }
     * We only need the keys.
     */
    @JsonProperty("currencies")
    private Map<String, Object> currencies;

    /**
     * Languages is a map keyed by BCP 47 code:
     * { "eng": "English" }
     * We only need the values (human-readable names).
     */
    @JsonProperty("languages")
    private Map<String, String> languages;

    /** Three-letter ISO codes of bordering countries. May be null for island nations. */
    @JsonProperty("borders")
    private List<String> borders;

    // -------------------------------------------------------------------------
    // Inner class — mirrors the nested "name" structure in the upstream payload
    // -------------------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NameObject {

        @JsonProperty("common")
        private String common;

        @JsonProperty("official")
        private String official;

        public String getCommon()           { return common; }
        public void   setCommon(String c)   { this.common = c; }

        public String getOfficial()         { return official; }
        public void   setOfficial(String o) { this.official = o; }
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public NameObject          getName()                      { return name; }
    public void                setName(NameObject n)          { this.name = n; }

    public String              getCca2()                      { return cca2; }
    public void                setCca2(String c)              { this.cca2 = c; }

    public List<String>        getCapital()                   { return capital; }
    public void                setCapital(List<String> c)     { this.capital = c; }

    public String              getRegion()                    { return region; }
    public void                setRegion(String r)            { this.region = r; }

    public long                getPopulation()                { return population; }
    public void                setPopulation(long p)          { this.population = p; }

    public Map<String, Object> getCurrencies()                { return currencies; }
    public void                setCurrencies(Map<String, Object> c) { this.currencies = c; }

    public Map<String, String> getLanguages()                 { return languages; }
    public void                setLanguages(Map<String, String> l)  { this.languages = l; }

    public List<String>        getBorders()                   { return borders; }
    public void                setBorders(List<String> b)     { this.borders = b; }
}
