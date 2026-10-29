package com.nemouw.specialist.availability;

import java.time.LocalTime;
import java.util.UUID;

record SpecialistAvailabilityRow(UUID id, UUID locationId, int dayOfWeek, LocalTime startTime, LocalTime endTime) {
}