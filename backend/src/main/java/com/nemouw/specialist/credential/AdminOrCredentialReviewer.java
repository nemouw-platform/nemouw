package com.nemouw.specialist.credential;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

/** Admin, or staff of the syndicate the credential's specialist belongs to. Needs a {@code credentialId} path variable. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasRole('admin') or @credentialReviewAccess.isReviewer(#credentialId, authentication)")
public @interface AdminOrCredentialReviewer {
}