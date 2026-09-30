package com.flowguard.identity_service.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * SecureRandom
 *      ↓
 * 32 random bytes (256 bits)
 *      ↓
 * URL-safe Base64 encoding
 *      ↓
 * raw refresh token
 */
@Component
public class RefreshTokenGenerator {

  private static final int TOKEN_BYTES = 32;
  private final SecureRandom secureRandom = new SecureRandom();

  public String generateToken() {
    byte[] tokenBytes = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(tokenBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
  }
}
