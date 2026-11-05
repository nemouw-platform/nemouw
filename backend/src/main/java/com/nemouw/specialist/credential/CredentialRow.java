package com.nemouw.specialist.credential;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

record CredentialRow(UUID id, UUID specialistId, String credentialTypeCode, String title,
                     String issuingOrganization, String credentialNumber, LocalDate issueDate,
                     LocalDate expiryDate, UUID documentId, String documentMimeType,
                     OffsetDateTime documentUploadedAt, String verificationStatusCode, UUID reviewedBy,
                     OffsetDateTime reviewedAt, String rejectionReason, OffsetDateTime createdAt,
                     OffsetDateTime updatedAt) {
    CredentialType credentialType() {
        return CredentialType.fromCode(credentialTypeCode);
    }

    CredentialVerificationStatus verificationStatus() {
        return CredentialVerificationStatus.fromCode(verificationStatusCode);
    }
}