package com.citi.cms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Simplified Camunda Configuration
 * 
 * Let Spring Boot auto-configuration handle ZeebeClient creation
 * and configure it through application.yml properties instead.
 */
@Configuration
public class CamundaConfig {

    private static final Logger logger = LoggerFactory.getLogger(CamundaConfig.class);

    // Remove all @Bean methods to avoid conflicts with auto-configuration
    // Configuration is now handled through application.yml properties
    
    public CamundaConfig() {
        logger.info("Using Camunda auto-configuration for ZeebeClient");
    }
}