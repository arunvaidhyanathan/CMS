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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.citi.cms.dto.request.TaskCompleteRequest;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class WorkflowServiceImpl implements WorkflowService {

    @Override
    public void completeTask(Long taskKey, TaskCompleteRequest request, Long userId) {
        logger.info("Completing task with key: {} and request: {}", taskKey, request);

        Map<String, Object> variables = new HashMap<>();
        // You might need to populate variables from the TaskCompleteRequest
        // For example:
        // variables.put("someVariable", request.getSomeValue());

        completeTask(taskKey, variables, userId);
    }

    private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImpl.class);

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
                    .bpmnProcessId("case-management-process")
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
        completeTask(taskKey, variables);
    }

    @Override
    public void completeTask(Long taskKey, Map<String, Object> variables, Long userId) {
        logger.info("Completing task with key: {}", taskKey);
        
        try {
            // Complete the Zeebe task
            zeebeClient.newCompleteCommand(taskKey)
                    .variables(variables)
                    .send()
                    .join();

            // Record the transition in database
            recordCaseTransition(taskKey, variables, userId);

            logger.info("Task completed successfully: {}", taskKey);
            
        } catch (Exception e) {
            logger.error("Error completing task {}: {}", taskKey, e.getMessage(), e);
            throw new RuntimeException("Failed to complete task", e);
        }
    }

    @Override
    public void deployProcesses() {
        logger.info("Deploying BPMN processes | DMN tables | Forms");
        
        try {
            zeebeClient.newDeployResourceCommand()
                    .addResourceFromClasspath("processes/cms_workflow.bpmn")
                    .addResourceFromClasspath("processes/cms_wf.dmn")
                    .addResourceFromClasspath("form/*")
                    .send()
                    .join();
            
            logger.info("Processes deployed successfully");
            
        } catch (Exception e) {
            logger.error("Error deploying processes: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to deploy processes", e);
        }
    }

    @Override
    public Map<String, Object> getProcessVariables(Long processInstanceKey) {
        // This is a placeholder for Zeebe, we can typically get variables through job workers
        // or by querying the process instance through Operate API
        return new HashMap<>();
    }

    private void recordCaseTransition(Long taskKey, Map<String, Object> variables, Long userId) {
        try {
            Long caseId = (Long) variables.get("caseId");
            if (caseId == null) {
                logger.warn("No caseId found in task variables for task: {}", taskKey);
                return;
            }

            com.citi.cms.entity.Case caseEntity = caseRepository.findByCaseId(caseId).orElse(null);
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

            transition.setTaskName(taskName);
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

        } catch (Exception e) {
            logger.error("Error recording case transition: {}", e.getMessage(), e);
            // Don't throw here to avoid breaking the workflow
        }
    }
}
