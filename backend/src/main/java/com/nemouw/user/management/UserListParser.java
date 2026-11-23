package com.nemouw.user.management;

import com.nemouw.common.enums.UserRole;
import com.nemouw.common.enums.UserStatus;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.query.QueryParser;
import com.nemouw.common.util.TextNormalizer;
import com.nemouw.common.util.Validator;
import com.nemouw.user.management.ManagedUserRepository.Filter;
import org.springframework.util.MultiValueMap;

import java.util.Optional;
import java.util.function.Function;

import static com.nemouw.common.util.ValidationLimits.EMAIL_MAX_LENGTH;

/** Validates the list query parameters; reports every bad parameter at once. */
final class UserListParser {
    static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PAGE = 10_000;

    record Query(Filter filter, int page, int pageSize) {
        int offset() {
            return (page - 1) * pageSize;
        }
    }

    private UserListParser() {
    }

    static Query parse(MultiValueMap<String, String> params) {
        Validator v = new Validator();
        Integer page = QueryParser.integer(params, "page", 1, MAX_PAGE, v);
        Integer pageSize = QueryParser.integer(params, "page_size", 1, MAX_PAGE_SIZE, v);
        UserRole role = option(params, "role", UserRole::tryFrom, v);
        UserStatus status = option(params, "user_status", UserStatus::tryFrom, v);
        String search = TextNormalizer.blankToNull(params.getFirst("search"));
        v.maxLength("search", search, EMAIL_MAX_LENGTH);
        v.throwIfInvalid();

        return new Query(new Filter(role, status, search),
                page == null ? 1 : page,
                pageSize == null ? QueryParser.DEFAULT_PAGE_SIZE : pageSize);
    }

    private static <E> E option(MultiValueMap<String, String> params, String name,
                                Function<String, Optional<E>> lookup, Validator v) {
        String raw = params.getFirst(name);
        if (raw == null || raw.isBlank()) return null;
        Optional<E> value = lookup.apply(raw.trim());
        v.check(value.isPresent(), FieldErrors.invalidFormat(name, "is not a valid value"));
        return value.orElse(null);
    }
}