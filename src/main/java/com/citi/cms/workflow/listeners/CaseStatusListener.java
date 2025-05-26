package com.citi.cms.workflow.listeners;

import com.citi.cms.entity.Case;
import com.citi.cms.entity.CaseStatus;
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
public class CaseStatusListener {

    private static final Logger logger = LoggerFactory.getLogger(CaseStatusListener.class);

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private CaseTransitionRepository caseTransitionRepository;

    @Autowired
    private UserRepository userRepository;

    @JobWorker(type = "case-status-change-listener")
    public void handleCaseStatusChange(final JobClient client, final ActivatedJob job) {
        logger.info("Processing case status change for job: {}", job.getKey());

        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            
            Long caseId = (Long) variables.get("caseId");
            String newStatus = (String) variables.get("newStatus");
            String oldStatus = (String) variables.get("oldStatus");
            String changeReason = (String) variables.get("changeReason");
            Long changedByUserId = (Long) variables.get("changedByUserId");
            String taskName = (String) variables.get("taskName");

            logger.info("Case status change - Case: {}, {} -> {}, Reason: {}", 
                       caseId, oldStatus, newStatus, changeReason);

            // Find the case
            Case caseEntity = caseRepository.findByCaseId(caseId).orElse(null);
            if (caseEntity == null) {
                logger.error("Case not found for status change: {}", caseId);
                throw new RuntimeException("Case not found: " + caseId);
            }

            // Validate status transition
            if (!isValidStatusTransition(oldStatus, newStatus)) {
                logger.error("Invalid status transition: {} -> {}", oldStatus, newStatus);
                throw new RuntimeException("Invalid status transition: " + oldStatus + " -> " + newStatus);
            }

            // Update case status
            CaseStatus previousStatus = caseEntity.getStatus();
            CaseStatus nextStatus = CaseStatus.valueOf(newStatus);
            caseEntity.setStatus(nextStatus);
            caseEntity.setUpdatedAt(LocalDateTime.now());
            
            caseRepository.save(caseEntity);

            // Record the status transition
            recordStatusTransition(caseEntity, previousStatus, nextStatus, changeReason, changedByUserId, taskName, variables);

            // Handle status-specific actions
            handleStatusSpecificActions(caseEntity, previousStatus, nextStatus, variables);

            // Trigger notifications
            triggerStatusChangeNotifications(caseEntity, previousStatus, nextStatus, changeReason);

            // Prepare output variables
            Map<String, Object> outputVariables = new HashMap<>();
            outputVariables.put("statusChangeProcessed", true);
            outputVariables.put("statusChangeDate", LocalDateTime.now().toString());
            outputVariables.put("previousStatus", previousStatus.name());
            outputVariables.put("currentStatus", nextStatus.name());

            logger.info("Case status change processed successfully for case: {}", caseEntity.getCaseNumber());

            // Complete the job
            client.newCompleteCommand(job.getKey())
                    .variables(outputVariables)
                    .send()
                    .join();

        } catch (Exception e) {
            logger.error("Error processing case status change for job {}: {}", job.getKey(), e.getMessage(), e);
            
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage("Case status change failed: " + e.getMessage())
                    .send()
                    .join();
        }
    }

    @JobWorker(type = "case-milestone-listener")
    public void handleCaseMilestone(final JobClient client, final ActivatedJob job) {
        logger.info("Processing case milestone for job: {}", job.getKey());

        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            
            Long caseId = (Long) variables.get("caseId");
            String milestone = (String) variables.get("milestone");
            String milestoneDetails = (String) variables.get("milestoneDetails");
            LocalDateTime milestoneDate = LocalDateTime.now();

            logger.info("Case milestone reached - Case: {}, Milestone: {}", caseId, milestone);

            // Find the case
            Case caseEntity = caseRepository.findByCaseId(caseId).orElse(null);
            if (caseEntity == null) {
                logger.error("Case not found for milestone: {}", caseId);
                throw new RuntimeException("Case not found: " + caseId);
            }

            // Process milestone-specific logic
            processMilestone(caseEntity, milestone, milestoneDetails, milestoneDate);

            // Trigger milestone notifications
            triggerMilestoneNotifications(caseEntity, milestone, milestoneDetails);

            // Check for SLA compliance
            checkSLACompliance(caseEntity, milestone);

            // Prepare output variables
            Map<String, Object> outputVariables = new HashMap<>();
            outputVariables.put("milestoneProcessed", true);
            outputVariables.put("milestoneDate", milestoneDate.toString());
            outputVariables.put("milestone", milestone);

            logger.info("Case milestone processed successfully for case: {}", caseEntity.getCaseNumber());

            // Complete the job
            client.newCompleteCommand(job.getKey())
                    .variables(outputVariables)
                    .send()
                    .join();

        } catch (Exception e) {
            logger.error("Error processing case milestone for job {}: {}", job.getKey(), e.getMessage(), e);
            
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage("Case milestone processing failed: " + e.getMessage())
                    .send()
                    .join();
        }
    }

    @JobWorker(type = "case-deadline-listener")
    public void handleCaseDeadline(final JobClient client, final ActivatedJob job) {
        logger.info("Processing case deadline for job: {}", job.getKey());

        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            
            Long caseId = (Long) variables.get("caseId");
            String deadlineType = (String) variables.get("deadlineType");
            Boolean isOverdue = (Boolean) variables.get("isOverdue");

            logger.info("Case deadline event - Case: {}, Type: {}, Overdue: {}", caseId, deadlineType, isOverdue);

            // Find the case
            Case caseEntity = caseRepository.findByCaseId(caseId).orElse(null);
            if (caseEntity == null) {
                logger.error("Case not found for deadline: {}", caseId);
                throw new RuntimeException("Case not found: " + caseId);
            }

            // Handle deadline-specific actions
            if (Boolean.TRUE.equals(isOverdue)) {
                handleOverdueCase(caseEntity, deadlineType);
            } else {
                handleUpcomingDeadline(caseEntity, deadlineType);
            }

            // Prepare output variables
            Map<String, Object> outputVariables = new HashMap<>();
            outputVariables.put("deadlineProcessed", true);
            outputVariables.put("deadlineDate", LocalDateTime.now().toString());
            outputVariables.put("deadlineType", deadlineType);
            outputVariables.put("isOverdue", isOverdue);

            logger.info("Case deadline processed successfully for case: {}", caseEntity.getCaseNumber());

            // Complete the job
            client.newCompleteCommand(job.getKey())
                    .variables(outputVariables)
                    .send()
                    .join();

        } catch (Exception e) {
            logger.error("Error processing case deadline for job {}: {}", job.getKey(), e.getMessage(), e);
            
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage("Case deadline processing failed: " + e.getMessage())
                    .send()
                    .join();
        }
    }

    /**
     * Validate if the status transition is valid
     */
    private boolean isValidStatusTransition(String fromStatus, String toStatus) {
        if (fromStatus == null || toStatus == null) return false;
        
        // Define valid transitions
        Map<String, String[]> validTransitions = new HashMap<>();
        validTransitions.put("OPEN", new String[]{"IN_PROGRESS", "CLOSED"});
        validTransitions.put("IN_PROGRESS", new String[]{"RESOLVED", "CLOSED", "OPEN"});
        validTransitions.put("RESOLVED", new String[]{"CLOSED", "IN_PROGRESS"});
        validTransitions.put("CLOSED", new String[]{}); // No transitions from closed
        
        String[] allowedTransitions = validTransitions.get(fromStatus);
        if (allowedTransitions == null) return false;
        
        for (String allowed : allowedTransitions) {
            if (allowed.equals(toStatus)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Record the status transition in the database
     */
    private void recordStatusTransition(Case caseEntity, CaseStatus fromStatus, CaseStatus toStatus, 
                                      String reason, Long changedByUserId, String taskName, Map<String, Object> variables) {
        try {
            User changedByUser = null;
            if (changedByUserId != null) {
                changedByUser = userRepository.findByUserId(changedByUserId).orElse(null);
            }

            CaseTransition transition = new CaseTransition();
            transition.setCaseEntity(caseEntity);
            transition.setFromStatus(fromStatus);
            transition.setToStatus(toStatus);
            transition.setTaskName(taskName != null ? taskName : "STATUS_CHANGE");
            transition.setPerformedBy(changedByUser);
            transition.setComments(reason);
            transition.setWorkflowInstanceKey(caseEntity.getWorkflowInstanceKey());
            transition.setTransitionDate(LocalDateTime.now());

            // Convert variables to string map
            Map<String, String> variableStrings = new HashMap<>();
            variables.forEach((key, value) -> {
                if (value != null) {
                    variableStrings.put(key, value.toString());
                }
            });
            transition.setVariables(variableStrings);

            caseTransitionRepository.save(transition);

            logger.debug("Status transition recorded: {} -> {} for case: {}", 
                        fromStatus, toStatus, caseEntity.getCaseNumber());

        } catch (Exception e) {
            logger.error("Error recording status transition: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle actions specific to certain status changes
     */
    private void handleStatusSpecificActions(Case caseEntity, CaseStatus fromStatus, CaseStatus toStatus, Map<String, Object> variables) {
        switch (toStatus) {
            case IN_PROGRESS:
                handleInProgressActions(caseEntity, variables);
                break;
            case RESOLVED:
                handleResolvedActions(caseEntity, variables);
                break;
            case CLOSED:
                handleClosedActions(caseEntity, variables);
                break;
            default:
                logger.debug("No specific actions required for status: {}", toStatus);
        }
    }

    /**
     * Handle actions when case moves to IN_PROGRESS
     */
    private void handleInProgressActions(Case caseEntity, Map<String, Object> variables) {
        logger.info("Processing IN_PROGRESS actions for case: {}", caseEntity.getCaseNumber());
        
        // Start SLA timer if not already started
        // Update case metrics
        // Trigger progress tracking
    }

    /**
     * Handle actions when case moves to RESOLVED
     */
    private void handleResolvedActions(Case caseEntity, Map<String, Object> variables) {
        logger.info("Processing RESOLVED actions for case: {}", caseEntity.getCaseNumber());
        
        // Calculate resolution time
        if (caseEntity.getCreatedAt() != null) {
            long resolutionDays = java.time.Duration.between(caseEntity.getCreatedAt(), LocalDateTime.now()).toDays();
            logger.info("Case {} resolved in {} days", caseEntity.getCaseNumber(), resolutionDays);
        }
        
        // Prepare final report
        // Schedule follow-up activities
    }

    /**
     * Handle actions when case moves to CLOSED
     */
    private void handleClosedActions(Case caseEntity, Map<String, Object> variables) {
        logger.info("Processing CLOSED actions for case: {}", caseEntity.getCaseNumber());
        
        // Finalize case metrics
        // Archive case documents
        // Send final notifications
        // Update compliance tracking
    }

    /**
     * Trigger status change notifications
     */
    private void triggerStatusChangeNotifications(Case caseEntity, CaseStatus fromStatus, CaseStatus toStatus, String reason) {
        logger.info("Triggering status change notifications for case: {} ({} -> {})", 
                   caseEntity.getCaseNumber(), fromStatus, toStatus);
        
        // Notify complainant
        if (caseEntity.getComplainantEmail() != null && !caseEntity.getComplainantEmail().trim().isEmpty()) {
            sendComplainantStatusNotification(caseEntity, fromStatus, toStatus);
        }
        
        // Notify assigned user
        if (caseEntity.getAssignedTo() != null) {
            sendAssigneeStatusNotification(caseEntity, fromStatus, toStatus);
        }
        
        // Notify management for certain status changes
        if (shouldNotifyManagement(toStatus)) {
            sendManagementStatusNotification(caseEntity, fromStatus, toStatus, reason);
        }
    }

    /**
     * Process milestone-specific logic
     */
    private void processMilestone(Case caseEntity, String milestone, String details, LocalDateTime milestoneDate) {
        logger.info("Processing milestone '{}' for case: {}", milestone, caseEntity.getCaseNumber());
        
        switch (milestone) {
            case "CASE_ASSIGNED":
                logger.info("Case assignment milestone reached");
                break;
            case "INVESTIGATION_STARTED":
                logger.info("Investigation start milestone reached");
                break;
            case "EVIDENCE_COLLECTED":
                logger.info("Evidence collection milestone reached");
                break;
            case "INVESTIGATION_COMPLETED":
                logger.info("Investigation completion milestone reached");
                break;
            case "CASE_REVIEWED":
                logger.info("Case review milestone reached");
                break;
            default:
                logger.debug("Processing custom milestone: {}", milestone);
        }
    }

    /**
     * Trigger milestone notifications
     */
    private void triggerMilestoneNotifications(Case caseEntity, String milestone, String details) {
        logger.info("Triggering milestone notifications for case: {} milestone: {}", 
                   caseEntity.getCaseNumber(), milestone);
        
        // Send internal milestone notification
        // Update progress tracking
        // Notify stakeholders if required
    }

    /**
     * Check SLA compliance for the case
     */
    private void checkSLACompliance(Case caseEntity, String milestone) {
        if (caseEntity.getCaseType() != null && caseEntity.getCaseType().getSlaHours() != null) {
            int slaHours = caseEntity.getCaseType().getSlaHours();
            long hoursElapsed = java.time.Duration.between(caseEntity.getCreatedAt(), LocalDateTime.now()).toHours();
            
            if (hoursElapsed > slaHours) {
                logger.warn("SLA breach detected for case: {} - {} hours elapsed (SLA: {} hours)", 
                           caseEntity.getCaseNumber(), hoursElapsed, slaHours);
                
                // Trigger SLA breach handling
                handleSLABreach(caseEntity, hoursElapsed, slaHours);
            } else {
                logger.debug("Case {} within SLA - {} hours elapsed (SLA: {} hours)", 
                           caseEntity.getCaseNumber(), hoursElapsed, slaHours);
            }
        }
    }

    /**
     * Handle overdue cases
     */
    private void handleOverdueCase(Case caseEntity, String deadlineType) {
        logger.warn("Handling overdue case: {} - Deadline type: {}", caseEntity.getCaseNumber(), deadlineType);
        
        // Send escalation notifications
        // Update case priority if needed
        // Trigger management alerts
        sendOverdueNotification(caseEntity, deadlineType);
    }

    /**
     * Handle upcoming deadlines
     */
    private void handleUpcomingDeadline(Case caseEntity, String deadlineType) {
        logger.info("Handling upcoming deadline for case: {} - Deadline type: {}", 
                   caseEntity.getCaseNumber(), deadlineType);
        
        // Send reminder notifications
        sendDeadlineReminderNotification(caseEntity, deadlineType);
    }

    /**
     * Handle SLA breach
     */
    private void handleSLABreach(Case caseEntity, long hoursElapsed, int slaHours) {
        logger.error("SLA BREACH: Case {} exceeded SLA by {} hours", 
                    caseEntity.getCaseNumber(), hoursElapsed - slaHours);
        
        // Send urgent notifications to management
        // Escalate case if needed
        // Update compliance metrics
    }

    /**
     * Check if management should be notified for this status change
     */
    private boolean shouldNotifyManagement(CaseStatus status) {
        return status == CaseStatus.RESOLVED || status == CaseStatus.CLOSED;
    }

    /**
     * Send status notification to complainant
     */
    private void sendComplainantStatusNotification(Case caseEntity, CaseStatus fromStatus, CaseStatus toStatus) {
        logger.info("Sending complainant status notification for case: {}", caseEntity.getCaseNumber());
        
        String subject = String.format("Case Status Update: %s", caseEntity.getCaseNumber());
        String message = String.format("Your case status has been updated from %s to %s.", fromStatus, toStatus);
        
        // In production, integrate with actual email service
        logger.info("Status notification sent to complainant: {}", caseEntity.getComplainantEmail());
    }

    /**
     * Send status notification to assigned user
     */
    private void sendAssigneeStatusNotification(Case caseEntity, CaseStatus fromStatus, CaseStatus toStatus) {
        logger.info("Sending assignee status notification for case: {}", caseEntity.getCaseNumber());
        
        String subject = String.format("Case Status Changed: %s", caseEntity.getCaseNumber());
        String message = String.format("Case %s status changed from %s to %s.", 
                                      caseEntity.getCaseNumber(), fromStatus, toStatus);
        
        // In production, integrate with actual email service
        logger.info("Status notification sent to assignee: {}", caseEntity.getAssignedTo().getEmail());
    }

    /**
     * Send status notification to management
     */
    private void sendManagementStatusNotification(Case caseEntity, CaseStatus fromStatus, CaseStatus toStatus, String reason) {
        logger.info("Sending management status notification for case: {}", caseEntity.getCaseNumber());
        
        String subject = String.format("Case Status Update - Management Alert: %s", caseEntity.getCaseNumber());
        String message = String.format("Case %s status changed from %s to %s. Reason: %s", 
                                      caseEntity.getCaseNumber(), fromStatus, toStatus, reason);
        
        // In production, integrate with actual email service
        logger.info("Management status notification sent for case: {}", caseEntity.getCaseNumber());
    }

    /**
     * Send overdue case notification
     */
    private void sendOverdueNotification(Case caseEntity, String deadlineType) {
        logger.error("Sending overdue notification for case: {} - Deadline: {}", 
                    caseEntity.getCaseNumber(), deadlineType);
        
        String subject = String.format("OVERDUE CASE ALERT: %s", caseEntity.getCaseNumber());
        String message = String.format("Case %s is overdue for %s. Immediate attention required.", 
                                      caseEntity.getCaseNumber(), deadlineType);
        
        // Send to management and assigned user
        logger.info("Overdue notification sent for case: {}", caseEntity.getCaseNumber());
    }

    /**
     * Send deadline reminder notification
     */
    private void sendDeadlineReminderNotification(Case caseEntity, String deadlineType) {
        logger.info("Sending deadline reminder for case: {} - Deadline: {}", 
                   caseEntity.getCaseNumber(), deadlineType);
        
        String subject = String.format("Deadline Reminder: %s", caseEntity.getCaseNumber());
        String message = String.format("Case %s has an upcoming %s deadline. Please take necessary action.", 
                                      caseEntity.getCaseNumber(), deadlineType);
        
        // Send to assigned user
        if (caseEntity.getAssignedTo() != null) {
            logger.info("Deadline reminder sent to: {}", caseEntity.getAssignedTo().getEmail());
        }
    }
}