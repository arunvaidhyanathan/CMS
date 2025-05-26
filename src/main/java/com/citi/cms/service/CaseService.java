package com.citi.cms.service;

import com.citi.cms.dto.request.CaseCreateRequest;
import com.citi.cms.dto.response.CaseResponse;
import com.citi.cms.dto.response.WorkItemResponse;
import com.citi.cms.entity.Case;

import java.util.List;

public interface CaseService {
    CaseResponse createCase(CaseCreateRequest request, Long userId);
    CaseResponse getCaseById(Long caseId);
    CaseResponse getCaseByCaseId(Long caseId);
    List<WorkItemResponse> getWorkItemsForUser(Long userId);
    void assignCase(Long caseId, Long userId);
    List<CaseResponse> getCasesForUser(Long userId);
    void updateCaseStatus(Long caseId, String status);
    String generateCaseNumber();
}