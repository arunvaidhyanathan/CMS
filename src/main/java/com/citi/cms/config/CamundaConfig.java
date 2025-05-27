package com.citi.cms.config;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.ZeebeClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

/**
 * Manual Camunda ZeebeClient Configuration
 * 
 * This configuration manually creates the ZeebeClient bean to ensure
 * plaintext connection works properly with local Docker setup.
 */
@Configuration
public class CamundaConfig {

    private static final Logger logger = LoggerFactory.getLogger(CamundaConfig.class);

    @Value("${zeebe.client.broker.gateway-address:localhost:26500}")
    private String gatewayAddress;

    @Value("${zeebe.client.security.plaintext:true}")
    private boolean plaintext;

    @Value("${zeebe.client.job.timeout:300000}")
    private long jobTimeout;

    @Value("${zeebe.client.job.poll-interval:100}")
    private long pollInterval;

    @Value("${zeebe.client.worker.max-jobs-active:32}")
    private int maxJobsActive;

    /**
     * Creates a ZeebeClient bean with explicit plaintext configuration
     * This overrides any auto-configuration to ensure plaintext mode works
     */
    @Bean
    @Primary
    @Profile("!test")
    public ZeebeClient zeebeClient() {
        logger.info("Creating manual ZeebeClient with gateway-address: {} (plaintext: {})", 
                   gatewayAddress, plaintext);

        ZeebeClientBuilder builder = ZeebeClient.newClientBuilder()
                .gatewayAddress(gatewayAddress)
                .defaultJobTimeout(Duration.ofMillis(jobTimeout))
                .defaultJobPollInterval(Duration.ofMillis(pollInterval))
                .defaultJobWorkerMaxJobsActive(maxJobsActive);

        if (plaintext) {
            builder.usePlaintext();
            logger.info("ZeebeClient configured for PLAINTEXT connection");
        }

        ZeebeClient client = builder.build();
        
        logger.info("ZeebeClient created successfully");
        return client;
    }

    /**
     * Test bean for testing profile
     */
    @Bean
    @Profile("test")
    public ZeebeClient testZeebeClient() {
        // For testing, you might want to use a different configuration
        // or mock client
        return ZeebeClient.newClientBuilder()
                .gatewayAddress("localhost:26500")
                .usePlaintext()
                .build();
    }
}