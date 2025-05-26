package com.citi.cms.config;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProvider;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProviderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class CamundaConfig {

    @Value("${camunda.client.zeebe.cloud.cluster-id}")
    private String clusterId;

    @Value("${camunda.client.zeebe.cloud.client-id}")
    private String clientId;

    @Value("${camunda.client.zeebe.cloud.client-secret}")
    private String clientSecret;

    @Value("${camunda.client.zeebe.cloud.region}")
    private String region;

    @Value("${camunda.client.zeebe.gateway-address}")
    private String gatewayAddress;

    @Value("${camunda.client.zeebe.security.plaintext}")
    private boolean plaintext;

    @Bean
    @Profile("!test")
    public ZeebeClient zeebeClient() {
        if (plaintext) {
            // development
            return ZeebeClient.newClientBuilder()
                    .gatewayAddress(gatewayAddress)
                    .usePlaintext()
                    .build();
        } else {
            // Camunda Cloud
            OAuthCredentialsProvider credentialsProvider = new OAuthCredentialsProviderBuilder()
                    .authorizationServerUrl("https://login.cloud.camunda.io/oauth/token")
                    .audience(String.format("%s.%s.zeebe.camunda.io", clusterId, region))
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .build();

            return ZeebeClient.newClientBuilder()
                    .gatewayAddress(String.format("%s.%s.zeebe.camunda.io:443", clusterId, region))
                    .credentialsProvider(credentialsProvider)
                    .build();
        }
    }
}