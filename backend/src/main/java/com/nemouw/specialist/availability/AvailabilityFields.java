package com.nemouw.specialist.availability;

import java.time.LocalTime;
import java.util.UUID;

/** A validated availability write. */
record AvailabilityFields(UUID locationId, int dayOfWeek, LocalTime startTime, LocalTime endTime) {
}