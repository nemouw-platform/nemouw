package com.nemouw.specialist.credential;

import com.nemouw.specialist.credential.CredentialDtos.CredentialResponse;
import com.nemouw.specialist.credential.CredentialDtos.CredentialWriteRequest;
import com.nemouw.specialist.credential.CredentialDtos.DocumentLinkResponse;
import com.nemouw.specialist.profile.CurrentSpecialistId;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/specialists/me/credentials")
public class CredentialController {
    private final CredentialService service;

    CredentialController(CredentialService service) {
        this.service = service;
    }

    @GetMapping
    List<CredentialResponse> list(@CurrentSpecialistId UUID specialistId) {
        return service.list(specialistId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<CredentialResponse> submit(
            @CurrentSpecialistId UUID specialistId,
            @RequestParam(name = "credential_type", required = false) String credentialType,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "issuing_organization", required = false) String issuingOrganization,
            @RequestParam(name = "credential_number", required = false) String credentialNumber,
            @RequestParam(name = "issue_date", required = false) String issueDate,
            @RequestParam(name = "expiry_date", required = false) String expiryDate,
            @RequestPart(name = "document", required = false) MultipartFile document) {
        var req = new CredentialWriteRequest(credentialType, title, issuingOrganization, credentialNumber,
                issueDate, expiryDate);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.submit(specialistId, req, document));
    }

    @GetMapping("/{credentialId}")
    CredentialResponse get(@CurrentSpecialistId UUID specialistId, @PathVariable UUID credentialId) {
        return service.get(specialistId, credentialId);
    }

    @PatchMapping("/{credentialId}")
    CredentialResponse update(@CurrentSpecialistId UUID specialistId, @PathVariable UUID credentialId,
                              @RequestBody CredentialWriteRequest req) {
        return service.update(specialistId, credentialId, req);
    }

    @DeleteMapping("/{credentialId}")
    ResponseEntity<Void> withdraw(@CurrentSpecialistId UUID specialistId, @PathVariable UUID credentialId) {
        service.withdraw(specialistId, credentialId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{credentialId}/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    CredentialResponse replaceDocument(@CurrentSpecialistId UUID specialistId, @PathVariable UUID credentialId,
                                       @RequestPart(name = "document", required = false) MultipartFile document) {
        return service.replaceDocument(specialistId, credentialId, document);
    }

    @GetMapping("/{credentialId}/document")
    DocumentLinkResponse documentLink(@CurrentSpecialistId UUID specialistId, @PathVariable UUID credentialId) {
        return service.documentLink(specialistId, credentialId);
    }
}