package com.nemouw.specialty;

import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.Function;

/**
 * Public lookup for specialty reference data.
 */
@Service
public class SpecialtyLookup {
    private final SpecialtyRepository specialties;

    SpecialtyLookup(SpecialtyRepository specialties) {
        this.specialties = specialties;
    }

    public boolean existsById(UUID id) {
        return specialties.existsById(id);
    }

    public interface SpecialtyExistsProvider extends Function<UUID, Boolean> {
    }
}