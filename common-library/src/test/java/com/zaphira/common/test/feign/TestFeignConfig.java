package com.zaphira.common.test.feign;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Feign configuration for test-sync profile.
 * 
 * Enables Feign clients for synchronous inter-service communication during tests.
 * Replaces Kafka messaging with REST-based Feign calls.
 * 
 * Activated by: @ActiveProfiles("test-sync")
 */
@Configuration
@Profile("test-sync")
@EnableFeignClients(basePackages = "com.zaphira.common.test.feign")
public class TestFeignConfig {
}
