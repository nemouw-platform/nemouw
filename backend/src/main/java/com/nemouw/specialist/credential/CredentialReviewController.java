package com.nemouw.specialist.credential;

import com.nemouw.specialist.credential.CredentialDtos.CredentialResponse;
import com.nemouw.specialist.credential.CredentialDtos.DocumentLinkResponse;
import com.nemouw.specialist.credential.CredentialDtos.RejectionRequest;
import com.nemouw.syndicate.access.AdminOrSyndicateStaff;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
public class CredentialReviewController {
    private final CredentialReviewService service;

    CredentialReviewController(CredentialReviewService service) {
        this.service = service;
    }

    @GetMapping("/syndicates/{syndicateId}/credentials")
    @AdminOrSyndicateStaff
    public List<CredentialResponse> queue(@PathVariable UUID syndicateId,
                                          @RequestParam(name = "verification_status", required = false) String status) {
        return service.queue(syndicateId, status);
    }

    @PostMapping("/credentials/{credentialId}/verify")
    @AdminOrCredentialReviewer
    public CredentialResponse verify(@PathVariable UUID credentialId, @AuthenticationPrincipal Jwt jwt) {
        return service.verify(credentialId, userId(jwt));
    }

    @PostMapping("/credentials/{credentialId}/reject")
    @AdminOrCredentialReviewer
    public CredentialResponse reject(@PathVariable UUID credentialId, @RequestBody RejectionRequest req,
                                     @AuthenticationPrincipal Jwt jwt) {
        return service.reject(credentialId, userId(jwt), req);
    }

    @GetMapping("/credentials/{credentialId}/document")
    @AdminOrCredentialReviewer
    public DocumentLinkResponse documentLink(@PathVariable UUID credentialId) {
        return service.documentLink(credentialId);
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
    }
}