package com.nemouw.condition;

import com.nemouw.condition.ConditionDtos.ConditionResponse;
import com.nemouw.condition.ConditionDtos.ConditionWriteRequest;
import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.util.Validator;
import jakarta.persistence.criteria.Predicate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.nemouw.common.util.TextNormalizer.blankToNull;
import static com.nemouw.common.util.ValidationLimits.DESCRIPTION_MAX;
import static com.nemouw.common.util.ValidationLimits.TEXT_MAX;

@Service
class ConditionService {
    private static final Sort BY_NAME = Sort.by("name");

    private final ConditionRepository conditions;

    ConditionService(ConditionRepository conditions) {
        this.conditions = conditions;
    }

    @Transactional(readOnly = true)
    List<ConditionResponse> list(Boolean active, String search) {
        Specification<Condition> spec = (root, query, cb) -> {
            List<Predicate> filters = new ArrayList<>();
            if (active != null) filters.add(cb.equal(root.get("active"), active));
            if (search != null && !search.isBlank()) {
                String like = "%" + escapeLike(search.trim().toLowerCase(Locale.ROOT)) + "%";
                filters.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like, '\\'),
                        cb.like(cb.lower(root.get("label")), like, '\\')));
            }
            return cb.and(filters.toArray(Predicate[]::new));
        };
        return conditions.findAll(spec, BY_NAME).stream().map(ConditionResponse::from).toList();
    }

    @Transactional
    ConditionResponse create(ConditionWriteRequest req) {
        validate(req, true);
        if (conditions.existsByCode(normalizeCode(req.code()))) throw new ApiException(ErrorCode.CONDITION_CODE_TAKEN);

        Condition condition = Condition.create();
        apply(condition, req);
        try {
            return ConditionResponse.from(conditions.saveAndFlush(condition));
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.CONDITION_CODE_TAKEN);   // lost a race with another create
        }
    }

    @Transactional
    ConditionResponse update(UUID id, ConditionWriteRequest req) {
        validate(req, false);
        Condition condition = conditions.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (req.code() != null && conditions.existsByCodeAndIdNot(normalizeCode(req.code()), id)) {
            throw new ApiException(ErrorCode.CONDITION_CODE_TAKEN);
        }
        apply(condition, req);
        try {
            conditions.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.CONDITION_CODE_TAKEN);
        }
        return ConditionResponse.from(condition);
    }

    @Transactional
    void delete(UUID id) {
        Condition condition = conditions.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        try {
            conditions.delete(condition);
            conditions.flush();   // force the foreign key check now, not at commit
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.CONDITION_IN_USE);   // a specialist still treats it
        }
    }

    /** Makes %, _ and \ in the user's text match literally. */
    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static String normalizeCode(String code) {
        return code.trim().toLowerCase(Locale.ROOT);
    }

    /** Copies only the fields that were sent. Validation has already run. */
    private static void apply(Condition c, ConditionWriteRequest r) {
        if (r.code() != null) c.setCode(normalizeCode(r.code()));
        if (r.name() != null) c.setName(r.name().trim());
        if (r.label() != null) c.setLabel(r.label().trim());
        if (r.labelFr() != null) c.setLabelFr(blankToNull(r.labelFr()));
        if (r.labelAr() != null) c.setLabelAr(blankToNull(r.labelAr()));
        if (r.description() != null) c.setDescription(blankToNull(r.description()));
        if (r.active() != null) c.setActive(r.active());
    }

    private static void validate(ConditionWriteRequest r, boolean create) {
        new Validator()
                .requiredForWrite(create, "code", r.code())
                .requiredForWrite(create, "name", r.name())
                .requiredForWrite(create, "label", r.label())
                .code("code", r.code())
                .maxLength("name", r.name(), TEXT_MAX)
                .maxLength("label", r.label(), TEXT_MAX)
                .maxLength("label_fr", r.labelFr(), TEXT_MAX)
                .maxLength("label_ar", r.labelAr(), TEXT_MAX)
                .maxLength("description", r.description(), DESCRIPTION_MAX)
                .throwIfInvalid();
    }
}