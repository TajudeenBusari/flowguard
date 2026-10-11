package com.tjtechy.workflowservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@Configuration
public class JwtConfig {

  @Bean
  public ReactiveJwtDecoder reactiveJwtDecoder(@Value("${security.jwt.jwk-set-uri}") String jwkSetUrl,
                                               @Value("${security.jwt.issuer}") String issuerUri) {
    NimbusReactiveJwtDecoder decoder =  NimbusReactiveJwtDecoder
            .withJwkSetUri(jwkSetUrl)
            .build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
    return decoder;
  }
}
