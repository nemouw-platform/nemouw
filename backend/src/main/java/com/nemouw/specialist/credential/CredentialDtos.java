package com.nemouw.specialist.credential;

import com.nemouw.media.api.PresignedLink;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class CredentialDtos {
    private CredentialDtos() {
    }

    /** Dates stay text so a bad value is a 422 on its field, not a generic 400. */
    public record CredentialWriteRequest(String credentialType, String title, String issuingOrganization,
                                         String credentialNumber, String issueDate, String expiryDate) {
    }

    public record RejectionRequest(String rejectionReason) {
    }

    public record CredentialDocument(String mimeType, Instant uploadedAt) {
    }

    public record CredentialResponse(UUID id, UUID specialistId, CredentialType credentialType, String title,
                                     String issuingOrganization, String credentialNumber, LocalDate issueDate,
                                     LocalDate expiryDate, CredentialDocument document,
                                     CredentialVerificationStatus verificationStatus, UUID reviewedBy,
                                     Instant reviewedAt, String rejectionReason, Instant createdAt,
                                     Instant updatedAt) {
        static CredentialResponse from(CredentialRow r) {
            CredentialDocument document = r.documentMimeType() == null ? null
                    : new CredentialDocument(r.documentMimeType(), r.documentUploadedAt().toInstant());
            return new CredentialResponse(r.id(), r.specialistId(), r.credentialType(), r.title(),
                    r.issuingOrganization(), r.credentialNumber(), r.issueDate(), r.expiryDate(), document,
                    r.verificationStatus(), r.reviewedBy(),
                    r.reviewedAt() == null ? null : r.reviewedAt().toInstant(), r.rejectionReason(),
                    r.createdAt().toInstant(), r.updatedAt().toInstant());
        }
    }

    public record DocumentLinkResponse(String url, Instant expiresAt) {
        static DocumentLinkResponse from(PresignedLink link) {
            return new DocumentLinkResponse(link.url(), link.expiresAt());
        }
    }
}