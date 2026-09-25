package com.flowguard.identity_service;

import com.flowguard.identity_service.config.RsaKeyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RsaKeyProperties.class)
public class IdentityServiceApplication {

	public static void main(String[] args) {

		SpringApplication.run(IdentityServiceApplication.class, args);
	}

}
