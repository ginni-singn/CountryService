
package com.example.controller;

import com.example.model.CountryResponse;
import com.example.service.CountryService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/countries")
public class CountryController {

    private final CountryService service;

    public CountryController(CountryService service) {
        this.service = service;
    }

    @GetMapping("/{code}")
    public CountryResponse getCountry(@PathVariable String code) {
        return service.getCountry(code);
    }
}
