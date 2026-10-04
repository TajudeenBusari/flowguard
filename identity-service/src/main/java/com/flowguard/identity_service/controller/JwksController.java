package com.flowguard.identity_service.controller;

import com.flowguard.identity_service.config.RsaKeyProperties;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwksController {

  private final RsaKeyProperties rsaKeyProperties;

  /**
   *This endpoint can be made to return Mono<Map<String, Object>> if you want to make it reactive.
   * But since this is a simple endpoint that returns a static value, we can return Map<String, Object> directly.
   * There is no database, network, filesystem, or other asynchronous operation involved,
   * so wrapping it in Mono provides no performance benefit. But it does keep the controller API consistently reactive.
   * Other FlowGuard services must be able to retrieve the public key without already possessing a valid JWT.
   * Otherwise, we'd create a circular dependency. So it must be a public endpoint (.permitAll() in SecurityConfiguration).
   */
  @GetMapping("/oauth2/jwks")
  public Mono<Map<String, Object>> getJwksSet(){
    RSAKey rsaKey = new RSAKey.Builder(rsaKeyProperties.publicKey())
            .keyID("flowguard-identity-key")
            .build();
    return Mono.just(
            Map.of(
                    "keys", List.of(rsaKey.toJSONObject())
            )
    );
  }

}
