package com.countryservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the upstream REST Countries API is unreachable or returns an
 * unexpected error (anything other than 200 or 404).
 *
 * Wrapping upstream failures in a dedicated exception lets us return a clear
 * 502 Bad Gateway to callers rather than leaking internal stack traces or
 * framework error messages.
 */
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class UpstreamApiException extends RuntimeException {

    public UpstreamApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public UpstreamApiException(String message) {
        super(message);
    }
}
