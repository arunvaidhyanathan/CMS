package com.citi.cms.service;

import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;


import java.util.Map;

import com.citi.cms.dto.request.TaskCompleteRequest;

public interface WorkflowService {
    ProcessInstanceEvent startCaseWorkflow(Long caseId, Map<String, Object> variables);
    void completeTask(Long taskKey, Map<String, Object> variables);
    void completeTask(Long taskKey, Map<String, Object> variables, Long userId);
    void completeTask(Long taskKey, TaskCompleteRequest request, Long userId);
    void deployProcesses();
    Map<String, Object> getProcessVariables(Long processInstanceKey);
}
