package com.citi.cms.controller;

import com.citi.cms.dto.request.CaseCreateRequest;
import com.citi.cms.dto.request.TaskCompleteRequest;
import com.citi.cms.dto.response.CaseResponse;
import com.citi.cms.dto.response.WorkItemResponse;
import com.citi.cms.security.UserPrincipal;
import com.citi.cms.service.CaseService;
import com.citi.cms.service.WorkflowService;
import com.citi.cms.util.Constants;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cases")
@CrossOrigin(origins = "*")
public class CaseController {

    private static final Logger logger = LoggerFactory.getLogger(CaseController.class);

    @Autowired
    private CaseService caseService;

    @Autowired
    private WorkflowService workflowService;

    /**
     * Create a new case
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'ADMIN')")
    public ResponseEntity<CaseResponse> createCase(
            @Valid @RequestBody CaseCreateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Creating case: {} by user: {}", request.getTitle(), currentUser.getUsername());
        
        try {
            CaseResponse response = caseService.createCase(request, currentUser.getUserId());
            
            logger.info("Case created successfully: {} by user: {}", 
                       response.getCaseNumber(), currentUser.getUsername());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            logger.error("Error creating case: {} by user: {}: {}", 
                        request.getTitle(), currentUser.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get case details by case ID
     */
    @GetMapping("/{caseId}")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'HR_SPECIALIST', 'LEGAL_COUNSEL', 'SECURITY_ANALYST', 'INVESTIGATOR', 'IU_MANAGER', 'DIRECTOR', 'ADMIN')")
    public ResponseEntity<CaseResponse> getCaseDetails(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Retrieving case details for case ID: {} by user: {}", caseId, currentUser.getUsername());
        
        try {
            CaseResponse response = caseService.getCaseByCaseId(caseId);
            
            logger.info("Case details retrieved successfully for case: {} by user: {}", 
                       response.getCaseNumber(), currentUser.getUsername());
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("Case not found: {} by user: {}: {}", caseId, currentUser.getUsername(), e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error retrieving case details for case ID: {} by user: {}: {}", 
                        caseId, currentUser.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get work items for a specific user
     */
    @GetMapping("/workitems/{userId}")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'HR_SPECIALIST', 'LEGAL_COUNSEL', 'SECURITY_ANALYST', 'INVESTIGATOR', 'IU_MANAGER', 'DIRECTOR', 'ADMIN') and (#userId == authentication.principal.userId or hasRole('ADMIN'))")
    public ResponseEntity<List<WorkItemResponse>> getWorkItemsForUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "PENDING") String status,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Retrieving work items for user ID: {} with status: {} by user: {}", 
                   userId, status, currentUser.getUsername());
        
        try {
            List<WorkItemResponse> workItems = caseService.getWorkItemsForUser(userId, status);
            
            logger.info("Retrieved {} work items for user ID: {} by user: {}", 
                       workItems.size(), userId, currentUser.getUsername());
            
            return ResponseEntity.ok(workItems);
            
        } catch (Exception e) {
            logger.error("Error retrieving work items for user ID: {} by user: {}: {}", 
                        userId, currentUser.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get current user's work items
     */
    @GetMapping("/my-workitems")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'HR_SPECIALIST', 'LEGAL_COUNSEL', 'SECURITY_ANALYST', 'INVESTIGATOR', 'IU_MANAGER', 'DIRECTOR', 'ADMIN')")
    public ResponseEntity<List<WorkItemResponse>> getMyWorkItems(
            @RequestParam(defaultValue = "PENDING") String status,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Retrieving work items for current user: {} with status: {}", currentUser.getUsername(), status);
        
        try {
            List<WorkItemResponse> workItems = caseService.getWorkItemsForUser(currentUser.getUserId(), status);
            
            logger.info("Retrieved {} work items for current user: {}", workItems.size(), currentUser.getUsername());
            
            return ResponseEntity.ok(workItems);
            
        } catch (Exception e) {
            logger.error("Error retrieving work items for current user: {}: {}", currentUser.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Assign case to a user
     */
    @PostMapping("/{caseId}/assign")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'IU_MANAGER', 'DIRECTOR', 'ADMIN')")
    public ResponseEntity<Void> assignCase(
            @PathVariable Long caseId,
            @RequestParam Long userId,
            @RequestParam(required = false) String comments,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Assigning case ID: {} to user ID: {} by user: {}", caseId, userId, currentUser.getUsername());
        
        try {
            caseService.assignCase(caseId, userId, comments);
            
            logger.info("Case ID: {} assigned to user ID: {} successfully by user: {}", 
                       caseId, userId, currentUser.getUsername());
            
            return ResponseEntity.ok().build();
            
        } catch (RuntimeException e) {
            logger.error("Error assigning case ID: {} to user ID: {} by user: {}: {}", 
                        caseId, userId, currentUser.getUsername(), e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Unexpected error assigning case ID: {} to user ID: {} by user: {}: {}", 
                        caseId, userId, currentUser.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get cases assigned to a specific user
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'HR_SPECIALIST', 'LEGAL_COUNSEL', 'SECURITY_ANALYST', 'INVESTIGATOR', 'IU_MANAGER', 'DIRECTOR', 'ADMIN') and (#userId == authentication.principal.userId or hasRole('ADMIN'))")
    public ResponseEntity<List<CaseResponse>> getCasesForUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Retrieving cases for user ID: {} by user: {}", userId, currentUser.getUsername());
        
        try {
            List<CaseResponse> cases = caseService.getCasesForUser(userId);
            
            logger.info("Retrieved {} cases for user ID: {} by user: {}", 
                       cases.size(), userId, currentUser.getUsername());
            
            return ResponseEntity.ok(cases);
            
        } catch (Exception e) {
            logger.error("Error retrieving cases for user ID: {} by user: {}: {}", 
                        userId, currentUser.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get current user's assigned cases
     */
    @GetMapping("/my-cases")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'HR_SPECIALIST', 'LEGAL_COUNSEL', 'SECURITY_ANALYST', 'INVESTIGATOR', 'IU_MANAGER', 'DIRECTOR', 'ADMIN')")
    public ResponseEntity<List<CaseResponse>> getMyCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Retrieving cases for current user: {}", currentUser.getUsername());
        
        try {
            List<CaseResponse> cases = caseService.getCasesForUser(currentUser.getUserId());
            
            logger.info("Retrieved {} cases for current user: {}", cases.size(), currentUser.getUsername());
            
            return ResponseEntity.ok(cases);
            
        } catch (Exception e) {
            logger.error("Error retrieving cases for current user: {}: {}", currentUser.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update case status
     */
    @PostMapping("/{caseId}/status")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'HR_SPECIALIST', 'LEGAL_COUNSEL', 'SECURITY_ANALYST', 'INVESTIGATOR', 'IU_MANAGER', 'DIRECTOR', 'ADMIN')")
    public ResponseEntity<Void> updateCaseStatus(
            @PathVariable Long caseId,
            @RequestParam String status,
            @RequestParam(required = false) String comments,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Updating status for case ID: {} to {} by user: {}", caseId, status, currentUser.getUsername());
        
        try {
            caseService.updateCaseStatus(caseId, status, comments, currentUser.getUserId());
            
            logger.info("Status updated successfully for case ID: {} to {} by user: {}", 
                       caseId, status, currentUser.getUsername());
            
            return ResponseEntity.ok().build();
            
        } catch (RuntimeException e) {
            logger.error("Error updating status for case ID: {} by user: {}: {}", 
                        caseId, currentUser.getUsername(), e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Unexpected error updating status for case ID: {} by user: {}: {}", 
                        caseId, currentUser.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Complete a workflow task
     */
    @PostMapping("/{caseId}/complete")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'HR_SPECIALIST', 'LEGAL_COUNSEL', 'SECURITY_ANALYST', 'INVESTIGATOR', 'IU_MANAGER', 'DIRECTOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> completeTask(
            @PathVariable Long caseId,
            @Valid @RequestBody TaskCompleteRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Completing task for case ID: {} by user: {}", caseId, currentUser.getUsername());
        
        try {
            // Add case ID and user information to variables
            request.getVariables().put("caseId", caseId);
            request.getVariables().put("completedByUserId", currentUser.getUserId());
            request.getVariables().put("completedByUsername", currentUser.getUsername());
            request.getVariables().put("completionDate", LocalDateTime.now().toString());
            
            workflowService.completeTask(caseId, request, currentUser.getUserId());
            
            logger.info("Task completed successfully for case ID: {} by user: {}", 
                       caseId, currentUser.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task completed successfully");
            response.put("caseId", caseId);
            response.put("taskKey", request.getTaskKey());
            response.put("completedAt", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            logger.error("Error completing task for case ID: {} by user: {}: {}", 
                        caseId, currentUser.getUsername(), e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to complete task: " + e.getMessage());
            errorResponse.put("caseId", caseId);
            
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error completing task for case ID: {} by user: {}: {}", 
                        caseId, currentUser.getUsername(), e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Internal server error occurred");
            errorResponse.put("caseId", caseId);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Search cases with various filters
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'HR_SPECIALIST', 'LEGAL_COUNSEL', 'SECURITY_ANALYST', 'INVESTIGATOR', 'IU_MANAGER', 'DIRECTOR', 'ADMIN')")
    public ResponseEntity<Page<CaseResponse>> searchCases(
            @RequestParam(required = false) String caseNumber,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String allegationType,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Searching cases with filters by user: {}", currentUser.getUsername());
        
        try {
            // Create search criteria
            Map<String, Object> searchCriteria = new HashMap<>();
            if (caseNumber != null && !caseNumber.trim().isEmpty()) searchCriteria.put("caseNumber", caseNumber);
            if (status != null && !status.trim().isEmpty()) searchCriteria.put("status", status);
            if (priority != null && !priority.trim().isEmpty()) searchCriteria.put("priority", priority);
            if (allegationType != null && !allegationType.trim().isEmpty()) searchCriteria.put("allegationType", allegationType);
            if (assignedTo != null && !assignedTo.trim().isEmpty()) searchCriteria.put("assignedTo", assignedTo);
            if (createdBy != null && !createdBy.trim().isEmpty()) searchCriteria.put("createdBy", createdBy);
            if (fromDate != null && !fromDate.trim().isEmpty()) searchCriteria.put("fromDate", fromDate);
            if (toDate != null && !toDate.trim().isEmpty()) searchCriteria.put("toDate", toDate);
            
            // Create pageable
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, Math.min(size, Constants.MAX_PAGE_SIZE), Sort.by(direction, sortBy));
            
            Page<CaseResponse> cases = caseService.searchCases(searchCriteria, pageable);
            
            logger.info("Found {} cases matching search criteria by user: {}", 
                       cases.getTotalElements(), currentUser.getUsername());
            
            return ResponseEntity.ok(cases);
            
        } catch (Exception e) {
            logger.error("Error searching cases by user: {}: {}", currentUser.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate a new case number
     */
    @GetMapping("/generate-case-number")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'ADMIN')")
    public ResponseEntity<Map<String, String>> generateCaseNumber(@AuthenticationPrincipal UserPrincipal currentUser) {
        logger.info("Generating new case number by user: {}", currentUser.getUsername());
        
        try {
            String caseNumber = caseService.generateCaseNumber();
            
            logger.info("Generated case number: {} by user: {}", caseNumber, currentUser.getUsername());
            
            Map<String, String> response = new HashMap<>();
            response.put("caseNumber", caseNumber);
            response.put("generatedAt", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error generating case number by user: {}: {}", currentUser.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get case statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('IU_MANAGER', 'DIRECTOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getCaseStatistics(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String groupBy,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Retrieving case statistics by user: {}", currentUser.getUsername());
        
        try {
            Map<String, Object> statistics = caseService.getCaseStatistics(fromDate, toDate, groupBy);
            
            logger.info("Case statistics retrieved by user: {}", currentUser.getUsername());
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Error retrieving case statistics by user: {}: {}", currentUser.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Export cases to CSV/Excel
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('IU_MANAGER', 'DIRECTOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> exportCases(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Exporting cases in {} format by user: {}", format, currentUser.getUsername());
        
        try {
            Map<String, Object> exportResult = caseService.exportCases(format, status, fromDate, toDate);
            
            logger.info("Cases exported successfully in {} format by user: {}", format, currentUser.getUsername());
            
            return ResponseEntity.ok(exportResult);
            
        } catch (Exception e) {
            logger.error("Error exporting cases by user: {}: {}", currentUser.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get case audit trail
     */
    @GetMapping("/{caseId}/audit-trail")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'HR_SPECIALIST', 'LEGAL_COUNSEL', 'SECURITY_ANALYST', 'INVESTIGATOR', 'IU_MANAGER', 'DIRECTOR', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getCaseAuditTrail(
            @PathVariable Long caseId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Retrieving audit trail for case ID: {} by user: {}", caseId, currentUser.getUsername());
        
        try {
            List<Map<String, Object>> auditTrail = caseService.getCaseAuditTrail(caseId);
            
            logger.info("Retrieved {} audit entries for case ID: {} by user: {}", 
                       auditTrail.size(), caseId, currentUser.getUsername());
            
            return ResponseEntity.ok(auditTrail);
            
        } catch (RuntimeException e) {
            logger.error("Case not found for audit trail: {} by user: {}: {}", 
                        caseId, currentUser.getUsername(), e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error retrieving audit trail for case ID: {} by user: {}: {}", 
                        caseId, currentUser.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        logger.debug("Case controller health check");
        
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "CaseController");
        health.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(health);
    }

    /**
     * Version information endpoint
     */
    @GetMapping("/version")
    public ResponseEntity<Map<String, String>> getVersion() {
        logger.debug("Case controller version check");
        
        Map<String, String> version = new HashMap<>();
        version.put("application", Constants.APPLICATION_NAME);
        version.put("version", Constants.APPLICATION_VERSION);
        version.put("apiVersion", Constants.API_VERSION);
        version.put("service", "CaseController");
        
        return ResponseEntity.ok(version);
    }

    /**
     * Deploy processes endpoint (Admin only)
     */
    @PostMapping("/processes/deploy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deployProcesses(@AuthenticationPrincipal UserPrincipal currentUser) {
        logger.info("Deploying BPMN processes and DMN tables by admin: {}", currentUser.getUsername());
        
        try {
            workflowService.deployProcesses();
            
            logger.info("Processes deployed successfully by admin: {}", currentUser.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Processes deployed successfully");
            response.put("deployedAt", LocalDateTime.now().toString());
            response.put("deployedBy", currentUser.getUsername());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error deploying processes by admin: {}: {}", currentUser.getUsername(), e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to deploy processes: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get process variables for a case
     */
    @GetMapping("/processes/variables/{processInstanceKey}")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'HR_SPECIALIST', 'LEGAL_COUNSEL', 'SECURITY_ANALYST', 'INVESTIGATOR', 'IU_MANAGER', 'DIRECTOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getProcessVariables(
            @PathVariable Long processInstanceKey,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Retrieving process variables for instance key: {} by user: {}", 
                   processInstanceKey, currentUser.getUsername());
        
        try {
            Map<String, Object> variables = workflowService.getProcessVariables(processInstanceKey);
            
            logger.info("Process variables retrieved successfully for instance: {} by user: {}", 
                       processInstanceKey, currentUser.getUsername());
            
            return ResponseEntity.ok(variables);
            
        } catch (Exception e) {
            logger.error("Error retrieving process variables for instance {} by user: {}: {}", 
                        processInstanceKey, currentUser.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}