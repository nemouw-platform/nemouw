package com.nemouw.country;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface CountryRepository extends JpaRepository<Country, UUID> {
    boolean existsByIsoCode(String isoCode);

    boolean existsByIsoCodeAndIdNot(String isoCode, UUID id);

    List<Country> findByActive(boolean active, Sort sort);
}