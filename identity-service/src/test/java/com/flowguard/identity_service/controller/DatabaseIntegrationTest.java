package com.flowguard.identity_service.controller;

import com.flowguard.identity_service.repository.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

public class DatabaseIntegrationTest extends AbstractIntegrationTest {
  @Autowired
  private OrganizationRepository organizationRepository;
  @Test
  void shouldStartPostgresAndApplyFlywayMigrations(){
    StepVerifier.create(organizationRepository.count())
            .expectNext(0L)
            .verifyComplete();
  }
}
