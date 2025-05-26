package com.citi.cms.repository;

import com.citi.cms.entity.CaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseTypeRepository extends JpaRepository<CaseType, Long> {
    Optional<CaseType> findByCaseTypeId(Long caseTypeId);
    Optional<CaseType> findByTypeCode(String typeCode);
    List<CaseType> findByIsActiveTrue();
    boolean existsByTypeCode(String typeCode);
}