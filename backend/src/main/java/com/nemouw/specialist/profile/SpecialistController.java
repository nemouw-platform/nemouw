package com.nemouw.specialist.profile;

import com.nemouw.specialist.profile.SpecialistDtos.SpecialistOnboardingRequest;
import com.nemouw.specialist.profile.SpecialistDtos.SpecialistResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/specialists/me")
public class SpecialistController {
    private final SpecialistService service;

    SpecialistController(SpecialistService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<SpecialistResponse> onboard(@RequestBody SpecialistOnboardingRequest req,
                                               @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.onboard(userId(jwt), req));
    }

    @GetMapping
    SpecialistResponse get(@AuthenticationPrincipal Jwt jwt) {
        return service.getMine(userId(jwt));
    }

    @PatchMapping
    SpecialistResponse update(@RequestBody Map<String, Object> body, @AuthenticationPrincipal Jwt jwt) {
        return service.update(userId(jwt), body);
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
    }
}