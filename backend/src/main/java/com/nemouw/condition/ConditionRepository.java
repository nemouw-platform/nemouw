package com.nemouw.condition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

interface ConditionRepository extends JpaRepository<Condition, UUID>, JpaSpecificationExecutor<Condition> {
    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);
}