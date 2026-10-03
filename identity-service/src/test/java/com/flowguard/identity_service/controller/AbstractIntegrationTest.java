package com.flowguard.identity_service.controller;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
//DirtiesContext is used to destroy the Spring context after each test class.
// This is important because the R2DBC connection pool is not closed automatically after each test class,
// which can lead to resource leaks and other issues.
// By using @DirtiesContext, we ensure that the Spring context is destroyed after each test class,
// which in turn closes the R2DBC connection pool and releases any resources associated with it.
//Test class A finishes
//    ↓
//destroy Spring context
//    ↓
//destroy R2DBC pool
//    ↓
//Test class B
//    ↓
//new container + new Spring context + new pool
public abstract class AbstractIntegrationTest {

  @Container
  static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:18")
          .withDatabaseName("identity_service_test")
          .withUsername("testuser")
          .withPassword("testpassword");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {

    registry.add("spring.r2dbc.url", () -> String.format("r2dbc:postgresql://%s:%d/%s",
            postgreSQLContainer.getHost(),
            postgreSQLContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
            postgreSQLContainer.getDatabaseName()));

    registry.add("spring.r2dbc.username", () -> postgreSQLContainer.getUsername());
    registry.add("spring.r2dbc.password", () -> postgreSQLContainer.getPassword());

    //flyway properties
    registry.add("spring.flyway.url", postgreSQLContainer::getJdbcUrl);
    registry.add("spring.flyway.user", postgreSQLContainer::getUsername);
    registry.add("spring.flyway.password", postgreSQLContainer::getPassword);
  }

}
