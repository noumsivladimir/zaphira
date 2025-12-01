package com.zaphira.service_user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ServiceUserApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceUserApplication.class, args);
	}

}
