package com.nemouw.specialist.treatedcondition;

import java.util.List;
import java.util.UUID;

public final class TreatedConditionDtos {
    private TreatedConditionDtos() {
    }

    public record TreatedConditionsWriteRequest(List<UUID> conditionIds) {
    }
}