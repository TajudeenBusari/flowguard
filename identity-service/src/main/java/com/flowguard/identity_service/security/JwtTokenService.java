package com.flowguard.identity_service.security;

import com.flowguard.identity_service.config.RsaKeyProperties;
import com.flowguard.identity_service.entity.Role;
import com.flowguard.identity_service.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;



@Service
@RequiredArgsConstructor
public class JwtTokenService {
  private final JwtEncoder jwtEncoder;
  private final RsaKeyProperties rsaKeyProperties;

  /**
   * Two important decisions here: sub is the immutable user UUID rather than email,
   * and organizationId is embedded in the signed token so downstream services
   * can derive tenant ownership from the authenticated identity
   * instead of trusting an organization ID supplied by the client.

   */
  public String createToken(Authentication authentication) {

    FlowGuardPrincipal principal = (FlowGuardPrincipal) authentication.getPrincipal();

    /*
     * since roles and permission are not necessarily same, when the security model eventually contains permissions beyond
     * roles, we can then introduce authorities claim to the token, but for now we will just include roles in the token
     */
    User user = principal.getUser();
    Instant now = Instant.now();
    Instant expiresAt = now.plus(rsaKeyProperties.accessTokenExpiration());


    JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(rsaKeyProperties.issuer())
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(user.getId().toString())
            .claim("organizationId", user.getOrganizationId().toString())
            .claim("email", user.getEmail())
            .claim("roles", principal.getRoles().stream().map(Role::name).toList())
            .build();
    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();

    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

}
