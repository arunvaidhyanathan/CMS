package com.citi.cms.workflow.delegates;

import com.citi.cms.entity.Case;
import com.citi.cms.entity.CaseStatus;
import com.citi.cms.repository.CaseRepository;
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
public class CaseCreationDelegate {

    private static final Logger logger = LoggerFactory.getLogger(CaseCreationDelegate.class);

    @Autowired
    private CaseRepository caseRepository;

    @JobWorker(type = "initialize-case")
    public void initializeCase(final JobClient client, final ActivatedJob job) {
        logger.info("Initializing case for job: {}", job.getKey());

        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Long caseId = (Long) variables.get("caseId");
            String caseNumber = (String) variables.get("caseNumber");
            
            logger.info("Processing case initialization - Case ID: {}, Case Number: {}", caseId, caseNumber);

            // Update case status and workflow information
            Case caseEntity = caseRepository.findByCaseId(caseId).orElse(null);
            if (caseEntity != null) {
                caseEntity.setStatus(CaseStatus.IN_PROGRESS);
                caseEntity.setWorkflowInstanceKey(job.getProcessInstanceKey());
                caseEntity.setUpdatedAt(LocalDateTime.now());
                
                caseRepository.save(caseEntity);
                
                logger.info("Case initialized successfully: {}", caseNumber);
            } else {
                logger.warn("Case not found for initialization: {}", caseId);
            }

            // Prepare variables for next step
            Map<String, Object> outputVariables = new HashMap<>();
            outputVariables.put("caseInitialized", true);
            outputVariables.put("initializationDate", LocalDateTime.now().toString());
            outputVariables.put("caseId", caseId);
            outputVariables.put("caseNumber", caseNumber);

            // Complete the job
            client.newCompleteCommand(job.getKey())
                    .variables(outputVariables)
                    .send()
                    .join();

            logger.info("Case initialization job completed successfully for case: {}", caseNumber);

        } catch (Exception e) {
            logger.error("Error initializing case for job {}: {}", job.getKey(), e.getMessage(), e);
            
            // Fail the job with retry
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage("Case initialization failed: " + e.getMessage())
                    .send()
                    .join();
        }
    }

    @JobWorker(type = "validate-case-data")
    public void validateCaseData(final JobClient client, final ActivatedJob job) {
        logger.info("Validating case data for job: {}", job.getKey());

        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Long caseId = (Long) variables.get("caseId");
            String allegationType = (String) variables.get("allegationType");
            String severity = (String) variables.get("severity");
            
            logger.info("Validating case data - Case ID: {}, Allegation: {}, Severity: {}", 
                       caseId, allegationType, severity);

            // Validate required fields
            boolean isValid = true;
            StringBuilder validationErrors = new StringBuilder();

            if (caseId == null) {
                isValid = false;
                validationErrors.append("Case ID is required; ");
            }

            if (allegationType == null || allegationType.trim().isEmpty()) {
                isValid = false;
                validationErrors.append("Allegation type is required; ");
            }

            if (severity == null || severity.trim().isEmpty()) {
                isValid = false;
                validationErrors.append("Severity is required; ");
            }

            // Additional business rule validations
            if (allegationType != null && !isValidAllegationType(allegationType)) {
                isValid = false;
                validationErrors.append("Invalid allegation type; ");
            }

            if (severity != null && !isValidSeverity(severity)) {
                isValid = false;
                validationErrors.append("Invalid severity level; ");
            }

            // Prepare output variables
            Map<String, Object> outputVariables = new HashMap<>();
            outputVariables.put("caseDataValid", isValid);
            outputVariables.put("validationDate", LocalDateTime.now().toString());
            
            if (!isValid) {
                outputVariables.put("validationErrors", validationErrors.toString());
                logger.warn("Case data validation failed for case {}: {}", caseId, validationErrors.toString());
            } else {
                logger.info("Case data validation successful for case: {}", caseId);
            }

            // Complete the job
            client.newCompleteCommand(job.getKey())
                    .variables(outputVariables)
                    .send()
                    .join();

        } catch (Exception e) {
            logger.error("Error validating case data for job {}: {}", job.getKey(), e.getMessage(), e);
            
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage("Case data validation failed: " + e.getMessage())
                    .send()
                    .join();
        }
    }

    @JobWorker(type = "prepare-case-routing")
    public void prepareCaseRouting(final JobClient client, final ActivatedJob job) {
        logger.info("Preparing case routing for job: {}", job.getKey());

        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Long caseId = (Long) variables.get("caseId");
            String allegationType = (String) variables.get("allegationType");
            String severity = (String) variables.get("severity");
            
            logger.info("Preparing routing - Case ID: {}, Allegation: {}, Severity: {}", 
                       caseId, allegationType, severity);

            // Prepare variables for DMN decision
            Map<String, Object> outputVariables = new HashMap<>();
            outputVariables.put("allegationType", allegationType);
            outputVariables.put("severity", severity);
            outputVariables.put("caseId", caseId);
            outputVariables.put("routingPrepared", true);
            outputVariables.put("routingDate", LocalDateTime.now().toString());

            // Add additional context for routing decision
            outputVariables.put("requiresUrgentHandling", "CRITICAL".equals(severity) || "HIGH".equals(severity));
            outputVariables.put("requiresSpecializedTeam", isSpecializedAllegation(allegationType));

            logger.info("Case routing prepared successfully for case: {}", caseId);

            // Complete the job
            client.newCompleteCommand(job.getKey())
                    .variables(outputVariables)
                    .send()
                    .join();

        } catch (Exception e) {
            logger.error("Error preparing case routing for job {}: {}", job.getKey(), e.getMessage(), e);
            
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage("Case routing preparation failed: " + e.getMessage())
                    .send()
                    .join();
        }
    }

    /**
     * Validates if the allegation type is in the allowed list
     */
    private boolean isValidAllegationType(String allegationType) {
        String[] validTypes = {
            "Harassment", "Sexual Harassment", "Workplace Harassment",
            "Discrimination", "Age Discrimination", "Gender Discrimination", "Racial Discrimination",
            "Policy Violation", "Code of Conduct", "Fraud", "Financial Fraud", "Embezzlement",
            "Compliance Violation", "Security Breach", "Data Breach", "Criminal Activity", "Theft"
        };
        
        for (String validType : validTypes) {
            if (validType.equals(allegationType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates if the severity level is valid
     */
    private boolean isValidSeverity(String severity) {
        return "LOW".equals(severity) || "MEDIUM".equals(severity) || 
               "HIGH".equals(severity) || "CRITICAL".equals(severity);
    }

    /**
     * Determines if the allegation requires specialized team handling
     */
    private boolean isSpecializedAllegation(String allegationType) {
        return allegationType != null && (
            allegationType.contains("Security") ||
            allegationType.contains("Fraud") ||
            allegationType.contains("Criminal") ||
            allegationType.contains("Legal") ||
            allegationType.equals("Embezzlement")
        );
    }
}