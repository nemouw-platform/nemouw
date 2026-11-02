package com.nemouw.specialist.availability;

import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.exception.Errors;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.util.Validator;
import com.nemouw.specialist.availability.SpecialistAvailabilityDtos.SpecialistAvailabilityResponse;
import com.nemouw.specialist.availability.SpecialistAvailabilityDtos.SpecialistAvailabilityWriteRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Optional;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
class SpecialistAvailabilityService {
    private final SpecialistAvailabilityRepository availabilities;

    SpecialistAvailabilityService(SpecialistAvailabilityRepository availabilities) {
        this.availabilities = availabilities;
    }

    @Transactional(readOnly = true)
    List<SpecialistAvailabilityResponse> list(UUID specialistId) {
        return availabilities.findAll(specialistId).stream()
                .map(SpecialistAvailabilityResponse::from)
                .sorted(Comparator.comparing(SpecialistAvailabilityResponse::dayOfWeek))
                .toList();
    }

    @Transactional
    SpecialistAvailabilityResponse add(UUID specialistId, SpecialistAvailabilityWriteRequest req) {
        AvailabilityFields fields = validated(specialistId, req);
        return SpecialistAvailabilityResponse.from(availabilities.insert(specialistId, fields));
    }

    @Transactional
    SpecialistAvailabilityResponse update(UUID specialistId, UUID availabilityId,
                                          SpecialistAvailabilityWriteRequest req) {
        AvailabilityFields fields = validated(specialistId, req);
        return availabilities.update(specialistId, availabilityId, fields)
                .map(SpecialistAvailabilityResponse::from)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    @Transactional
    void delete(UUID specialistId, UUID availabilityId) {
        if (!availabilities.delete(specialistId, availabilityId)) throw new ApiException(ErrorCode.NOT_FOUND);
    }

    private AvailabilityFields validated(UUID specialistId, SpecialistAvailabilityWriteRequest req) {
        Validator v = new Validator();
        WeekDay day = parseDay(v, req.dayOfWeek());
        LocalTime start = parseTime(v, "start_time", req.startTime());
        LocalTime end = parseTime(v, "end_time", req.endTime());
        if (start != null && end != null) {
            v.check(end.isAfter(start), FieldErrors.invalidFormat("end_time", "must be after start_time"));
        }
        v.throwIfInvalid();

        if (req.locationId() != null && !availabilities.locationBelongsTo(specialistId, req.locationId())) {
            throw Errors.invalidField("location_id", "unknown location");
        }
        return new AvailabilityFields(req.locationId(), day, start, end);
    }

    private static LocalTime parseTime(Validator v, String field, String text) {
        if (text == null || text.isBlank()) {
            v.required(field, text);
            return null;
        }
        LocalTime time = ClockTime.parse(text);
        v.check(time != null, FieldErrors.invalidFormat(field, "must be a time as HH:mm"));
        return time;
    }

    private static WeekDay parseDay(Validator v, String text) {
        if (text == null || text.isBlank()) {
            v.required("day_of_week", text);
            return null;
        }
        Optional<WeekDay> day = WeekDay.tryFrom(text);
        v.check(day.isPresent(), FieldErrors.invalidFormat("day_of_week", "must be one of " + WeekDay.allCodes()));
        return day.orElse(null);
    }
}