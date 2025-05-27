package com.citi.cms.service;

import com.citi.cms.dto.request.CaseCreateRequest;
import com.citi.cms.dto.response.CaseResponse;
import com.citi.cms.dto.response.WorkItemResponse;


import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CaseService {
    CaseResponse createCase(CaseCreateRequest request, Long userId);
    CaseResponse getCaseById(Long caseId);
    CaseResponse getCaseByCaseId(Long caseId);
    List<WorkItemResponse> getWorkItemsForUser(Long userId,String status);
    void assignCase(Long caseId, Long userId, String comments);
    List<CaseResponse> getCasesForUser(Long userId);
    void updateCaseStatus(Long caseId, String status, String comments, Long long1);
    String generateCaseNumber();
    List<Map<String, Object>> getCaseAuditTrail(Long caseId);
    Page<CaseResponse> searchCases(Map<String, Object> searchCriteria, Pageable pageable);
    Map<String, Object> getCaseStatistics(String fromDate, String toDate, String groupBy);
    Map<String, Object> exportCases(String format, String status, String fromDate, String toDate);
}