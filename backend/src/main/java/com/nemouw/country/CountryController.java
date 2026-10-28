package com.nemouw.country;

import com.nemouw.country.CountryDtos.CountryResponse;
import com.nemouw.country.CountryDtos.CountryWriteRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/countries")
public class CountryController {
    private final CountryService service;

    CountryController(CountryService service) {
        this.service = service;
    }

    @GetMapping
    List<CountryResponse> list(@RequestParam(name = "is_active", required = false) Boolean isActive) {
        return service.list(isActive);
    }

    @GetMapping("/{countryId}")
    CountryResponse get(@PathVariable UUID countryId) {
        return service.get(countryId);
    }

    @PostMapping
    ResponseEntity<CountryResponse> create(@RequestBody CountryWriteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PatchMapping("/{countryId}")
    CountryResponse update(@PathVariable UUID countryId, @RequestBody CountryWriteRequest req) {
        return service.update(countryId, req);
    }

    @DeleteMapping("/{countryId}")
    ResponseEntity<Void> delete(@PathVariable UUID countryId) {
        service.delete(countryId);
        return ResponseEntity.noContent().build();
    }
}