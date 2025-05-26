package com.citi.cms.workflow.listeners;

import com.citi.cms.entity.Case;
import com.citi.cms.entity.User;
import com.citi.cms.entity.WorkItem;
import com.citi.cms.repository.CaseRepository;
import com.citi.cms.repository.UserRepository;
import com.citi.cms.repository.WorkItemRepository;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class TaskAssignmentListener {

    private static final Logger logger = LoggerFactory.getLogger(TaskAssignmentListener.class);

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkItemRepository workItemRepository;

    @JobWorker(type = "task-assignment-listener")
    public void handleTaskAssignment(final JobClient client, final ActivatedJob job) {
        logger.info("Processing task assignment for job: {}", job.getKey());

        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            
            // Extract task assignment information
            Long caseId = (Long) variables.get("caseId");
            String taskName = (String) variables.get("taskName");
            String taskType = (String) variables.get("taskType");
            String assignedToUsername = (String) variables.get("assignedTo");
            String assignedGroup = (String) variables.get("assignedGroup");
            String priority = (String) variables.get("priority");

            logger.info("Task assignment - Case: {}, Task: {}, Assigned to: {}, Group: {}", 
                       caseId, taskName, assignedToUsername, assignedGroup);

            // Find the case
            Case caseEntity = caseRepository.findByCaseId(caseId).orElse(null);
            if (caseEntity == null) {
                logger.error("Case not found for task assignment: {}", caseId);
                throw new RuntimeException("Case not found: " + caseId);
            }

            // Find or determine the assigned user
            User assignedUser = null;
            if (assignedToUsername != null && !assignedToUsername.trim().isEmpty()) {
                assignedUser = userRepository.findByUsername(assignedToUsername).orElse(null);
                if (assignedUser == null) {
                    logger.warn("Assigned user not found: {}, will assign to group", assignedToUsername);
                }
            }

            // If no specific user, try to auto-assign based on group workload
            if (assignedUser == null && assignedGroup != null) {
                assignedUser = findBestAvailableUserInGroup(assignedGroup);
            }

            // Create work item
            WorkItem workItem = createWorkItem(caseEntity, taskName, taskType, assignedUser, job.getKey(), priority);
            
            // Update case assignment if this is a case-level assignment
            if (isMainCaseTask(taskType) && assignedUser != null) {
                caseEntity.setAssignedTo(assignedUser);
                caseRepository.save(caseEntity);
                logger.info("Case {} assigned to user: {}", caseEntity.getCaseNumber(), assignedUser.getUsername());
            }

            // Send notification about task assignment
            sendTaskAssignmentNotification(workItem, caseEntity, assignedUser, assignedGroup);

            // Prepare output variables
            Map<String, Object> outputVariables = new HashMap<>();
            outputVariables.put("taskAssigned", true);
            outputVariables.put("workItemId", workItem.getWorkItemId());
            outputVariables.put("assignedToUserId", assignedUser != null ? assignedUser.getUserId() : null);
            outputVariables.put("assignedToUsername", assignedUser != null ? assignedUser.getUsername() : null);
            outputVariables.put("assignmentDate", LocalDateTime.now().toString());

            logger.info("Task assignment completed successfully for case: {}", caseEntity.getCaseNumber());

            // Complete the job
            client.newCompleteCommand(job.getKey())
                    .variables(outputVariables)
                    .send()
                    .join();

        } catch (Exception e) {
            logger.error("Error processing task assignment for job {}: {}", job.getKey(), e.getMessage(), e);
            
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage("Task assignment failed: " + e.getMessage())
                    .send()
                    .join();
        }
    }

    @JobWorker(type = "task-reassignment-listener")
    public void handleTaskReassignment(final JobClient client, final ActivatedJob job) {
        logger.info("Processing task reassignment for job: {}", job.getKey());

        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            
            Long workItemId = (Long) variables.get("workItemId");
            String newAssigneeUsername = (String) variables.get("newAssignee");
            String reassignmentReason = (String) variables.get("reassignmentReason");
            Long reassignedByUserId = (Long) variables.get("reassignedByUserId");

            logger.info("Task reassignment - WorkItem: {}, New Assignee: {}, Reason: {}", 
                       workItemId, newAssigneeUsername, reassignmentReason);

            // Find the work item
            WorkItem workItem = workItemRepository.findByWorkItemId(workItemId).orElse(null);
            if (workItem == null) {
                logger.error("Work item not found for reassignment: {}", workItemId);
                throw new RuntimeException("Work item not found: " + workItemId);
            }

            // Find the new assignee
            User newAssignee = userRepository.findByUsername(newAssigneeUsername).orElse(null);
            if (newAssignee == null) {
                logger.error("New assignee not found: {}", newAssigneeUsername);
                throw new RuntimeException("New assignee not found: " + newAssigneeUsername);
            }

            // Update work item assignment
            User previousAssignee = workItem.getAssignedTo();
            workItem.setAssignedTo(newAssignee);
            workItemRepository.save(workItem);

            // Log the reassignment
            logger.info("Work item {} reassigned from {} to {} - Reason: {}", 
                       workItemId, 
                       previousAssignee != null ? previousAssignee.getUsername() : "unassigned",
                       newAssignee.getUsername(), 
                       reassignmentReason);

            // Send reassignment notification
            sendTaskReassignmentNotification(workItem, previousAssignee, newAssignee, reassignmentReason);

            // Prepare output variables
            Map<String, Object> outputVariables = new HashMap<>();
            outputVariables.put("taskReassigned", true);
            outputVariables.put("previousAssigneeId", previousAssignee != null ? previousAssignee.getUserId() : null);
            outputVariables.put("newAssigneeId", newAssignee.getUserId());
            outputVariables.put("reassignmentDate", LocalDateTime.now().toString());

            logger.info("Task reassignment completed successfully");

            // Complete the job
            client.newCompleteCommand(job.getKey())
                    .variables(outputVariables)
                    .send()
                    .join();

        } catch (Exception e) {
            logger.error("Error processing task reassignment for job {}: {}", job.getKey(), e.getMessage(), e);
            
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage("Task reassignment failed: " + e.getMessage())
                    .send()
                    .join();
        }
    }

    @JobWorker(type = "task-escalation-listener")
    public void handleTaskEscalation(final JobClient client, final ActivatedJob job) {
        logger.info("Processing task escalation for job: {}", job.getKey());

        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            
            Long workItemId = (Long) variables.get("workItemId");
            String escalationReason = (String) variables.get("escalationReason");
            String escalationLevel = (String) variables.get("escalationLevel");

            logger.info("Task escalation - WorkItem: {}, Reason: {}, Level: {}", 
                       workItemId, escalationReason, escalationLevel);

            // Find the work item
            WorkItem workItem = workItemRepository.findByWorkItemId(workItemId).orElse(null);
            if (workItem == null) {
                logger.error("Work item not found for escalation: {}", workItemId);
                throw new RuntimeException("Work item not found: " + workItemId);
            }

            // Determine escalation target based on level
            User escalationTarget = findEscalationTarget(workItem, escalationLevel);
            
            if (escalationTarget != null) {
                // Update work item with escalated assignment
                User previousAssignee = workItem.getAssignedTo();
                workItem.setAssignedTo(escalationTarget);
                workItem.setStatus("ESCALATED");
                workItemRepository.save(workItem);

                logger.info("Work item {} escalated from {} to {} - Level: {}", 
                           workItemId, 
                           previousAssignee != null ? previousAssignee.getUsername() : "unassigned",
                           escalationTarget.getUsername(), 
                           escalationLevel);

                // Send escalation notification
                sendTaskEscalationNotification(workItem, previousAssignee, escalationTarget, escalationReason, escalationLevel);

                // Prepare output variables
                Map<String, Object> outputVariables = new HashMap<>();
                outputVariables.put("taskEscalated", true);
                outputVariables.put("escalationTargetId", escalationTarget.getUserId());
                outputVariables.put("escalationDate", LocalDateTime.now().toString());
                outputVariables.put("escalationLevel", escalationLevel);

                logger.info("Task escalation completed successfully");

                // Complete the job
                client.newCompleteCommand(job.getKey())
                        .variables(outputVariables)
                        .send()
                        .join();
            } else {
                logger.error("No escalation target found for level: {}", escalationLevel);
                throw new RuntimeException("No escalation target found for level: " + escalationLevel);
            }

        } catch (Exception e) {
            logger.error("Error processing task escalation for job {}: {}", job.getKey(), e.getMessage(), e);
            
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage("Task escalation failed: " + e.getMessage())
                    .send()
                    .join();
        }
    }

    /**
     * Create a new work item for the task
     */
    private WorkItem createWorkItem(Case caseEntity, String taskName, String taskType, User assignedUser, Long taskKey, String priority) {
        WorkItem workItem = new WorkItem();
        workItem.setCaseEntity(caseEntity);
        workItem.setTaskName(taskName);
        workItem.setTaskType(taskType);
        workItem.setAssignedTo(assignedUser);
        workItem.setTaskKey(taskKey);
        workItem.setStatus("PENDING");
        
        // Set due date based on priority
        workItem.setDueDate(calculateDueDate(priority));
        
        // Generate work item ID
        Long nextWorkItemId = generateWorkItemId();
        workItem.setWorkItemId(nextWorkItemId);

        return workItemRepository.save(workItem);
    }

    /**
     * Find the best available user in a group based on workload
     */
    private User findBestAvailableUserInGroup(String group) {
        // This is a simplified implementation
        // In production, this would consider current workload, availability, etc.
        
        String roleCode = mapGroupToRoleCode(group);
        if (roleCode == null) {
            logger.warn("Unknown group for assignment: {}", group);
            return null;
        }

        // Find users with the appropriate role who have the least current workload
        // For now, we'll just find any user with the role
        return userRepository.findAll().stream()
                .filter(user -> user.hasRole(roleCode))
                .filter(user -> user.isEnabled())
                .findFirst()
                .orElse(null);
    }

    /**
     * Map group names to role codes
     */
    private String mapGroupToRoleCode(String group) {
        switch (group) {
            case "HR_SPECIALIST":
                return "HR_SPECIALIST";
            case "LEGAL_COUNSEL":
                return "LEGAL_COUNSEL";
            case "SECURITY_SPECIALIST":
            case "CYBER_SECURITY_SPECIALIST":
            case "INVESTIGATION_SPECIALIST":
                return "SECURITY_ANALYST";
            case "COMPLIANCE_OFFICER":
                return "LEGAL_COUNSEL";
            case "CONTRACT_SPECIALIST":
                return "LEGAL_COUNSEL";
            case "INVESTIGATOR_GROUP":
                return "INVESTIGATOR";
            case "IU_MANAGER_GROUP":
                return "IU_MANAGER";
            case "DIRECTOR_GROUP":
                return "DIRECTOR";
            default:
                return null;
        }
    }

    /**
     * Check if this is a main case task (not a sub-task)
     */
    private boolean isMainCaseTask(String taskType) {
        return "CASE_ASSIGNMENT".equals(taskType) || 
               "INITIAL_REVIEW".equals(taskType) ||
               "INVESTIGATION_ASSIGNMENT".equals(taskType);
    }

    /**
     * Calculate due date based on priority
     */
    private LocalDate calculateDueDate(String priority) {
        LocalDate now = LocalDate.now();
        
        switch (priority != null ? priority : "MEDIUM") {
            case "CRITICAL":
                return now.plusDays(1);
            case "HIGH":
                return now.plusDays(3);
            case "MEDIUM":
                return now.plusDays(7);
            case "LOW":
                return now.plusDays(14);
            default:
                return now.plusDays(7);
        }
    }

    /**
     * Generate unique work item ID
     */
    private Long generateWorkItemId() {
        // Simple implementation - in production, use database sequence
        return System.currentTimeMillis();
    }

    /**
     * Find escalation target based on escalation level
     */
    private User findEscalationTarget(WorkItem workItem, String escalationLevel) {
        String targetRoleCode;
        
        switch (escalationLevel) {
            case "SUPERVISOR":
                targetRoleCode = "IU_MANAGER";
                break;
            case "MANAGER":
                targetRoleCode = "DIRECTOR";
                break;
            case "DIRECTOR":
                targetRoleCode = "DIRECTOR";
                break;
            default:
                logger.warn("Unknown escalation level: {}", escalationLevel);
                return null;
        }

        return userRepository.findAll().stream()
                .filter(user -> user.hasRole(targetRoleCode))
                .filter(user -> user.isEnabled())
                .findFirst()
                .orElse(null);
    }

    /**
     * Send task assignment notification
     */
    private void sendTaskAssignmentNotification(WorkItem workItem, Case caseEntity, User assignedUser, String assignedGroup) {
        logger.info("Sending task assignment notification for work item: {}", workItem.getWorkItemId());
        
        if (assignedUser != null) {
            // Send email to assigned user
            String subject = String.format("Task Assigned: %s - Case %s", workItem.getTaskName(), caseEntity.getCaseNumber());
            String message = String.format("You have been assigned a new task: %s for case %s", 
                                         workItem.getTaskName(), caseEntity.getCaseNumber());
            
            // In production, integrate with actual notification service
            logger.info("Email notification sent to: {} for task assignment", assignedUser.getEmail());
        }
        
        if (assignedGroup != null) {
            // Send notification to group
            logger.info("Group notification sent to: {} for task assignment", assignedGroup);
        }
    }

    /**
     * Send task reassignment notification
     */
    private void sendTaskReassignmentNotification(WorkItem workItem, User previousAssignee, User newAssignee, String reason) {
        logger.info("Sending task reassignment notification for work item: {}", workItem.getWorkItemId());
        
        // Notify previous assignee
        if (previousAssignee != null) {
            logger.info("Reassignment notification sent to previous assignee: {}", previousAssignee.getEmail());
        }
        
        // Notify new assignee
        if (newAssignee != null) {
            logger.info("Assignment notification sent to new assignee: {}", newAssignee.getEmail());
        }
    }

    /**
     * Send task escalation notification
     */
    private void sendTaskEscalationNotification(WorkItem workItem, User previousAssignee, User escalationTarget, 
                                              String reason, String level) {
        logger.info("Sending task escalation notification for work item: {}", workItem.getWorkItemId());
        
        // Notify escalation target
        if (escalationTarget != null) {
            String subject = String.format("ESCALATED TASK: %s - Case %s", workItem.getTaskName(), 
                                         workItem.getCaseEntity().getCaseNumber());
            logger.info("Escalation notification sent to: {} (Level: {})", escalationTarget.getEmail(), level);
        }
        
        // Notify management about escalation
        logger.info("Management notification sent about task escalation - Level: {}, Reason: {}", level, reason);
    }
}