package com.citi.cms.repository;

import com.citi.cms.entity.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseRepository extends JpaRepository<Case, Long> {
    
    Optional<Case> findByCaseId(Long caseId);
    Optional<Case> findByCaseNumber(String caseNumber);
    List<Case> findByAssignedToUserId(Long userId);
    List<Case> findByStatus(String status);
    
    @Query("SELECT COUNT(c) FROM Case c WHERE YEAR(c.createdAt) = :year")
    long countByYear(@Param("year") int year);
    
    @Query("SELECT MAX(c.caseId) FROM Case c")
    Long findMaxCaseId();
    
    @Query("SELECT c FROM Case c WHERE c.assignedTo.userId = :userId OR c.createdBy.userId = :userId")
    List<Case> findCasesForUser(@Param("userId") Long userId);
}