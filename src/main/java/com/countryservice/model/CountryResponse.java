package com.countryservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Outbound response model returned by GET /countries/{code}.
 *
 * Only the fields explicitly required by the specification are included here.
 * Keeping the response model narrow (rather than returning the raw upstream payload)
 * means callers are insulated from upstream API changes and we expose only what we own.
 */
public class CountryResponse {

    /** Two-letter ISO 3166-1 alpha-2 code, upper-cased (e.g. "US"). */
    @JsonProperty("countryCode")
    private String countryCode;

    /** Official/common country name. */
    @JsonProperty("name")
    private String name;

    /** Primary capital city. Null-safe: returned as null when the API omits this field. */
    @JsonProperty("capital")
    private String capital;

    /** World region (e.g. "Americas", "Europe"). */
    @JsonProperty("region")
    private String region;

    /** ISO 4217 currency codes used in the country (e.g. ["USD"]). */
    @JsonProperty("currencies")
    private List<String> currencies;

    /** Languages spoken in the country (e.g. ["English"]). */
    @JsonProperty("languages")
    private List<String> languages;

    /** ISO 3166-1 alpha-3 codes of bordering countries (e.g. ["CAN", "MEX"]). */
    @JsonProperty("borders")
    private List<String> borders;

    /**
     * Population-based size category computed by the service layer:
     * SMALL  < 1 000 000
     * MEDIUM 1 000 000 – 10 000 000
     * LARGE  > 10 000 000
     */
    @JsonProperty("sizeCategory")
    private String sizeCategory;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public CountryResponse() { }

    public CountryResponse(String countryCode, String name, String capital,
                           String region, List<String> currencies,
                           List<String> languages, List<String> borders,
                           String sizeCategory) {
        this.countryCode  = countryCode;
        this.name         = name;
        this.capital      = capital;
        this.region       = region;
        this.currencies   = currencies;
        this.languages    = languages;
        this.borders      = borders;
        this.sizeCategory = sizeCategory;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getCountryCode()        { return countryCode; }
    public void   setCountryCode(String c){ this.countryCode = c; }

    public String getName()               { return name; }
    public void   setName(String n)       { this.name = n; }

    public String getCapital()            { return capital; }
    public void   setCapital(String c)    { this.capital = c; }

    public String getRegion()             { return region; }
    public void   setRegion(String r)     { this.region = r; }

    public List<String> getCurrencies()             { return currencies; }
    public void         setCurrencies(List<String> c){ this.currencies = c; }

    public List<String> getLanguages()              { return languages; }
    public void         setLanguages(List<String> l) { this.languages = l; }

    public List<String> getBorders()                { return borders; }
    public void         setBorders(List<String> b)  { this.borders = b; }

    public String getSizeCategory()           { return sizeCategory; }
    public void   setSizeCategory(String s)   { this.sizeCategory = s; }
}
