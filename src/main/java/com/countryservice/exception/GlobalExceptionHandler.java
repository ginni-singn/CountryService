package com.countryservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralised exception handler for the CountryService API.
 *
 * Returning a structured JSON body (status, error, message, timestamp) rather
 * than Spring's default whitespace HTML error page makes the API easier to
 * consume programmatically and gives callers actionable information without
 * leaking internal implementation details.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------------------------------------------------------------
    // 400 — caller sent a malformed country code
    // -------------------------------------------------------------------------

    @ExceptionHandler(InvalidCountryCodeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCode(InvalidCountryCodeException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // 404 — valid code format but country not found upstream
    // -------------------------------------------------------------------------

    @ExceptionHandler(CountryNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(CountryNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // 502 — upstream REST Countries API is unreachable or returned an error
    // -------------------------------------------------------------------------

    @ExceptionHandler(UpstreamApiException.class)
    public ResponseEntity<Map<String, Object>> handleUpstreamError(UpstreamApiException ex) {
        return buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // 500 — catch-all for unexpected runtime failures
    // -------------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        // Log the full exception server-side but return only a generic message to callers.
        // (In a production system, wire in SLF4J/Logback here.)
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status",    status.value());
        body.put("error",     status.getReasonPhrase());
        body.put("message",   message);

        return ResponseEntity.status(status).body(body);
    }
}
