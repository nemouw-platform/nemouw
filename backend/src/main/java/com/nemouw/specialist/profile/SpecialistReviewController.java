package com.nemouw.specialist.profile;

import com.nemouw.specialist.profile.SpecialistDtos.SpecialistRejectionRequest;
import com.nemouw.specialist.profile.SpecialistDtos.SpecialistResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/specialists/by-id/{specialistId}")
@AdminOrSpecialistReviewer
public class SpecialistReviewController {
    private final SpecialistReviewService service;

    SpecialistReviewController(SpecialistReviewService service) {
        this.service = service;
    }

    @GetMapping
    public SpecialistResponse get(@PathVariable UUID specialistId) {
        return service.get(specialistId);
    }

    @PostMapping("/verify")
    public SpecialistResponse verify(@PathVariable UUID specialistId, @AuthenticationPrincipal Jwt jwt) {
        return service.verify(specialistId, userId(jwt));
    }

    @PostMapping("/reject")
    public SpecialistResponse reject(@PathVariable UUID specialistId, @RequestBody SpecialistRejectionRequest req,
                                     @AuthenticationPrincipal Jwt jwt) {
        return service.reject(specialistId, userId(jwt), req);
    }

    @PostMapping("/suspend")
    public SpecialistResponse suspend(@PathVariable UUID specialistId, @AuthenticationPrincipal Jwt jwt) {
        return service.suspend(specialistId, userId(jwt));
    }

    @PostMapping("/unsuspend")
    public SpecialistResponse unsuspend(@PathVariable UUID specialistId, @AuthenticationPrincipal Jwt jwt) {
        return service.unsuspend(specialistId, userId(jwt));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
    }
}