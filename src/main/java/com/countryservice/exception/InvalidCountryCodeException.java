package com.countryservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the caller supplies a country code that does not match the
 * two-letter ISO 3166-1 alpha-2 format (e.g. "USA", "1A", "").
 *
 * Maps to HTTP 400 Bad Request.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidCountryCodeException extends RuntimeException {

    public InvalidCountryCodeException(String code) {
        super("Invalid country code format: '" + code +
              "'. Expected a two-letter ISO 3166-1 alpha-2 code (e.g. US, DE).");
    }
}
