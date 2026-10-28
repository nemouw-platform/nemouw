package com.nemouw.syndicate.access;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

/** Admin, or the owner of the syndicate named by the {@code syndicateId} path variable. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasRole('admin') or @syndicateAccess.isOwnerOf(#syndicateId, authentication)")
public @interface AdminOrSyndicateOwner {
}