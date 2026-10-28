package com.nemouw.syndicate.access;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

/** Admin, or staff of the syndicate named by the {@code syndicateId} path variable. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasRole('admin') or @syndicateAccess.isStaffOf(#syndicateId, authentication)")
public @interface AdminOrSyndicateStaff {
}