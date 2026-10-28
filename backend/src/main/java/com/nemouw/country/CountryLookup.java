package com.nemouw.country;

import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.Function;

/**
 * Public lookup for country reference data.
 */
@Service
public class CountryLookup {
    private final CountryRepository countries;

    CountryLookup(CountryRepository countries) {
        this.countries = countries;
    }

    public boolean existsById(UUID id) {
        return countries.existsById(id);
    }

    public interface CountryExistsProvider extends Function<UUID, Boolean> {
    }
}