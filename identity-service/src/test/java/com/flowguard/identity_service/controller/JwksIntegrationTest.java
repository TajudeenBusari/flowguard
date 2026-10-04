package com.flowguard.identity_service.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

@AutoConfigureWebTestClient
public class JwksIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private WebTestClient webTestClient;

  @Test
  void shouldExposePublicJwksWithoutAuthentication() {
    webTestClient
            .get()
            .uri("/oauth2/jwks")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.keys").isArray()
            .jsonPath("$.keys.length()").isEqualTo(1)
            .jsonPath("$.keys[0].kty").isEqualTo("RSA")
            .jsonPath("$.keys[0].kid").isEqualTo("flowguard-identity-key")
            .jsonPath("$.keys[0].n").isNotEmpty()
            .jsonPath("$.keys[0].e").isNotEmpty()
            .jsonPath("$.keys[0].d").doesNotExist() // Ensure private exponent is not exposed
            .jsonPath("$.keys[0].p").doesNotExist()
            .jsonPath("$.keys[0].q").doesNotExist()
            .jsonPath("$.keys[0].dp").doesNotExist()
            .jsonPath("$.keys[0].dq").doesNotExist()
            .jsonPath("$.keys[0].qi").doesNotExist();
  }
}
