package com.citi.cms.service.impl;

import com.citi.cms.dto.request.CaseCreateRequest;
import com.citi.cms.dto.response.CaseResponse;
import com.citi.cms.dto.response.WorkItemResponse;
import com.citi.cms.entity.*;
import com.citi.cms.repository.*;
import com.citi.cms.service.CaseService;
import com.citi.cms.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class CaseServiceImpl implements CaseService {

    private static final Logger logger = LoggerFactory.getLogger(CaseServiceImpl.class);

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private WorkItemRepository workItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaseTypeRepository caseTypeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private WorkflowService workflowService;

    @Override
    public CaseResponse createCase(CaseCreateRequest request, Long userId) {
        logger.info("Creating new case for user: {}", userId);

        try {
            User createdBy = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            // Generate case number
            String caseNumber = generateCaseNumber();

            // Find case type
            CaseType caseType = caseTypeRepository.findByTypeCode(request.getCaseTypeCode())
                    .orElseThrow(() -> new RuntimeException("Case type not found: " + request.getCaseTypeCode()));

            // Create case entity
            Case caseEntity = new Case();
            caseEntity.setCaseNumber(caseNumber);
            caseEntity.setTitle(request.getTitle());
            caseEntity.setDescription(request.getDescription());
            caseEntity.setCaseType(caseType);
            caseEntity.setPriority(Priority.valueOf(request.getPriority()));
            caseEntity.setComplainantName(request.getComplainantName());
            caseEntity.setComplainantEmail(request.getComplainantEmail());
            caseEntity.setCreatedBy(createdBy);
            caseEntity.setStatus(CaseStatus.OPEN);

            // Set case ID (business ID)
            Long nextCaseId = getNextCaseId();
            caseEntity.setCaseId(nextCaseId);

            // Save case
            Case savedCase = caseRepository.save(caseEntity);

            // Start workflow
            Map<String, Object> workflowVariables = new HashMap<>();
            workflowVariables.put("caseId", savedCase.getCaseId());
            workflowVariables.put("caseNumber", savedCase.getCaseNumber());
            workflowVariables.put("caseTitle", savedCase.getTitle());
            workflowVariables.put("allegationType", request.getAllegationType());
            workflowVariables.put("severity", request.getSeverity());
            workflowVariables.put("complainantName", savedCase.getComplainantName());
            workflowVariables.put("complainantEmail", savedCase.getComplainantEmail());
            workflowVariables.put("priority", savedCase.getPriority().name());

            workflowService.startCaseWorkflow(savedCase.getCaseId(), workflowVariables);

            logger.info("Case created successfully: {}", savedCase.getCaseNumber());

            return convertToResponse(savedCase);

        } catch (Exception e) {
            logger.error("Error creating case: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create case", e);
        }
    }

    @Override
    public CaseResponse getCaseById(Long id) {
        Case caseEntity = caseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Case not found: " + id));
        return convertToResponse(caseEntity);
    }

    @Override
    public CaseResponse getCaseByCaseId(Long caseId) {
        Case caseEntity = caseRepository.findByCaseId(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));
        return convertToResponse(caseEntity);
    }

    @Override
    public List<WorkItemResponse> getWorkItemsForUser(Long userId) {
        List<WorkItem> workItems = workItemRepository.findByAssignedToUserIdAndStatus(userId, "PENDING");
        return workItems.stream()
                .map(this::convertToWorkItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void assignCase(Long caseId, Long userId) {
        Case caseEntity = caseRepository.findByCaseId(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        User assignedUser = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        caseEntity.setAssignedTo(assignedUser);
        caseRepository.save(caseEntity);
    }

    @Override
    public List<CaseResponse> getCasesForUser(Long userId) {
        List<Case> cases = caseRepository.findByAssignedToUserId(userId);
        return cases.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void updateCaseStatus(Long caseId, String status) {
        Case caseEntity = caseRepository.findByCaseId(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        caseEntity.setStatus(CaseStatus.valueOf(status));
        caseRepository.save(caseEntity);
    }

    @Override
    public String generateCaseNumber() {
        int currentYear = LocalDateTime.now().getYear();
        long count = caseRepository.countByYear(currentYear);
        return String.format("CMS-%d-%06d", currentYear, count + 1);
    }

    private Long getNextCaseId() {
        // This should use the sequence from the database
        // For now, we'll use a simple approach
        Long maxCaseId = caseRepository.findMaxCaseId();
        return (maxCaseId != null ? maxCaseId : 0L) + 1;
    }

    private CaseResponse convertToResponse(Case caseEntity) {
        CaseResponse response = new CaseResponse();
        response.setId(caseEntity.getId());
        response.setCaseId(caseEntity.getCaseId());
        response.setCaseNumber(caseEntity.getCaseNumber());
        response.setTitle(caseEntity.getTitle());
        response.setDescription(caseEntity.getDescription());
        response.setPriority(caseEntity.getPriority().name());
        response.setStatus(caseEntity.getStatus().name());
        response.setComplainantName(caseEntity.getComplainantName());
        response.setComplainantEmail(caseEntity.getComplainantEmail());
        response.setCreatedAt(caseEntity.getCreatedAt());
        response.setUpdatedAt(caseEntity.getUpdatedAt());

        if (caseEntity.getCaseType() != null) {
            response.setCaseTypeName(caseEntity.getCaseType().getTypeName());
        }

        if (caseEntity.getAssignedTo() != null) {
            response.setAssignedToName(caseEntity.getAssignedTo().getFullName());
            response.setAssignedToUserId(caseEntity.getAssignedTo().getUserId());
        }

        if (caseEntity.getCreatedBy() != null) {
            response.setCreatedByName(caseEntity.getCreatedBy().getFullName());
        }

        return response;
    }

    private WorkItemResponse convertToWorkItemResponse(WorkItem workItem) {
        WorkItemResponse response = new WorkItemResponse();
        response.setWorkItemId(workItem.getWorkItemId());
        response.setTaskName(workItem.getTaskName());
        response.setTaskType(workItem.getTaskType());
        response.setStatus(workItem.getStatus());
        response.setDueDate(workItem.getDueDate());
        response.setCreatedAt(workItem.getCreatedAt());

        if (workItem.getCaseEntity() != null) {
            response.setCaseId(workItem.getCaseEntity().getCaseId());
            response.setCaseNumber(workItem.getCaseEntity().getCaseNumber());
            response.setCaseTitle(workItem.getCaseEntity().getTitle());
            response.setCasePriority(workItem.getCaseEntity().getPriority().name());
        }

        return response;
    }
}