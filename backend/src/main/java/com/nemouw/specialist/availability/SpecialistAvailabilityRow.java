package com.nemouw.specialist.availability;

import java.time.LocalTime;
import java.util.UUID;

record SpecialistAvailabilityRow(UUID id, UUID locationId, String dayOfWeek, LocalTime startTime, LocalTime endTime) {
}