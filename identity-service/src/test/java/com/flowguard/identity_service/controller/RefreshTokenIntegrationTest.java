package com.flowguard.identity_service.controller;

import com.flowguard.identity_service.dto.*;
import com.flowguard.identity_service.entity.*;
import com.flowguard.identity_service.exception.InvalidRefreshTokenException;
import com.flowguard.identity_service.repository.OrganizationRepository;
import com.flowguard.identity_service.repository.RefreshSessionRepository;
import com.flowguard.identity_service.repository.UserRepository;
import com.flowguard.identity_service.repository.UserRoleRepository;
import com.flowguard.identity_service.security.RefreshTokenHasher;
import com.flowguard.identity_service.service.AuthService;
import com.flowguard.identity_service.service.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class RefreshTokenIntegrationTest extends AbstractIntegrationTest {

  //one to invoke the cleanup
  @Autowired
  private RefreshTokenService refreshTokenService;

  //one dependency to insert/inspect sessions
  @Autowired
  private RefreshSessionRepository refreshSessionRepository;

  //refresh_sessions.user_id must reference an existing user because of foreign key constraint,
  // so we need a user repository to insert a user for the test
  @Autowired
  private UserRepository userRepository;

  //user need an organization, so we need an organization repository to insert an organization for the test
  @Autowired
  private OrganizationRepository organizationRepository;

  //we need the auth service to perform login and logout operations for the idempotent logout test
  @Autowired
  private AuthService authService;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private UserRoleRepository userRoleRepository;

  @Autowired
  private RefreshTokenHasher refreshTokenHasher;

  @Test
  @DisplayName("Should delete expired refresh sessions and preserve non-expired ones")
  void shouldDeleteExpiredRefreshSessions(){

    Instant now = Instant.now();

    Organization organization = Organization.builder()
            .name("Refresh Cleanup Test Organization")
            .slug("refresh-cleanup-test-org")
            .createdAt(now)
            .updatedAt(now)
            .build();
    Organization savedOrganization = organizationRepository
            .save(organization)
            .block();
    assertNotNull(savedOrganization);
    assertNotNull(savedOrganization.getId());

    User user = User.builder()
            .organizationId(savedOrganization.getId())
            .email("refresh-cleanup-test-user@example.com")
            .passwordHash("securepassword") //just provide a non-null value for the test; actual password hashing is not relevant here
            .firstName("Refresh")
            .lastName("Cleanup")
            .status(UserStatus.ACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .build();
    User savedUser = userRepository
            .save(user)
            .block();
    assertNotNull(savedUser);
    assertNotNull(savedUser.getId());

    //createdAt → 2 hours ago
    //expiresAt → 1 hour ago
    //now       → current test time
    //expiresAt < now

    //create expired refresh session
    RefreshSession expiredSession = RefreshSession.builder()
            .userId(savedUser.getId())
            .tokenHash("expired-refresh-token-hash")
            .createdAt(now.minusSeconds(7200)) //created 2 hours ago
            .expiresAt(now.minusSeconds(3600)) //expired 1 hour ago
            .build();
    RefreshSession savedExpiredSession = refreshSessionRepository
            .save(expiredSession)
            .block();
    assertNotNull(savedExpiredSession);
    assertNotNull(savedExpiredSession.getId());

    //Create non-expired refresh session
    RefreshSession nonExpiredSession = RefreshSession.builder()
            .userId(savedUser.getId())
            .tokenHash("non-expired-refresh-token-hash")
            .createdAt(now) //created now
            .expiresAt(now.plusSeconds(3600)) //expires in 1 hour
            .build();
    RefreshSession savedNonExpiredSession = refreshSessionRepository
            .save(nonExpiredSession)
            .block();
    assertNotNull(savedNonExpiredSession);
    assertNotNull(savedNonExpiredSession.getId());

    //assert
    Long deletedCount = refreshTokenService.cleanupExpiredRefreshSessions(Instant.now()).block();
    assertNotNull(deletedCount);
    assertTrue(deletedCount >= 1 );

    //assert it does not exist anymore
    RefreshSession deletedExpiredSession = refreshSessionRepository.findById(savedExpiredSession.getId()).block();
    assertNull(deletedExpiredSession);

    // assert the active session still exists
    RefreshSession remainingActiveSession = refreshSessionRepository.findById(savedNonExpiredSession.getId()).block();
    assertNotNull(remainingActiveSession);
    assertEquals(savedNonExpiredSession.getId(), remainingActiveSession.getId());
  }

  @Test
  @DisplayName("Logout should preserve idempotency")
  void shouldAllowIdempotentLogoutAndRefreshReject() {
    //Login → Refresh A
    //Logout(A) → success
    //Logout(A) again → success
    //Refresh(A) → rejected

    //authService.login(...)
    //authService.logout(...)
    //authService.refreshToken(...)

    Instant now = Instant.now();

    Organization organization = Organization.builder()
            .name("Logout Idempotency Test Organization")
            .slug("logout-idempotency-test-org")
            .createdAt(now)
            .updatedAt(now)
            .build();
    Organization savedOrganization = organizationRepository
            .save(organization)
            .block();
    assertNotNull(savedOrganization);
    assertNotNull(savedOrganization.getId());

    String password = "Securepassword123!";

    User user = User.builder()
            .organizationId(savedOrganization.getId())
            .email("logout-idempotency-test-user@example.com")
            .passwordHash(passwordEncoder.encode(password))
            .firstName("Logout")
            .lastName("Idempotency")
            .status(UserStatus.ACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .build();
    User savedUser = userRepository
            .save(user)
            .block();
    assertNotNull(savedUser);
    assertNotNull(savedUser.getId());

    UserRole savedUserRole = userRoleRepository.save(savedUser.getId(), Role.MEMBER).block();
    assertNotNull(savedUserRole);

    LoginResponse loginResponse = authService.login(new LoginRequest(savedOrganization.getId(), savedUser.getEmail(), password)).block();
    assertNotNull(loginResponse);
    assertNotNull(loginResponse.accessToken());
    String refreshToken = loginResponse.refreshToken();
    assertNotNull(refreshToken);

    authService.logout(new LogoutRequest(refreshToken)).block();

    //call the logout again with the same refresh token, should still succeed
    authService.logout(new LogoutRequest(refreshToken)).block();

    //verify that after the second logout, the refresh token is no longer valid
    assertThrows(InvalidRefreshTokenException.class,
            () -> authService.refreshToken(new RefreshTokenRequest(refreshToken))
                    .block());
  }

  @Test
  @DisplayName("Should rotate refresh token and reject old token")
  void shouldRotateRefreshTokenAndRejectOldToken(){

    Instant now = Instant.now();

    Organization organization = Organization.builder()
            .name("Refresh Token Rotation Test Organization")
            .slug("refresh-token-rotation-test-org")
            .createdAt(now)
            .updatedAt(now)
            .build();
    Organization savedOrganization = organizationRepository
            .save(organization)
            .block();
    assertNotNull(savedOrganization);
    assertNotNull(savedOrganization.getId());

    String password = "Securepassword123!";

    User user = User.builder()
            .organizationId(savedOrganization.getId())
            .email("refresh-token-rotation-test-user@example.com")
            .passwordHash(passwordEncoder.encode(password))
            .firstName("Refresh")
            .lastName("Token")
            .status(UserStatus.ACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .build();

    User savedUser = userRepository
            .save(user)
            .block();
    assertNotNull(savedUser);
    assertNotNull(savedUser.getId());

    UserRole savedUserRole = userRoleRepository.save(savedUser.getId(), Role.MEMBER).block();
    assertNotNull(savedUserRole);

    LoginResponse loginResponse = authService.login(new LoginRequest(savedOrganization.getId(), savedUser.getEmail(), password)).block();
    assertNotNull(loginResponse);
    assertNotNull(loginResponse.accessToken());
    String refreshTokenA = loginResponse.refreshToken();
    assertNotNull(refreshTokenA);

    //rotate the refresh token
    RefreshTokenResponse refreshTokenB = authService.refreshToken(new RefreshTokenRequest(refreshTokenA)).block();
    assertNotNull(refreshTokenB);

    //verify they are not equal and refreshTokenA is no longer valid
    assertNotEquals(refreshTokenA, refreshTokenB.refreshToken());
    assertThrows(InvalidRefreshTokenException.class,
            () -> authService.refreshToken(new RefreshTokenRequest(refreshTokenA))
                    .block());
    //verify refreshTokenB is valid
    RefreshTokenResponse refreshTokenC = authService.refreshToken(new RefreshTokenRequest(refreshTokenB.refreshToken())).block();
    assertNotNull(refreshTokenC);
    assertNotNull(refreshTokenC.refreshToken());
    assertNotNull(refreshTokenC.accessToken());
    assertNotEquals(refreshTokenB.refreshToken(), refreshTokenC.refreshToken());
  }

  @Test
  @DisplayName("Should preserve revoked session until it expires")
  void shouldPreserveRevokedSessionUntilExpires(){
    Instant now = Instant.now();

    Organization organization = Organization.builder()
            .name("Revoked Session Test Organization")
            .slug("revoked-session-test-org")
            .createdAt(now)
            .updatedAt(now)
            .build();
    Organization savedOrganization = organizationRepository
            .save(organization)
            .block();
    assertNotNull(savedOrganization);
    assertNotNull(savedOrganization.getId());

    User user = User.builder()
            .organizationId(savedOrganization.getId())
            .email("revoked-session-test-user@example.com")
            .passwordHash("not-used-in-this-test") //just provide a non-null value for the test; actual password hashing is not relevant here
            .firstName("Revoked")
            .lastName("Session")
            .status(UserStatus.ACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .build();
    User savedUser = userRepository
            .save(user)
            .block();
    assertNotNull(savedUser);
    assertNotNull(savedUser.getId());

    //create a revoked but non-expired refresh session
    RefreshSession revokedSession = RefreshSession.builder()
            .userId(savedUser.getId())
            .tokenHash("revoked-refresh-token-hash")
            .createdAt(now.minusSeconds(3600)) //created 1 hour ago
            .expiresAt(now.plusSeconds(3600)) //expires in 1 hour
            .revokedAt(now.minusSeconds(1800)) //revoked 30 minutes ago
            .build();
    RefreshSession savedRevokedSession = refreshSessionRepository
            .save(revokedSession)
            .block();
    assertNotNull(savedRevokedSession);
    assertNotNull(savedRevokedSession.getId());
    Long deletedCount = refreshTokenService.cleanupExpiredRefreshSessions(Instant.now()).block();
    assertNotNull(deletedCount);
    //assert that the revoked session is still present because it has not expired yet
    //revoked + not expired → preserved
    //expired               → deleted
    //In other words, revoked_at makes the token unusable, while expires_at determines when the cleanup job removes the row
    RefreshSession remainingRevokedSession = refreshSessionRepository.findById(savedRevokedSession.getId()).block();
    assertNotNull(remainingRevokedSession);
    assertNotNull(remainingRevokedSession.getRevokedAt());
    assertEquals(savedRevokedSession.getId(), remainingRevokedSession.getId());
  }

  @Test
  @DisplayName("Should reject refresh token when user is suspended")
  void shouldRejectRefreshTokenWhenUserIsSuspended() {

    //ACTIVE user → login → Refresh A
    //user becomes SUSPENDED
    //Refresh A → rejected
    Instant now = Instant.now();

    Organization organization = Organization.builder()
            .name("Suspended User Test Organization")
            .slug("suspended-user-test-org")
            .createdAt(now)
            .updatedAt(now)
            .build();
    Organization savedOrganization = organizationRepository
            .save(organization)
            .block();
    assertNotNull(savedOrganization);
    assertNotNull(savedOrganization.getId());

    String password = "Securepassword123!";

    User user = User.builder()
            .organizationId(savedOrganization.getId())
            .email("to-be-suspended-user@example.com")
            .passwordHash(passwordEncoder.encode(password))
            .firstName("toBeSuspended")
            .lastName("UserToBeSuspended")
            .status(UserStatus.ACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .build();
    User savedUser = userRepository
            .save(user)
            .block();
    assertNotNull(savedUser);
    assertNotNull(savedUser.getId());

    UserRole savedUserRole = userRoleRepository.save(savedUser.getId(), Role.MEMBER).block();
    assertNotNull(savedUserRole);

    LoginResponse loginResponse = authService.login(new LoginRequest(savedOrganization.getId(), savedUser.getEmail(), password)).block();
    assertNotNull(loginResponse);
    assertNotNull(loginResponse.accessToken());
    String refreshToken = loginResponse.refreshToken();
    assertNotNull(refreshToken);

    //suspend user
    //Here we're deliberately updating the repository directly rather than using your normal status-management service.
    // That isolates the behavior we want this particular test to prove
    savedUser.setStatus(UserStatus.SUSPENDED);
    User suspendedUser = userRepository.save(savedUser).block();
    assertNotNull(suspendedUser);
    assertEquals(UserStatus.SUSPENDED, suspendedUser.getStatus());

    //verify that the refresh token is rejected after the user is suspended
    assertThrows(InvalidRefreshTokenException.class, () -> authService
            .refreshToken(new RefreshTokenRequest(refreshToken)).block());
  }

  @Test
  @DisplayName("Should reject refresh token when user is disabled")
  void shouldRejectRefreshTokenWhenUserIsDisabled() {

    //ACTIVE user → login → Refresh A
    //user becomes DISABLED
    //Refresh A → rejected
    Instant now = Instant.now();

    Organization organization = Organization.builder()
            .name("Disabled User Test Organization")
            .slug("disabled-user-test-org")
            .createdAt(now)
            .updatedAt(now)
            .build();
    Organization savedOrganization = organizationRepository
            .save(organization)
            .block();
    assertNotNull(savedOrganization);
    assertNotNull(savedOrganization.getId());

    String password = "Securepassword123!";

    User user = User.builder()
            .organizationId(savedOrganization.getId())
            .email("to-be-disabled-user@example.com")
            .passwordHash(passwordEncoder.encode(password))
            .firstName("toBeDisabled")
            .lastName("UserToBeDisabled")
            .status(UserStatus.ACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .build();
    User savedUser = userRepository
            .save(user)
            .block();
    assertNotNull(savedUser);
    assertNotNull(savedUser.getId());

    UserRole savedUserRole = userRoleRepository.save(savedUser.getId(), Role.MEMBER).block();
    assertNotNull(savedUserRole);

    LoginResponse loginResponse = authService.login(new LoginRequest(savedOrganization.getId(), savedUser.getEmail(), password)).block();
    assertNotNull(loginResponse);
    assertNotNull(loginResponse.accessToken());
    String refreshToken = loginResponse.refreshToken();
    assertNotNull(refreshToken);

    //set to DISABLED
    savedUser.setStatus(UserStatus.DISABLED);
    User disabledUser = userRepository.save(savedUser).block();
    assertNotNull(disabledUser);
    assertEquals(UserStatus.DISABLED, disabledUser.getStatus());

    //assert that the refresh token is rejected after the user is disabled
    assertThrows(InvalidRefreshTokenException.class, () -> authService.refreshToken(new RefreshTokenRequest(refreshToken)).block());
  }

  @Test
  @DisplayName("Should reject expired refresh token")
  void shouldRejectExpiredRefreshToken() {

    Instant now = Instant.now();
    Organization organization = Organization.builder()
            .name("Expired Refresh Token Test Organization")
            .slug("expired-refresh-token-test-org")
            .createdAt(now)
            .updatedAt(now)
            .build();
    Organization savedOrganization = organizationRepository
            .save(organization)
            .block();
    assertNotNull(savedOrganization);
    assertNotNull(savedOrganization.getId());

    User user = User.builder()
            .organizationId(savedOrganization.getId())
            .email("to-be-expired-refresh-token-user@example.com")
            .passwordHash("not-used-in-this-test") //just provide a non-null value for the test; actual password hashing is not relevant here
            .firstName("toBeExpired")
            .lastName("RefreshTokenUser")
            .status(UserStatus.ACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .build();
    User savedUser = userRepository
            .save(user)
            .block();
    assertNotNull(savedUser);
    assertNotNull(savedUser.getId());

    // we don't need the BCrypt or role because we are going to create expired refresh token directly in the repository rather than going through the login flow
    String rawRefreshToken = "expired-refresh-token";
    String hashedRefreshToken = refreshTokenHasher.hash(rawRefreshToken);

    //rawRefreshToken
    //      ↓ SHA-256
    //tokenHash stored in DB
    //
    //expiresAt < now
    RefreshSession expiredSession = RefreshSession.builder()
            .userId(savedUser.getId())
            .tokenHash(hashedRefreshToken)
            .createdAt(now.minusSeconds(7200)) //created 2 hours ago
            .expiresAt(now.minusSeconds(3600)) //expired 1 hour ago
            .build();
    RefreshSession savedExpiredSession = refreshSessionRepository
            .save(expiredSession)
            .block();
    assertNotNull(savedExpiredSession);
    assertNotNull(savedExpiredSession.getId());

    //verify that the expired refresh token is rejected
    assertThrows(InvalidRefreshTokenException.class, () -> authService.refreshToken(new RefreshTokenRequest(rawRefreshToken)).block());
  }

  @Test
  @DisplayName("Should reject unknown refresh token")
  void shouldRejectUnknownRefreshToken(){

    //verify that an unknown refresh token is rejected
    String unknownRefreshToken = "unknown-refresh-token";
    assertThrows(InvalidRefreshTokenException.class, () -> authService.refreshToken(new RefreshTokenRequest(unknownRefreshToken)).block());
  }

  @Test
  @DisplayName("Should reject revoked refresh token")
  void shouldRejectRevokedRefreshToken(){
    //exists
    //hash matches
    //not expired
    //revokedAt is not null
    //
    //→ rejected

    Instant now = Instant.now();
    Organization organization = Organization.builder()
            .name("Revoked Refresh Token Test Organization")
            .slug("revoked-refresh-token-test-org")
            .createdAt(now)
            .updatedAt(now)
            .build();
    Organization savedOrganization = organizationRepository
            .save(organization)
            .block();
    assertNotNull(savedOrganization);
    assertNotNull(savedOrganization.getId());

    User user = User.builder()
            .organizationId(savedOrganization.getId())
            .email("revoked-refresh-token-user@example.com")
            .passwordHash("not-used-in-this-test") //just provide a non-null value for the test; actual password hashing is not relevant here
            .firstName("Revoked")
            .lastName("RefreshTokenUser")
            .status(UserStatus.ACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .build();
    User savedUser = userRepository.save(user).block();
    assertNotNull(savedUser);
    assertNotNull(savedUser.getId());

    //create the raw token and hash it
    String rawRefreshToken = "revoked-refresh-token";
    String hashedRefreshToken = refreshTokenHasher.hash(rawRefreshToken);

    RefreshSession revokedSession = RefreshSession.builder()
            .userId(savedUser.getId())
            .tokenHash(hashedRefreshToken)
            .createdAt(now.minusSeconds(3600)) //created 1 hour ago
            .expiresAt(now.plusSeconds(3600)) //expires in 1 hour
            .revokedAt(now.minusSeconds(1800)) //revoked 30 minutes ago
            .build();
    RefreshSession savedRevokedSession = refreshSessionRepository.save(revokedSession).block();
    assertNotNull(savedRevokedSession);
    assertNotNull(savedRevokedSession.getId());

    //verify that the revoked refresh token is rejected
    //Token exists
    //Hash matches
    //Token not expired
    //revokedAt != null
    //
    //Result → InvalidRefreshTokenException
    assertThrows(InvalidRefreshTokenException.class, () -> authService.refreshToken(new RefreshTokenRequest(rawRefreshToken)).block());
  }

}
