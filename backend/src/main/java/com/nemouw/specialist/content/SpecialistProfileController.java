package com.nemouw.specialist.content;

import com.nemouw.specialist.content.SpecialistProfileDtos.SpecialistProfileResponse;
import com.nemouw.specialist.content.SpecialistProfileDtos.SpecialistProfileWriteRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/specialists/me/profiles")
public class SpecialistProfileController {
    private final SpecialistProfileService service;

    SpecialistProfileController(SpecialistProfileService service) {
        this.service = service;
    }

    @GetMapping
    List<SpecialistProfileResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(userId(jwt));
    }

    @GetMapping("/{locale}")
    SpecialistProfileResponse get(@PathVariable String locale, @AuthenticationPrincipal Jwt jwt) {
        return service.get(userId(jwt), locale);
    }

    @PutMapping("/{locale}")
    SpecialistProfileResponse put(@PathVariable String locale, @RequestBody SpecialistProfileWriteRequest req,
                                  @AuthenticationPrincipal Jwt jwt) {
        return service.put(userId(jwt), locale, req);
    }

    @DeleteMapping("/{locale}")
    ResponseEntity<Void> delete(@PathVariable String locale, @AuthenticationPrincipal Jwt jwt) {
        service.delete(userId(jwt), locale);
        return ResponseEntity.noContent().build();
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
    }
}