package com.flowguard.identity_service.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

  @Value("${api.endpoint.base-url}")
  private String baseUrl;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12); // 12 is the strength parameter
  }

  //convert JWT roles to ROLE_OWNER, ROLE_ADMIN, ROLE_USER, etc. so that we can use hasRole/hasAuthority/hasAnyAuthority("OWNER") in the security configuration
  @Bean
  public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthoritiesClaimName("roles");
    authoritiesConverter.setAuthorityPrefix("ROLE_");

    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
  }

  /**
   * With the current implementation:
   * For example Member A in the organization cannot access the profile of Member B in the same organization.
   * This is because /me does not take the userId from client. The code derives identity from the verified JWT token.
   * So, the userId in the JWT token is used to fetch the profile of the authenticated user.
   */
  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                    .pathMatchers(HttpMethod.POST, baseUrl + "/auth/login").permitAll()
                    .pathMatchers(HttpMethod.POST, baseUrl + "/organizations" ).permitAll()
                    .pathMatchers(HttpMethod.POST, baseUrl + "/organizations/users").hasRole("OWNER")
                    .pathMatchers(HttpMethod.GET, baseUrl + "/organizations/users").hasAnyRole("OWNER", "ADMIN")
                    .anyExchange()
                    .authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .build();
  }

}
