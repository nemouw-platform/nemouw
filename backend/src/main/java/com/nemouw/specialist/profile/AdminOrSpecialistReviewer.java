package com.nemouw.specialist.profile;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

/** Admin, or staff of the syndicate the specialist belongs to. Needs a {@code specialistId} path variable. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasRole('admin') or @specialistReviewAccess.isReviewer(#specialistId, authentication)")
public @interface AdminOrSpecialistReviewer {
}