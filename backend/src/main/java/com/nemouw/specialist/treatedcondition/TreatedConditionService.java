package com.nemouw.specialist.treatedcondition;

import com.nemouw.common.exception.Errors;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.util.Validator;
import com.nemouw.specialist.treatedcondition.TreatedConditionDtos.TreatedConditionsWriteRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
class TreatedConditionService {
    private final TreatedConditionRepository treated;

    TreatedConditionService(TreatedConditionRepository treated) {
        this.treated = treated;
    }

    @Transactional(readOnly = true)
    List<UUID> list(UUID specialistId) {
        return treated.findIds(specialistId);
    }

    @Transactional
    List<UUID> replace(UUID specialistId, TreatedConditionsWriteRequest req) {
        List<UUID> sent = req.conditionIds();
        new Validator()
                .notNull("condition_ids", sent)
                .check(sent == null || sent.stream().noneMatch(Objects::isNull),
                        FieldErrors.invalidFormat("condition_ids", "must not contain null"))
                .throwIfInvalid();

        Set<UUID> conditionIds = new LinkedHashSet<>(sent);
        if (!treated.allExist(conditionIds)) {
            throw Errors.invalidField("condition_ids", "contains an unknown condition");
        }
        treated.replace(specialistId, conditionIds);
        return treated.findIds(specialistId);
    }
}