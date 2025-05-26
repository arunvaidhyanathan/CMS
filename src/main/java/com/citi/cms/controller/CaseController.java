package com.citi.cms.controller;

import com.citi.cms.dto.request.CaseCreateRequest;
import com.citi.cms.dto.request.TaskCompleteRequest;
import com.citi.cms.dto.response.CaseResponse;
import com.citi.cms.dto.response.WorkItemResponse;
import com.citi.cms.security.UserPrincipal;
import com.citi.cms.service.CaseService;
import com.citi.cms.service.WorkflowService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cases")
@CrossOrigin(origins = "*")
public class CaseController {

    private static final Logger logger = LoggerFactory.getLogger(CaseController.class);

    @Autowired
    private CaseService caseService;

    @Autowired
    private WorkflowService workflowService;

    @PostMapping
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'ADMIN')")
    public ResponseEntity<CaseResponse> createCase(
            @Valid @RequestBody CaseCreateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Creating case: {} by user: {}", request.getTitle(), currentUser.getUsername());
        
        CaseResponse response = caseService.createCase(request, currentUser.getUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{caseId}")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'HR_SPECIALIST', 'LEGAL_COUNSEL', 'SECURITY_ANALYST', 'INVESTIGATOR', 'DIRECTOR', 'ADMIN')")
    public ResponseEntity<CaseResponse> getCaseDetails(@PathVariable Long caseId) {
        logger.info("Retrieving case details for case ID: {}", caseId);
        
        CaseResponse response = caseService.getCaseByCaseId(caseId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/workitems/{userId}")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'HR_SPECIALIST', 'LEGAL_COUNSEL', 'SECURITY_ANALYST', 'INVESTIGATOR', 'IU_MANAGER', 'DIRECTOR', 'ADMIN')")
    public ResponseEntity<List<WorkItemResponse>> getWorkItemsForUser(@PathVariable Long userId) {
        logger.info("Retrieving work items for user ID: {}", userId);
        
        List<WorkItemResponse> workItems = caseService.getWorkItemsForUser(userId);
        return ResponseEntity.ok(workItems);
    }
    @PostMapping("/{caseId}/assign")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'ADMIN')")
    public ResponseEntity<Void> assignCase(
            @PathVariable Long caseId,
            @RequestParam Long userId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Assigning case ID: {} to user ID: {} by user: {}", caseId, userId, currentUser.getUsername());
        
        caseService.assignCase(caseId, userId);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'HR_SPECIALIST', 'LEGAL_COUNSEL', 'SECURITY_ANALYST', 'INVESTIGATOR', 'IU_MANAGER', 'DIRECTOR', 'ADMIN')")
    public ResponseEntity<List<CaseResponse>> getCasesForUser(@PathVariable Long userId) {
        logger.info("Retrieving cases for user ID: {}", userId);
        
        List<CaseResponse> cases = caseService.getCasesForUser(userId);
        return ResponseEntity.ok(cases);
    }
    @PostMapping("/{caseId}/status")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'HR_SPECIALIST', 'LEGAL_COUNSEL', 'SECURITY_ANALYST', 'INVESTIGATOR', 'DIRECTOR', 'ADMIN')")
    public ResponseEntity<Void> updateCaseStatus(
            @PathVariable Long caseId,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Updating status for case ID: {} to {} by user: {}", caseId, status, currentUser.getUsername());
        
        caseService.updateCaseStatus(caseId, status);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/{caseId}/complete")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'HR_SPECIALIST', 'LEGAL_COUNSEL', 'SECURITY_ANALYST', 'INVESTIGATOR', 'DIRECTOR', 'ADMIN')")
    public ResponseEntity<Void> completeTask(
            @PathVariable Long caseId,
            @Valid @RequestBody TaskCompleteRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Completing task for case ID: {} by user: {}", caseId, currentUser.getUsername());
        
        workflowService.completeTask(caseId, request, currentUser.getUserId());
        return ResponseEntity.ok().build();
    }
    @GetMapping("/generate-case-number")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'ADMIN')")
    public ResponseEntity<String> generateCaseNumber() {
        logger.info("Generating new case number");
        
        String caseNumber = caseService.generateCaseNumber();
        return ResponseEntity.ok(caseNumber);
    }
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        logger.info("Health check endpoint hit");
        return ResponseEntity.ok("CMS API is running");
    }
    @GetMapping("/version")
    public ResponseEntity<String> getVersion() {
        logger.info("Version endpoint hit");
        return ResponseEntity.ok("CMS API Version 1.0.0");
    }
    @GetMapping("/processes/deploy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deployProcesses() {
        logger.info("Deploying BPMN processes and DMN tables");
        
        workflowService.deployProcesses();
        return ResponseEntity.ok("Processes deployed successfully");
    }
    @GetMapping("/processes/variables/{processInstanceKey}")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'HR_SPECIALIST', 'LEGAL_COUNSEL', 'SECURITY_ANALYST', 'INVESTIGATOR', 'DIRECTOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getProcessVariables(@PathVariable Long processInstanceKey) {
        logger.info("Retrieving process variables for instance key: {}", processInstanceKey);
        
        Map<String, Object> variables = workflowService.getProcessVariables(processInstanceKey);
        return ResponseEntity.ok(variables);
    }
    