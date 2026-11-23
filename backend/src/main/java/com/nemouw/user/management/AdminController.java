package com.nemouw.user.management;

import com.nemouw.user.api.UserSummary;
import com.nemouw.user.management.ManagementDtos.AdminInviteRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/admins")
public class AdminController {
    private final AdminService service;

    AdminController(AdminService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<UserSummary> invite(@RequestBody AdminInviteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.invite(req));
    }

    @GetMapping
    List<UserSummary> list() {
        return service.list();
    }

    @GetMapping("/{userId}")
    UserSummary get(@PathVariable UUID userId) {
        return service.get(userId);
    }

    @PostMapping("/{userId}/suspend")
    UserSummary suspend(@PathVariable UUID userId, @AuthenticationPrincipal Jwt jwt) {
        return service.suspend(userId, UUID.fromString(Objects.requireNonNull(jwt.getSubject())));
    }

    @PostMapping("/{userId}/unsuspend")
    UserSummary unsuspend(@PathVariable UUID userId) {
        return service.unsuspend(userId);
    }
}