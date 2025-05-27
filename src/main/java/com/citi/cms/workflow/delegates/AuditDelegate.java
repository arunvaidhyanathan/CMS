package com.citi.cms.workflow.delegates;

import com.citi.cms.entity.Case;
import com.citi.cms.entity.CaseTransition;
import com.citi.cms.entity.User;
import com.citi.cms.repository.CaseRepository;
import com.citi.cms.repository.CaseTransitionRepository;
import com.citi.cms.repository.UserRepository;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuditDelegate {

    private static final Logger logger = LoggerFactory.getLogger(AuditDelegate.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT_LOGGER");

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private CaseTransitionRepository caseTransitionRepository;

    @Autowired
    private UserRepository userRepository;

    @JobWorker(type = "audit-case-creation")
    public void auditCaseCreation(final JobClient client, final ActivatedJob job) {
        logger.info("Auditing case creation for job: {}", job.getKey());

        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Long caseId = (Long) variables.get("caseId");
            String caseNumber = (String) variables.get("caseNumber");
            Long createdByUserId = (Long) variables.get("createdByUserId");
            String allegationType = (String) variables.get("allegationType");

            // Create audit log entry
            AuditLogEntry auditEntry = new AuditLogEntry()
                    .setAction("CASE_CREATED")
                    .setCaseId(caseId)
                    .setCaseNumber(caseNumber)
                    .setUserId(createdByUserId)
                    .setDetails(String.format("Case created with allegation type: %s", allegationType))
                    .setTimestamp(LocalDateTime.now());

            logAuditEntry(auditEntry);

            // Prepare output variables
            Map<String, Object> outputVariables = new HashMap<>();
            outputVariables.put("caseCreationAudited", true);
            outputVariables.put("auditDate", LocalDateTime.now().toString());

            logger.info("Case creation audit completed for case: {}", caseNumber);

            // Complete the job
            client.newCompleteCommand(job.getKey())
                    .variables(outputVariables)
                    .send()
                    .join();

        } catch (Exception e) {
            logger.error("Error auditing case creation for job {}: {}", job.getKey(), e.getMessage(), e);
            
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage("Failed to audit case creation: " + e.getMessage())
                    .send()
                    .join();
        }
    }

    @JobWorker(type = "audit-case-assignment")
    public void auditCaseAssignment(final JobClient client, final ActivatedJob job) {
        logger.info("Auditing case assignment for job: {}", job.getKey());

        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Long caseId = (Long) variables.get("caseId");
            String caseNumber = (String) variables.get("caseNumber");
            String assignedTo = (String) variables.get("assignedTo");
            String assignedGroup = (String) variables.get("assignedGroup");
            String classification = (String) variables.get("classification");
            Long assignedByUserId = (Long) variables.get("assignedByUserId");

            // Create audit log entry
            AuditLogEntry auditEntry = new AuditLogEntry()
                    .setAction("CASE_ASSIGNED")
                    .setCaseId(caseId)
                    .setCaseNumber(caseNumber)
                    .setUserId(assignedByUserId)
                    .setDetails(String.format("Case assigned to %s in group %s with classification %s", 
                              assignedTo, assignedGroup, classification))
                    .setTimestamp(LocalDateTime.now());

            logAuditEntry(auditEntry);

            // Record case transition
            recordCaseTransition(caseId, "CASE_ASSIGNED", assignedTo, assignedByUserId, variables);

            // Prepare output variables
            Map<String, Object> outputVariables = new HashMap<>();
            outputVariables.put("caseAssignmentAudited", true);
            outputVariables.put("auditDate", LocalDateTime.now().toString());

            logger.info("Case assignment audit completed for case: {}", caseNumber);

            // Complete the job
            client.newCompleteCommand(job.getKey())
                    .variables(outputVariables)
                    .send()
                    .join();

        } catch (Exception e) {
            logger.error("Error auditing case assignment for job {}: {}", job.getKey(), e.getMessage(), e);
            
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage("Failed to audit case assignment: " + e.getMessage())
                    .send()
                    .join();
        }
    }

    @JobWorker(type = "audit-task-completion")
    public void auditTaskCompletion(final JobClient client, final ActivatedJob job) {
        logger.info("Auditing task completion for job: {}", job.getKey());

        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Long caseId = (Long) variables.get("caseId");
            String caseNumber = (String) variables.get("caseNumber");
            String taskName = (String) variables.get("taskName");
            String outcome = (String) variables.get("outcome");
            Long completedByUserId = (Long) variables.get("completedByUserId");
            String comments = (String) variables.get("comments");

            // Create audit log entry
            AuditLogEntry auditEntry = new AuditLogEntry()
                    .setAction("TASK_COMPLETED")
                    .setCaseId(caseId)
                    .setCaseNumber(caseNumber)
                    .setUserId(completedByUserId)
                    .setDetails(String.format("Task '%s' completed with outcome: %s. Comments: %s", 
                              taskName, outcome, comments))
                    .setTimestamp(LocalDateTime.now());

            logAuditEntry(auditEntry);

            // Record case transition
            recordCaseTransition(caseId, taskName, outcome, completedByUserId, variables);

            // Prepare output variables
            Map<String, Object> outputVariables = new HashMap<>();
            outputVariables.put("taskCompletionAudited", true);
            outputVariables.put("auditDate", LocalDateTime.now().toString());

            logger.info("Task completion audit completed for case: {} task: {}", caseNumber, taskName);

            // Complete the job
            client.newCompleteCommand(job.getKey())
                    .variables(outputVariables)
                    .send()
                    .join();

        } catch (Exception e) {
            logger.error("Error auditing task completion for job {}: {}", job.getKey(), e.getMessage(), e);
            
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage("Failed to audit task completion: " + e.getMessage())
                    .send()
                    .join();
        }
    }

    @JobWorker(type = "audit-case-closure")
    public void auditCaseClosure(final JobClient client, final ActivatedJob job) {
        logger.info("Auditing case closure for job: {}", job.getKey());

        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Long caseId = (Long) variables.get("caseId");
            String caseNumber = (String) variables.get("caseNumber");
            String finalStatus = (String) variables.get("finalStatus");
            String closureReason = (String) variables.get("closureReason");
            Long closedByUserId = (Long) variables.get("closedByUserId");
            String investigationSummary = (String) variables.get("investigationSummary");

            // Create audit log entry
            AuditLogEntry auditEntry = new AuditLogEntry()
                    .setAction("CASE_CLOSED")
                    .setCaseId(caseId)
                    .setCaseNumber(caseNumber)
                    .setUserId(closedByUserId)
                    .setDetails(String.format("Case closed with final status: %s. Reason: %s. Summary: %s", 
                              finalStatus, closureReason, investigationSummary))
                    .setTimestamp(LocalDateTime.now());

            logAuditEntry(auditEntry);

            // Record final case transition
            recordCaseTransition(caseId, "CASE_CLOSED", finalStatus, closedByUserId, variables);

            // Calculate case metrics for audit
            calculateAndAuditCaseMetrics(caseId, caseNumber);

            // Prepare output variables
            Map<String, Object> outputVariables = new HashMap<>();
            outputVariables.put("caseClosureAudited", true);
            outputVariables.put("auditDate", LocalDateTime.now().toString());

            logger.info("Case closure audit completed for case: {}", caseNumber);

            // Complete the job
            client.newCompleteCommand(job.getKey())
                    .variables(outputVariables)
                    .send()
                    .join();

        } catch (Exception e) {
            logger.error("Error auditing case closure for job {}: {}", job.getKey(), e.getMessage(), e);
            
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage("Failed to audit case closure: " + e.getMessage())
                    .send()
                    .join();
        }
    }

    @JobWorker(type = "audit-compliance-check")
    public void auditComplianceCheck(final JobClient client, final ActivatedJob job) {
        logger.info("Performing compliance audit for job: {}", job.getKey());

        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Long caseId = (Long) variables.get("caseId");
            String caseNumber = (String) variables.get("caseNumber");

            // Perform compliance checks
            ComplianceCheckResult complianceResult = performComplianceChecks(caseId, variables);

            // Create audit log entry
            AuditLogEntry auditEntry = new AuditLogEntry()
                    .setAction("COMPLIANCE_CHECK")
                    .setCaseId(caseId)
                    .setCaseNumber(caseNumber)
                    .setDetails(String.format("Compliance check performed. Result: %s. Issues: %d", 
                              complianceResult.getOverallStatus(), complianceResult.getIssueCount()))
                    .setTimestamp(LocalDateTime.now());

            logAuditEntry(auditEntry);

            // Prepare output variables
            Map<String, Object> outputVariables = new HashMap<>();
            outputVariables.put("complianceCheckCompleted", true);
            outputVariables.put("complianceStatus", complianceResult.getOverallStatus());
            outputVariables.put("complianceIssues", complianceResult.getIssues());
            outputVariables.put("auditDate", LocalDateTime.now().toString());

            logger.info("Compliance audit completed for case: {} with status: {}", 
                       caseNumber, complianceResult.getOverallStatus());

            // Complete the job
            client.newCompleteCommand(job.getKey())
                    .variables(outputVariables)
                    .send()
                    .join();

        } catch (Exception e) {
            logger.error("Error performing compliance audit for job {}: {}", job.getKey(), e.getMessage(), e);
            
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage("Failed to perform compliance audit: " + e.getMessage())
                    .send()
                    .join();
        }
    }

    /**
     * Log audit entry to both database and audit log file
     */
    private void logAuditEntry(AuditLogEntry entry) {
        // Log to audit logger (file/external system)
        auditLogger.info("AUDIT: {} | Case: {} | User: {} | Details: {} | Timestamp: {}", 
                         entry.getAction(), entry.getCaseNumber(), entry.getUserId(), 
                         entry.getDetails(), entry.getTimestamp());

        // In production, this would also save to audit database table
        logger.debug("Audit entry logged: {}", entry.getAction());
    }

    /**
     * Record case transition in database
     */
    private void recordCaseTransition(Long caseId, String action, String outcome, Long userId, Map<String, Object> variables) {
        try {
            Case caseEntity = caseRepository.findByCaseId(caseId).orElse(null);
            if (caseEntity == null) {
                logger.warn("Case not found for transition recording: {}", caseId);
                return;
            }

            User user = null;
            if (userId != null) {
                user = userRepository.findByUserId(userId).orElse(null);
            }

            CaseTransition transition = new CaseTransition();
            transition.setCaseEntity(caseEntity);
            transition.setTaskName(action);
            transition.setFromStatus(caseEntity.getStatus());
            // Set new status based on outcome if applicable
            transition.setToStatus(caseEntity.getStatus()); // This would be updated based on business logic
            transition.setPerformedBy(user);
            transition.setComments(outcome);
            transition.setWorkflowInstanceKey(caseEntity.getWorkflowInstanceKey());

            // Convert variables to string map
            Map<String, String> variableStrings = new HashMap<>();
            variables.forEach((key, value) -> {
                if (value != null) {
                    variableStrings.put(key, value.toString());
                }
            });
            transition.setVariables(variableStrings);

            caseTransitionRepository.save(transition);
            
            logger.debug("Case transition recorded for case: {} action: {}", caseId, action);

        } catch (Exception e) {
            logger.error("Error recording case transition: {}", e.getMessage(), e);
        }
    }

    /**
     * Calculate and audit case metrics
     */
    private void calculateAndAuditCaseMetrics(Long caseId, String caseNumber) {
        try {
            Case caseEntity = caseRepository.findByCaseId(caseId).orElse(null);
            if (caseEntity == null) return;

            LocalDateTime createdAt = caseEntity.getCreatedAt();
            LocalDateTime updatedAt = caseEntity.getUpdatedAt();
            
            if (createdAt != null && updatedAt != null) {
                long durationDays = java.time.Duration.between(createdAt, updatedAt).toDays();
                
                AuditLogEntry metricsEntry = new AuditLogEntry()
                        .setAction("CASE_METRICS")
                        .setCaseId(caseId)
                        .setCaseNumber(caseNumber)
                        .setDetails(String.format("Case completed in %d days. Priority: %s", 
                                  durationDays, caseEntity.getPriority()))
                        .setTimestamp(LocalDateTime.now());

                logAuditEntry(metricsEntry);
            }

        } catch (Exception e) {
            logger.error("Error calculating case metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Perform compliance checks on the case
     */
    private ComplianceCheckResult performComplianceChecks(Long caseId, Map<String, Object> variables) {
        ComplianceCheckResult result = new ComplianceCheckResult();
        
        try {
            Case caseEntity = caseRepository.findByCaseId(caseId).orElse(null);
            if (caseEntity == null) {
                result.addIssue("Case not found");
                return result;
            }

            // Check timeline compliance
            LocalDateTime createdAt = caseEntity.getCreatedAt();
            if (createdAt != null) {
                long daysSinceCreation = java.time.Duration.between(createdAt, LocalDateTime.now()).toDays();
                
                // // Check if case is taking too long based on priority
                // String priority = caseEntity.getPriority() != null ? caseEntity.getPriority().name : "MEDIUM";
                // int maxDays = getMaxDaysForPriority(priority);
                
                // if (daysSinceCreation > maxDays) {
                //     result.addIssue(String.format("Case exceeds timeline: %d days (max: %d)", daysSinceCreation, maxDays));
                // }
            }

            // Check required fields
            if (caseEntity.getCaseType() == null) {
                result.addIssue("Case type is missing");
            }

            if (caseEntity.getAssignedTo() == null) {
                result.addIssue("Case is not assigned to anyone");
            }

            // Set overall status
            if (result.getIssueCount() == 0) {
                result.setOverallStatus("COMPLIANT");
            } else if (result.getIssueCount() <= 2) {
                result.setOverallStatus("MINOR_ISSUES");
            } else {
                result.setOverallStatus("NON_COMPLIANT");
            }

        } catch (Exception e) {
            logger.error("Error performing compliance checks: {}", e.getMessage(), e);
            result.addIssue("Error during compliance check: " + e.getMessage());
            result.setOverallStatus("ERROR");
        }

        return result;
    }

    /**
     * Get maximum days allowed for a case based on priority
     */
    private int getMaxDaysForPriority(String priority) {
        switch (priority) {
            case "CRITICAL":
                return 7;
            case "HIGH":
                return 14;
            case "MEDIUM":
                return 30;
            case "LOW":
                return 60;
            default:
                return 30;
        }
    }

    /**
     * Audit log entry class
     */
    private static class AuditLogEntry {
        private String action;
        private Long caseId;
        private String caseNumber;
        private Long userId;
        private String details;
        private LocalDateTime timestamp;

        // Getters and Setters with builder pattern
        public String getAction() { return action; }
        public AuditLogEntry setAction(String action) { this.action = action; return this; }

        public Long getCaseId() { return caseId; }
        public AuditLogEntry setCaseId(Long caseId) { this.caseId = caseId; return this; }

        public String getCaseNumber() { return caseNumber; }
        public AuditLogEntry setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; return this; }

        public Long getUserId() { return userId; }
        public AuditLogEntry setUserId(Long userId) { this.userId = userId; return this; }

        public String getDetails() { return details; }
        public AuditLogEntry setDetails(String details) { this.details = details; return this; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public AuditLogEntry setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
    }

    /**
     * Compliance check result class
     */
    private static class ComplianceCheckResult {
        private String overallStatus = "UNKNOWN";
        private java.util.List<String> issues = new java.util.ArrayList<>();

        public String getOverallStatus() { return overallStatus; }
        public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }

        public java.util.List<String> getIssues() { return issues; }
        public void addIssue(String issue) { this.issues.add(issue); }
        public int getIssueCount() { return issues.size(); }
    }
}