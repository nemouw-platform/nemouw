package com.nemouw.specialist.availability;

import com.nemouw.specialist.availability.SpecialistAvailabilityDtos.SpecialistAvailabilityResponse;
import com.nemouw.specialist.availability.SpecialistAvailabilityDtos.SpecialistAvailabilityWriteRequest;
import com.nemouw.specialist.profile.CurrentSpecialistId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/specialists/me/availabilities")
public class SpecialistAvailabilityController {
    private final SpecialistAvailabilityService service;

    SpecialistAvailabilityController(SpecialistAvailabilityService service) {
        this.service = service;
    }

    @GetMapping
    List<SpecialistAvailabilityResponse> list(@CurrentSpecialistId UUID specialistId) {
        return service.list(specialistId);
    }

    @PostMapping
    ResponseEntity<SpecialistAvailabilityResponse> add(@CurrentSpecialistId UUID specialistId,
                                                       @RequestBody SpecialistAvailabilityWriteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(specialistId, req));
    }

    @PutMapping("/{availabilityId}")
    SpecialistAvailabilityResponse update(@CurrentSpecialistId UUID specialistId,
                                          @PathVariable UUID availabilityId,
                                          @RequestBody SpecialistAvailabilityWriteRequest req) {
        return service.update(specialistId, availabilityId, req);
    }

    @DeleteMapping("/{availabilityId}")
    ResponseEntity<Void> delete(@CurrentSpecialistId UUID specialistId, @PathVariable UUID availabilityId) {
        service.delete(specialistId, availabilityId);
        return ResponseEntity.noContent().build();
    }
}