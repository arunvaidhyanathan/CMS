package com.citi.cms.workflow.delegates;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class NotificationDelegate {

    private static final Logger logger = LoggerFactory.getLogger(NotificationDelegate.class);

    @JobWorker(type = "send-case-assignment-notification")
    public void sendCaseAssignmentNotification(final JobClient client, final ActivatedJob job) {
        logger.info("Sending case assignment notification for job: {}", job.getKey());

        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Long caseId = (Long) variables.get("caseId");
            String caseNumber = (String) variables.get("caseNumber");
            String assignedTo = (String) variables.get("assignedTo");
            String assignedGroup = (String) variables.get("assignedGroup");
            String allegationType = (String) variables.get("allegationType");
            String priority = (String) variables.get("priority");

            logger.info("Sending assignment notification - Case: {}, Assigned to: {}, Group: {}", 
                       caseNumber, assignedTo, assignedGroup);

            // Simulate email notification
            String emailSubject = String.format("Case Assignment: %s - %s", caseNumber, allegationType);
            String emailBody = buildAssignmentEmailBody(caseNumber, allegationType, priority, assignedTo, assignedGroup);
            
            // In a real implementation, this would integrate with an email service
            sendEmailNotification(assignedTo, emailSubject, emailBody);

            // Prepare output variables
            Map<String, Object> outputVariables = new HashMap<>();
            outputVariables.put("assignmentNotificationSent", true);
            outputVariables.put("notificationDate", LocalDateTime.now().toString());
            outputVariables.put("notificationType", "ASSIGNMENT");

            logger.info("Case assignment notification sent successfully for case: {}", caseNumber);

            // Complete the job
            client.newCompleteCommand(job.getKey())
                    .variables(outputVariables)
                    .send()
                    .join();

        } catch (Exception e) {
            logger.error("Error sending case assignment notification for job {}: {}", job.getKey(), e.getMessage(), e);
            
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage("Failed to send assignment notification: " + e.getMessage())
                    .send()
                    .join();
        }
    }

    @JobWorker(type = "send-case-status-notification")
    public void sendCaseStatusNotification(final JobClient client, final ActivatedJob job) {
        logger.info("Sending case status notification for job: {}", job.getKey());

        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Long caseId = (Long) variables.get("caseId");
            String caseNumber = (String) variables.get("caseNumber");
            String oldStatus = (String) variables.get("oldStatus");
            String newStatus = (String) variables.get("newStatus");
            String complainantEmail = (String) variables.get("complainantEmail");
            String complainantName = (String) variables.get("complainantName");

            logger.info("Sending status notification - Case: {}, Status change: {} -> {}", 
                       caseNumber, oldStatus, newStatus);

            // Send notification to complainant if email is provided
            if (complainantEmail != null && !complainantEmail.trim().isEmpty()) {
                String emailSubject = String.format("Case Status Update: %s", caseNumber);
                String emailBody = buildStatusUpdateEmailBody(caseNumber, oldStatus, newStatus, complainantName);
                
                sendEmailNotification(complainantEmail, emailSubject, emailBody);
            }

            // Send internal notification to relevant stakeholders
            sendInternalStatusNotification(caseNumber, oldStatus, newStatus, variables);

            // Prepare output variables
            Map<String, Object> outputVariables = new HashMap<>();
            outputVariables.put("statusNotificationSent", true);
            outputVariables.put("notificationDate", LocalDateTime.now().toString());
            outputVariables.put("notificationType", "STATUS_UPDATE");

            logger.info("Case status notification sent successfully for case: {}", caseNumber);

            // Complete the job
            client.newCompleteCommand(job.getKey())
                    .variables(outputVariables)
                    .send()
                    .join();

        } catch (Exception e) {
            logger.error("Error sending case status notification for job {}: {}", job.getKey(), e.getMessage(), e);
            
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage("Failed to send status notification: " + e.getMessage())
                    .send()
                    .join();
        }
    }

    @JobWorker(type = "send-case-completion-notification")
    public void sendCaseCompletionNotification(final JobClient client, final ActivatedJob job) {
        logger.info("Sending case completion notification for job: {}", job.getKey());

        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Long caseId = (Long) variables.get("caseId");
            String caseNumber = (String) variables.get("caseNumber");
            String finalStatus = (String) variables.get("finalStatus");
            String complainantEmail = (String) variables.get("complainantEmail");
            String complainantName = (String) variables.get("complainantName");
            String investigationSummary = (String) variables.get("investigationSummary");

            logger.info("Sending completion notification - Case: {}, Final Status: {}", caseNumber, finalStatus);

            // Send notification to complainant
            if (complainantEmail != null && !complainantEmail.trim().isEmpty()) {
                String emailSubject = String.format("Case Completion: %s", caseNumber);
                String emailBody = buildCompletionEmailBody(caseNumber, finalStatus, complainantName, investigationSummary);
                
                sendEmailNotification(complainantEmail, emailSubject, emailBody);
            }

            // Send internal completion notification
            sendInternalCompletionNotification(caseNumber, finalStatus, variables);

            // Prepare output variables
            Map<String, Object> outputVariables = new HashMap<>();
            outputVariables.put("completionNotificationSent", true);
            outputVariables.put("notificationDate", LocalDateTime.now().toString());
            outputVariables.put("notificationType", "COMPLETION");

            logger.info("Case completion notification sent successfully for case: {}", caseNumber);

            // Complete the job
            client.newCompleteCommand(job.getKey())
                    .variables(outputVariables)
                    .send()
                    .join();

        } catch (Exception e) {
            logger.error("Error sending case completion notification for job {}: {}", job.getKey(), e.getMessage(), e);
            
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage("Failed to send completion notification: " + e.getMessage())
                    .send()
                    .join();
        }
    }

    @JobWorker(type = "send-urgent-notification")
    public void sendUrgentNotification(final JobClient client, final ActivatedJob job) {
        logger.info("Sending urgent notification for job: {}", job.getKey());

        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Long caseId = (Long) variables.get("caseId");
            String caseNumber = (String) variables.get("caseNumber");
            String urgencyReason = (String) variables.get("urgencyReason");
            String priority = (String) variables.get("priority");

            logger.info("Sending urgent notification - Case: {}, Reason: {}", caseNumber, urgencyReason);

            // Send urgent notification to management
            String emailSubject = String.format("URGENT: Case Alert - %s", caseNumber);
            String emailBody = buildUrgentNotificationBody(caseNumber, urgencyReason, priority);
            
            // Send to management and supervisors
            sendEmailNotification("management@company.com", emailSubject, emailBody);
            sendEmailNotification("ethics-director@company.com", emailSubject, emailBody);

            // Send SMS notification for critical cases
            if ("CRITICAL".equals(priority)) {
                sendSMSNotification("Emergency contact", "CRITICAL case alert: " + caseNumber);
            }

            // Prepare output variables
            Map<String, Object> outputVariables = new HashMap<>();
            outputVariables.put("urgentNotificationSent", true);
            outputVariables.put("notificationDate", LocalDateTime.now().toString());
            outputVariables.put("notificationType", "URGENT");

            logger.info("Urgent notification sent successfully for case: {}", caseNumber);

            // Complete the job
            client.newCompleteCommand(job.getKey())
                    .variables(outputVariables)
                    .send()
                    .join();

        } catch (Exception e) {
            logger.error("Error sending urgent notification for job {}: {}", job.getKey(), e.getMessage(), e);
            
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage("Failed to send urgent notification: " + e.getMessage())
                    .send()
                    .join();
        }
    }

    /**
     * Build email body for case assignment notification
     */
    private String buildAssignmentEmailBody(String caseNumber, String allegationType, String priority, 
                                          String assignedTo, String assignedGroup) {
        return String.format(
            "Dear %s,\n\n" +
            "A new case has been assigned to you:\n\n" +
            "Case Number: %s\n" +
            "Allegation Type: %s\n" +
            "Priority: %s\n" +
            "Assigned Group: %s\n\n" +
            "Please log into the Case Management System to review the case details and begin processing.\n\n" +
            "Best regards,\n" +
            "Ethics Office Case Management System",
            assignedTo, caseNumber, allegationType, priority, assignedGroup
        );
    }

    /**
     * Build email body for status update notification
     */
    private String buildStatusUpdateEmailBody(String caseNumber, String oldStatus, String newStatus, String complainantName) {
        return String.format(
            "Dear %s,\n\n" +
            "This is to inform you that the status of your case has been updated:\n\n" +
            "Case Number: %s\n" +
            "Previous Status: %s\n" +
            "Current Status: %s\n\n" +
            "If you have any questions, please contact the Ethics Office.\n\n" +
            "Best regards,\n" +
            "Ethics Office",
            complainantName != null ? complainantName : "Complainant",
            caseNumber, oldStatus, newStatus
        );
    }

    /**
     * Build email body for case completion notification
     */
    private String buildCompletionEmailBody(String caseNumber, String finalStatus, String complainantName, String summary) {
        return String.format(
            "Dear %s,\n\n" +
            "Your case has been completed:\n\n" +
            "Case Number: %s\n" +
            "Final Status: %s\n\n" +
            "%s\n\n" +
            "Thank you for your cooperation during the investigation process.\n\n" +
            "Best regards,\n" +
            "Ethics Office",
            complainantName != null ? complainantName : "Complainant",
            caseNumber, finalStatus,
            summary != null ? "Summary: " + summary : "Please contact the Ethics Office for more details."
        );
    }

    /**
     * Build email body for urgent notification
     */
    private String buildUrgentNotificationBody(String caseNumber, String urgencyReason, String priority) {
        return String.format(
            "URGENT CASE ALERT\n\n" +
            "Case Number: %s\n" +
            "Priority: %s\n" +
            "Urgency Reason: %s\n\n" +
            "This case requires immediate attention. Please review and take appropriate action.\n\n" +
            "Ethics Office Case Management System",
            caseNumber, priority, urgencyReason
        );
    }

    /**
     * Simulate sending email notification
     * In production, this would integrate with actual email service
     */
    private void sendEmailNotification(String recipient, String subject, String body) {
        logger.info("Sending email notification to: {} with subject: {}", recipient, subject);
        // TODO: Integrate with actual email service
        logger.debug("Email body: {}", body);
    }

    /**
     * Simulate sending SMS notification
     * In production, this would integrate with SMS service
     */
    private void sendSMSNotification(String recipient, String message) {
        logger.info("Sending SMS notification to: {} with message: {}", recipient, message);
        // TODO: Integrate with actual SMS service
    }

    /**
     * Send internal status notification to stakeholders
     */
    private void sendInternalStatusNotification(String caseNumber, String oldStatus, String newStatus, Map<String, Object> variables) {
        logger.info("Sending internal status notification for case: {}", caseNumber);
        
        // Notify relevant internal groups based on case assignment
        String assignedGroup = (String) variables.get("assignedGroup");
        if (assignedGroup != null) {
            String internalEmail = getGroupEmail(assignedGroup);
            if (internalEmail != null) {
                String subject = String.format("Internal Case Status Update: %s", caseNumber);
                String body = String.format(
                    "Case %s status has been updated from %s to %s.\n\n" +
                    "Please review the case in the Case Management System for details.",
                    caseNumber, oldStatus, newStatus
                );
                sendEmailNotification(internalEmail, subject, body);
            }
        }
    }

    /**
     * Send internal completion notification
     */
    private void sendInternalCompletionNotification(String caseNumber, String finalStatus, Map<String, Object> variables) {
        logger.info("Sending internal completion notification for case: {}", caseNumber);
        
        // Notify management and relevant departments
        String subject = String.format("Case Completed: %s", caseNumber);
        String body = String.format(
            "Case %s has been completed with final status: %s.\n\n" +
            "Please review the final report in the Case Management System.",
            caseNumber, finalStatus
        );
        
        sendEmailNotification("management@company.com", subject, body);
        sendEmailNotification("ethics-office@company.com", subject, body);
    }

    /**
     * Get email address for internal groups
     */
    private String getGroupEmail(String group) {
        switch (group) {
            case "HR_SPECIALIST":
                return "hr-specialists@company.com";
            case "LEGAL_COUNSEL":
                return "legal-team@company.com";
            case "SECURITY_SPECIALIST":
            case "CYBER_SECURITY_SPECIALIST":
            case "INVESTIGATION_SPECIALIST":
                return "security-team@company.com";
            case "COMPLIANCE_OFFICER":
                return "compliance@company.com";
            case "CONTRACT_SPECIALIST":
                return "contracts@company.com";
            default:
                return "ethics-office@company.com";
        }
    }
}