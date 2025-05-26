package com.citi.cms.controller;

import com.citi.cms.dto.request.TaskCompleteRequest;
import com.citi.cms.security.UserPrincipal;
import com.citi.cms.service.WorkflowService;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/workflow")
@CrossOrigin(origins = "*")
public class WorkflowController {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowController.class);

    @Autowired
    private WorkflowService workflowService;

    @PostMapping("/start/{caseId}")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'ADMIN')")
    public ResponseEntity<ProcessInstanceEvent> startWorkflow(
            @PathVariable Long caseId,
            @RequestBody Map<String, Object> variables,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Starting workflow for case ID: {} by user: {}", caseId, currentUser.getUsername());
        
        try {
            ProcessInstanceEvent processInstance = workflowService.startCaseWorkflow(caseId, variables);
            
            logger.info("Workflow started successfully. Process Instance Key: {}", 
                       processInstance.getProcessInstanceKey());
            
            return ResponseEntity.ok(processInstance);
            
        } catch (Exception e) {
            logger.error("Error starting workflow for case {}: {}", caseId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/complete-task")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'HR_SPECIALIST', 'LEGAL_COUNSEL', 'SECURITY_ANALYST', 'INVESTIGATOR', 'DIRECTOR', 'ADMIN')")
    public ResponseEntity<Void> completeTask(
            @Valid @RequestBody TaskCompleteRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Completing task with key: {} by user: {}", request.getTaskKey(), currentUser.getUsername());
        
        try {
            workflowService.completeTask(request.getTaskKey(), request.getVariables(), currentUser.getUserId());
            
            logger.info("Task completed successfully: {}", request.getTaskKey());
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            logger.error("Error completing task {}: {}", request.getTaskKey(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/complete-task/{taskKey}")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'HR_SPECIALIST', 'LEGAL_COUNSEL', 'SECURITY_ANALYST', 'INVESTIGATOR', 'DIRECTOR', 'ADMIN')")
    public ResponseEntity<Void> completeTaskWithKey(
            @PathVariable Long taskKey,
            @RequestBody Map<String, Object> variables,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Completing task with key: {} by user: {}", taskKey, currentUser.getUsername());
        
        try {
            workflowService.completeTask(taskKey, variables, currentUser.getUserId());
            
            logger.info("Task completed successfully: {}", taskKey);
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            logger.error("Error completing task {}: {}", taskKey, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/deploy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deployProcesses(@AuthenticationPrincipal UserPrincipal currentUser) {
        logger.info("Deploying BPMN processes and DMN tables by user: {}", currentUser.getUsername());
        
        try {
            workflowService.deployProcesses();
            
            logger.info("Processes deployed successfully by user: {}", currentUser.getUsername());
            
            return ResponseEntity.ok("Processes deployed successfully");
            
        } catch (Exception e) {
            logger.error("Error deploying processes: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Failed to deploy processes: " + e.getMessage());
        }
    }

    @GetMapping("/process-variables/{processInstanceKey}")
    @PreAuthorize("hasAnyRole('INTAKE_ANALYST', 'HR_SPECIALIST', 'LEGAL_COUNSEL', 'SECURITY_ANALYST', 'INVESTIGATOR', 'DIRECTOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getProcessVariables(
            @PathVariable Long processInstanceKey,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Retrieving process variables for instance key: {} by user: {}", 
                   processInstanceKey, currentUser.getUsername());
        
        try {
            Map<String, Object> variables = workflowService.getProcessVariables(processInstanceKey);
            
            logger.info("Process variables retrieved successfully for instance: {}", processInstanceKey);
            
            return ResponseEntity.ok(variables);
            
        } catch (Exception e) {
            logger.error("Error retrieving process variables for instance {}: {}", 
                        processInstanceKey, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        logger.info("Workflow service health check");
        return ResponseEntity.ok("Workflow service is running");
    }

    @GetMapping("/version")
    public ResponseEntity<String> getVersion() {
        logger.info("Workflow service version check");
        return ResponseEntity.ok("Workflow Service Version 1.0.0");
    }
}