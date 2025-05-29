package com.citi.cms.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.citi.cms.service.impl.WorkflowServiceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ZeebeRestService {
    
    private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImpl.class);

    private final RestTemplate restTemplate;
    private final String zeebeRestUrl = "http://localhost:8088";
    
    public ZeebeRestService() {
        this.restTemplate = new RestTemplate();
    }
    
    public void completeUserTask(Long taskKey, Map<String, Object> variables) {
        String url = zeebeRestUrl + "/v1/user-tasks/" + taskKey + "/completion";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("variables", variables);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            logger.info("User task completed successfully: {}, Response: {}", taskKey, response.getStatusCode());
        } catch (Exception e) {
            logger.error("Error completing user task via REST: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to complete user task", e);
        }
    }
}
