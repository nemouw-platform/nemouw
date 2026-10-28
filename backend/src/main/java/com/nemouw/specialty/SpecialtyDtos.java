package com.nemouw.specialty;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class SpecialtyDtos {
    private SpecialtyDtos() {
    }

    /**
     * Used for create and update. On update, null means "leave unchanged".
     */
    public record SpecialtyWriteRequest(String code, String name, String label, String labelFr, String labelAr,
                                        String description, @JsonProperty("is_active") Boolean active) {
    }

    public record SpecialtyResponse(UUID id, String code, String name, String label, String labelFr,
                                    String labelAr, String description,
                                    @JsonProperty("is_active") boolean active) {
        static SpecialtyResponse from(Specialty s) {
            return new SpecialtyResponse(s.getId(), s.getCode(), s.getName(), s.getLabel(), s.getLabelFr(),
                    s.getLabelAr(), s.getDescription(), s.isActive());
        }
    }
}