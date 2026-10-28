package com.nemouw.specialty;

import com.nemouw.specialty.SpecialtyDtos.SpecialtyResponse;
import com.nemouw.specialty.SpecialtyDtos.SpecialtyWriteRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/specialties")
public class SpecialtyController {
    private final SpecialtyService service;

    SpecialtyController(SpecialtyService service) {
        this.service = service;
    }

    @GetMapping
    List<SpecialtyResponse> list(@RequestParam(name = "is_active", required = false) Boolean isActive) {
        return service.list(isActive);
    }

    @PostMapping
    ResponseEntity<SpecialtyResponse> create(@RequestBody SpecialtyWriteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PatchMapping("/{specialtyId}")
    SpecialtyResponse update(@PathVariable UUID specialtyId, @RequestBody SpecialtyWriteRequest req) {
        return service.update(specialtyId, req);
    }
}