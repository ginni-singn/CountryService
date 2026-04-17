package com.countryservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the upstream REST Countries API returns a 404 for the requested
 * country code, meaning the code is well-formed but does not correspond to a
 * recognised country.
 *
 * Maps to HTTP 404 Not Found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class CountryNotFoundException extends RuntimeException {

    public CountryNotFoundException(String code) {
        super("No country found for code: '" + code + "'.");
    }
}
