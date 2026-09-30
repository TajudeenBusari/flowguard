package com.flowguard.identity_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "refresh_sessions")
public class RefreshSession {

  //no raw fresh token field is stored in the database, only the hash of the token is stored for security reasons
  //no manual generation of id when creating session. It is generated automatically by the database when the session is created
  //This avoids the R2DBC save() issue we encountered earlier where a pre-populated ID caused Spring Data to treat a new entity as existing.
  //The refresh token itself won't be a JWT. We'll generate a cryptographically secure random opaque token and store only its SHA-256 hash.
  @Id
  private UUID id;
  private UUID userId;
  private String tokenHash;
  private Instant expiresAt;
  private Instant revokedAt;
  private Instant createdAt;
}
