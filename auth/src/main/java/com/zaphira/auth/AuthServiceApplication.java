package com.zaphira.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.zaphira.auth.client")
@EnableJpaRepositories(basePackages = {
        "com.zaphira.auth.repository",
        "com.zaphira.common.repository"
})
@EntityScan(basePackages = {
        "com.zaphira.auth.model",
        "com.zaphira.common.model.entities"
})
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
