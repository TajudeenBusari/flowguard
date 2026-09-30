package com.flowguard.identity_service.exception;

/**
 * unknown token ──┐
 * expired token ──┼──► InvalidRefreshTokenException
 * revoked token ──┘
 */
public class InvalidRefreshTokenException extends RuntimeException{
  public InvalidRefreshTokenException(){
    super("Invalid or expired refresh token");
  }
}
