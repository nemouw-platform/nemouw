package com.nemouw.condition;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public final class ConditionDtos {
    private ConditionDtos() {}

    /** Used for create and update. On update, null means "leave unchanged". */
    public record ConditionWriteRequest(String code, String name, String label, String labelFr, String labelAr,
                                        String description, @JsonProperty("is_active") Boolean active) {}

    public record ConditionResponse(UUID id, String code, String name, String label, String labelFr,
                                    String labelAr, String description,
                                    @JsonProperty("is_active") boolean active) {
        static ConditionResponse from(Condition c) {
            return new ConditionResponse(c.getId(), c.getCode(), c.getName(), c.getLabel(), c.getLabelFr(),
                    c.getLabelAr(), c.getDescription(), c.isActive());
        }
    }
}