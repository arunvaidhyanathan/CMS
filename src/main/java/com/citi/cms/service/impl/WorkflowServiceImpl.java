package com.citi.cms.service.impl;

import com.citi.cms.entity.Case;
import com.citi.cms.entity.CaseTransition;
import com.citi.cms.entity.User;
import com.citi.cms.repository.CaseRepository;
import com.citi.cms.repository.CaseTransitionRepository;
import com.citi.cms.repository.UserRepository;
import com.citi.cms.service.WorkflowService;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.citi.cms.config.ZeebeRestService;
import com.citi.cms.dto.request.TaskCompleteRequest;

import java.util.HashMap;
import java.util.Map;
import java.io.InputStream;
import java.io.IOException;


@Service
@Transactional
public class WorkflowServiceImpl implements WorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImpl.class);

    @Autowired
    private ZeebeRestService zeebeRestService;

    @Autowired
    private ZeebeClient zeebeClient;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private CaseTransitionRepository caseTransitionRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public ProcessInstanceEvent startCaseWorkflow(Long caseId, Map<String, Object> variables) {
        logger.info("Starting workflow for case ID: {}", caseId);
        
        try {
            // Add case ID to variables
            variables.put("caseId", caseId);
            variables.put("processStartTime", System.currentTimeMillis());
            
            ProcessInstanceEvent processInstance = zeebeClient.newCreateInstanceCommand()
                    // .bpmnProcessId("case-management-process")
                    .bpmnProcessId("Process_CMS_Workflow")
                    .latestVersion()
                    .variables(variables)
                    .send()
                    .join();

            // Update case with workflow instance key
            Case caseEntity = caseRepository.findByCaseId(caseId)
                    .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));
            
            caseEntity.setWorkflowInstanceKey(processInstance.getProcessInstanceKey());
            caseRepository.save(caseEntity);

            logger.info("Workflow started successfully. Process Instance Key: {}", 
                       processInstance.getProcessInstanceKey());
            
            return processInstance;
            
        } catch (Exception e) {
            logger.error("Error starting workflow for case {}: {}", caseId, e.getMessage(), e);
            throw new RuntimeException("Failed to start workflow", e);
        }
    }

    @Override
    public void completeTask(Long taskKey, Map<String, Object> variables) {
        completeTask(taskKey, variables, null);
    }

    @Override
    public void completeTask(Long taskKey, Map<String, Object> variables, Long userId) {
        logger.info("Completing task with key: {}", taskKey);
        
        try {
            // Complete the Zeebe task
            // zeebeClient.newCompleteCommand(taskKey)
            //         .variables(variables)
            //         .send()
            //         .join();
            // zeebeClient.newUserTaskCompleteCommand(taskKey)
            //     .variables(variables)
            //     .action("COMPLETE") // Optional custom action
            //     .send()
            //     .join();
            
                // Use the REST service instead of Java client
                zeebeRestService.completeUserTask(taskKey, variables);

            // Record the transition in database
            recordCaseTransition(taskKey, variables, userId);

            logger.info("Task completed successfully: {}", taskKey);
            
        } catch (Exception e) {
            logger.error("Error completing task {}: {}", taskKey, e.getMessage(), e);
            throw new RuntimeException("Failed to complete task", e);
        }
    }

    @Override
    public void completeTask(Long taskKey, TaskCompleteRequest request, Long userId) {
        logger.info("Completing task with key: {} and request", taskKey);

        Map<String, Object> variables = new HashMap<>(request.getVariables());
        
        // Add additional variables from request
        if (request.getOutcome() != null) {
            variables.put("outcome", request.getOutcome());
        }
        if (request.getComments() != null) {
            variables.put("comments", request.getComments());
        }
        
        // Add user information
        variables.put("completedByUserId", userId);
        variables.put("completionDate", System.currentTimeMillis());

        completeTask(taskKey, variables, userId);
    }

    @Override
    public void deployProcesses() {
        logger.info("Deploying BPMN processes, DMN tables, and Forms");
        
        try {
            // Check if resources exist before deployment
            validateResourcesExist();
            
            DeploymentEvent deployment = zeebeClient.newDeployResourceCommand()
                    // Deploy the simple BPMN process (not the complex collaboration)
                    .addResourceFromClasspath("processes/cms_workflow.bpmn")
                    // Deploy DMN decision table
                    .addResourceFromClasspath("processes/allegation-classification.dmn")
                    // Deploy forms (only if they exist)
                    // .addResourceFromClasspath("forms/eoIntakeForm.form")
                    // .addResourceFromClasspath("forms/hrAssignmentForm.form")
                    // .addResourceFromClasspath("forms/legalAssignmentForm.form")
                    // .addResourceFromClasspath("forms/csisAssignmentForm.form")
                    // .addResourceFromClasspath("forms/investigationFinalizationForm.form")
                    // .addResourceFromClasspath("forms/closeEoForm.form")
                    .send()
                    .join();
            
            logger.info("Deployment successful. Deployment key: {}", deployment.getKey());
            
            // Log deployed resources
            deployment.getProcesses().forEach(process -> 
                logger.info("Deployed process: {} v{} with key {}", 
                           process.getBpmnProcessId(), process.getVersion(), process.getProcessDefinitionKey()));
            
            deployment.getDecisionRequirements().forEach(dmn -> 
                logger.info("Deployed DMN: {} v{} with key {}", 
                           dmn.getDmnDecisionRequirementsId(), dmn.getVersion(), dmn.getDecisionRequirementsKey()));
            
            deployment.getForm().forEach(form -> 
                logger.info("Deployed form: {} with key {}", form.getFormId(), form.getFormKey()));
            
        } catch (Exception e) {
            logger.error("Error deploying processes: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to deploy processes: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getProcessVariables(Long processInstanceKey) {
        // This is a placeholder for Zeebe - variables are typically retrieved through job workers
        // or by querying Operate API in a real implementation
        logger.debug("Getting process variables for instance: {}", processInstanceKey);
        return new HashMap<>();
    }

    private void validateResourcesExist() {
        String[] requiredResources = {
            "processes/cms_workflow.bpmn",
            "processes/allegation-classification.dmn",
            // "forms/eoIntakeForm.form",
            // "forms/hrAssignmentForm.form",
            // "forms/legalAssignmentForm.form",
            // "forms/csisAssignmentForm.form",
            // "forms/investigationFinalizationForm.form",
            // "forms/closeEoForm.form"
        };
        
        for (String resource : requiredResources) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(resource)) {
                if (is == null) {
                    logger.warn("Resource not found: {}", resource);
                } else {
                    logger.debug("Resource found: {}", resource);
                }
            } catch (IOException e) {
                logger.warn("Error checking resource {}: {}", resource, e.getMessage());
            }
        }
    }

    private void recordCaseTransition(Long taskKey, Map<String, Object> variables, Long userId) {
        try {
            Long caseId = (Long) variables.get("caseId");
            if (caseId == null) {
                logger.warn("No caseId found in task variables for task: {}", taskKey);
                return;
            }

            Case caseEntity = caseRepository.findByCaseId(caseId).orElse(null);
            if (caseEntity == null) {
                logger.warn("Case not found for ID: {}", caseId);
                return;
            }

            User user = null;
            if (userId != null) {
                user = userRepository.findByUserId(userId).orElse(null);
            }

            CaseTransition transition = new CaseTransition();
            transition.setCaseEntity(caseEntity);
            transition.setTaskKey(taskKey);
            transition.setFromStatus(caseEntity.getStatus());
            transition.setWorkflowInstanceKey(caseEntity.getWorkflowInstanceKey());
            transition.setPerformedBy(user);

            // Extract task name and outcome from variables
            String taskName = (String) variables.get("taskName");
            String outcome = (String) variables.get("outcome");
            String comments = (String) variables.get("comments");

            transition.setTaskName(taskName != null ? taskName : "TASK_COMPLETION");
            transition.setComments(comments);

            // Convert variables to string map for storage
            Map<String, String> variableStrings = new HashMap<>();
            variables.forEach((key, value) -> {
                if (value != null) {
                    variableStrings.put(key, value.toString());
                }
            });
            transition.setVariables(variableStrings);

            caseTransitionRepository.save(transition);
            
            logger.debug("Case transition recorded for case: {} task: {}", caseId, taskKey);

        } catch (Exception e) {
            logger.error("Error recording case transition: {}", e.getMessage(), e);
            // Don't throw here to avoid breaking the workflow
        }
    }
}