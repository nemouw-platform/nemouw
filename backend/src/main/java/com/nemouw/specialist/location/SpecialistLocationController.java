package com.nemouw.specialist.location;

import com.nemouw.specialist.location.SpecialistLocationDtos.SpecialistLocationResponse;
import com.nemouw.specialist.location.SpecialistLocationDtos.SpecialistLocationWriteRequest;
import com.nemouw.specialist.profile.CurrentSpecialistId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/specialists/me/locations")
public class SpecialistLocationController {
    private final SpecialistLocationService service;

    SpecialistLocationController(SpecialistLocationService service) {
        this.service = service;
    }

    @GetMapping
    List<SpecialistLocationResponse> list(@CurrentSpecialistId UUID specialistId) {
        return service.list(specialistId);
    }

    @PostMapping
    ResponseEntity<SpecialistLocationResponse> add(@CurrentSpecialistId UUID specialistId,
                                                   @RequestBody SpecialistLocationWriteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(specialistId, req));
    }

    @PutMapping("/{locationId}")
    SpecialistLocationResponse update(@CurrentSpecialistId UUID specialistId, @PathVariable UUID locationId,
                                      @RequestBody SpecialistLocationWriteRequest req) {
        return service.update(specialistId, locationId, req);
    }

    @DeleteMapping("/{locationId}")
    ResponseEntity<Void> delete(@CurrentSpecialistId UUID specialistId, @PathVariable UUID locationId) {
        service.delete(specialistId, locationId);
        return ResponseEntity.noContent().build();
    }
}