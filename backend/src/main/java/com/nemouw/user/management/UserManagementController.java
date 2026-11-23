package com.nemouw.user.management;

import com.nemouw.user.management.ManagementDtos.UserDetail;
import com.nemouw.user.management.ManagementDtos.UserPage;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
public class UserManagementController {
    private final UserManagementService service;

    UserManagementController(UserManagementService service) {
        this.service = service;
    }

    @GetMapping
    UserPage list(@RequestParam MultiValueMap<String, String> params) {
        return service.list(params);
    }

    @GetMapping("/{userId}")
    UserDetail get(@PathVariable UUID userId) {
        return service.get(userId);
    }

    @PatchMapping("/{userId}")
    UserDetail update(@PathVariable UUID userId, @RequestBody Map<String, Object> body) {
        return service.update(userId, body);
    }

    @PostMapping("/{userId}/suspend")
    UserDetail suspend(@PathVariable UUID userId) {
        return service.suspend(userId);
    }

    @PostMapping("/{userId}/unsuspend")
    UserDetail unsuspend(@PathVariable UUID userId) {
        return service.unsuspend(userId);
    }
}