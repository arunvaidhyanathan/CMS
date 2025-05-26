package com.citi.cms.workflow.workers;

import com.citi.cms.entity.Case;
import com.citi.cms.entity.WorkItem;
import com.citi.cms.repository.CaseRepository;
import com.citi.cms.repository.WorkItemRepository;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TaskAssignmentWorker {

    private static final Logger logger = LoggerFactory.getLogger(TaskAssignmentWorker.class);

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private WorkItemRepository workItemRepository;

    @JobWorker(type = "create-work-item")
    public void createWorkItem(final JobClient client, final ActivatedJob job) {
        logger.info("Creating work item for job: {}", job.getKey());

        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Long caseId = (Long) variables.get("caseId");
            String taskName = (String) variables.get("taskName");
            String taskType = (String) variables.get("taskType");
            Long assignedToUserId = (Long) variables.get("assignedToUserId");

            Case caseEntity = caseRepository.findByCaseId(caseId).orElse(null);
            if (caseEntity != null) {
                WorkItem workItem = new WorkItem();
                workItem.setCaseEntity(caseEntity);
                workItem.setTaskKey(job.getKey());
                workItem.setTaskName(taskName);
                workItem.setTaskType(taskType);
                workItem.setStatus("PENDING");
                
                // Set business ID
                Long nextWorkItemId = getNextWorkItemId();
                workItem.setWorkItemId(nextWorkItemId);

                workItemRepository.save(workItem);
                logger.info("Work item created successfully: {}", workItem.getWorkItemId());
            }

            client.newCompleteCommand(job.getKey()).send().join();

        } catch (Exception e) {
            logger.error("Error creating work item: {}", e.getMessage(), e);
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage("Failed to create work item: " + e.getMessage())
                    .send()
                    .join();
        }
    }

    private Long getNextWorkItemId() {
        // Simple implementation - in production, use database sequences
        return System.currentTimeMillis();
    }
}