package com.nemouw.specialist.credential;

import java.time.LocalDate;

/** A validated, normalized credential write. */
record CredentialFields(CredentialType type, String title, String issuingOrganization, String credentialNumber,
                        LocalDate issueDate, LocalDate expiryDate) {
}