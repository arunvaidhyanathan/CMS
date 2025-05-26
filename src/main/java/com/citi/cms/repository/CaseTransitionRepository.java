package com.citi.cms.repository;

import com.citi.cms.entity.CaseTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseTransitionRepository extends JpaRepository<CaseTransition, Long> {
    List<CaseTransition> findByCaseEntityCaseIdOrderByTransitionDateDesc(Long caseId);
    List<CaseTransition> findByPerformedByUserId(Long userId);
}