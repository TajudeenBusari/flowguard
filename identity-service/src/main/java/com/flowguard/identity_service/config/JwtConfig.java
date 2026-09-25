package com.flowguard.identity_service.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.*;


@Configuration
public class JwtConfig {
  /**
   * private.pem + public.pem -> RSAKey ->NimbusEncoder ->JwtEncoder
   * Validation checks: RSA signature, expiration, not-before, issuer
   */
  @Bean
  public JwtEncoder jwtEncoder(RsaKeyProperties rsaKeyProperties) {
    RSAKey rsaKey = new RSAKey.Builder(rsaKeyProperties.publicKey())
            .privateKey(rsaKeyProperties.privateKey())
            .build();
    var jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
    return new NimbusJwtEncoder(jwkSource);
  }

  //next we need to make the identity-service validate the JWT token it issues, so protected endpoints can use Authorization
  @Bean
  public ReactiveJwtDecoder jwtDecoder(RsaKeyProperties rsaKeyProperties) {
    NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withPublicKey(rsaKeyProperties.publicKey()).build();

    //this line of code ensures that the JWT token is validated against the issuer
    // specified in the application properties file. This is important for security,
    // as it prevents tokens from other issuers from being accepted.
    //Not just the RSA signature and expiration needs to be validated.

    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(rsaKeyProperties.issuer()));
    return decoder;
  }
}
