package com.nemouw.specialist.availability;

import java.util.UUID;

public final class SpecialistAvailabilityDtos {
    private SpecialistAvailabilityDtos() {
    }

    public record SpecialistAvailabilityWriteRequest(UUID locationId, Integer dayOfWeek, String startTime,
                                                     String endTime) {
    }

    public record SpecialistAvailabilityResponse(UUID id, UUID locationId, int dayOfWeek, String startTime,
                                                 String endTime) {
        static SpecialistAvailabilityResponse from(SpecialistAvailabilityRow r) {
            return new SpecialistAvailabilityResponse(r.id(), r.locationId(), r.dayOfWeek(),
                    ClockTime.format(r.startTime()), ClockTime.format(r.endTime()));
        }
    }
}