package com.flowguard.identity_service.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Raw refresh token
 *       │
 *       │ SHA-256
 *       ▼
 * 64-character hexadecimal hash
 *       │
 *       ▼
 * refresh_sessions.token_hash
 */
@Component
public class RefreshTokenHasher {
  public String hash(String refreshToken) {
    try{
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    }catch (NoSuchAlgorithmException e){
      throw new RuntimeException("SHA-256 algorithm not found", e);

    }
  }
}
