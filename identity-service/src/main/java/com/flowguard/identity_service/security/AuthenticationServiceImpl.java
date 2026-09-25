package com.flowguard.identity_service.security;

import com.flowguard.identity_service.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

  private final FlowGuardUserDetailsService flowGuardUserDetailsService;
  private final PasswordEncoder passwordEncoder;

  //The boundedElastic() part is intentional. BCrypt verification is CPU-intensive/synchronous,
  // and the identity service is WebFlux, so we don't want BCrypt running directly on the Netty event-loop thread.
  @Override
  public Mono<Authentication> authenticate(LoginRequest request) {
    return flowGuardUserDetailsService.loadUser(request.organizationId(), request.email())
            .flatMap(principal -> Mono.fromCallable(() -> {
              if(!passwordEncoder.matches(request.password(), principal.getPassword())){
                throw new BadCredentialsException("Invalid email or password");
              }
              return (Authentication) UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
            }).subscribeOn(Schedulers.boundedElastic()));
  }
}
