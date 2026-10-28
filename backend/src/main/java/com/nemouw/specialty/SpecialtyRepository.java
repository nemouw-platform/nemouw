package com.nemouw.specialty;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpecialtyRepository extends JpaRepository<Specialty, UUID> {
    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    List<Specialty> findByActive(boolean active, Sort sort);
}