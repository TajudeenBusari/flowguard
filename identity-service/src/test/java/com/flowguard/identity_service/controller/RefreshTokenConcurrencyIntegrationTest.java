package com.flowguard.identity_service.controller;

import com.flowguard.identity_service.dto.LoginRequest;
import com.flowguard.identity_service.dto.LoginResponse;
import com.flowguard.identity_service.dto.RefreshTokenRequest;
import com.flowguard.identity_service.dto.RefreshTokenResponse;
import com.flowguard.identity_service.entity.*;
import com.flowguard.identity_service.exception.InvalidRefreshTokenException;
import com.flowguard.identity_service.repository.OrganizationRepository;
import com.flowguard.identity_service.repository.RefreshSessionRepository;
import com.flowguard.identity_service.repository.UserRepository;
import com.flowguard.identity_service.repository.UserRoleRepository;
import com.flowguard.identity_service.service.AuthService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RefreshTokenConcurrencyIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private AuthService authService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserRoleRepository userRoleRepository;

  @Autowired
  private RefreshSessionRepository refreshSessionRepository;

  //We'll use this to create the test user's passwordHash,
  // then obtain Refresh A through the real authService.login(...)
  // rather than manually inserting a refresh session
  @Autowired
  private PasswordEncoder passwordEncoder;

  private static final String TEST_EMAIL = "refresh-concurrency@flowguard.test";
  private static final String TEST_PASSWORD = "TestPassword123!";

  //creating user will require to create organization, so we need this repository as well
  @Autowired
  private OrganizationRepository organizationRepository;

  /**
   * This test will eventually send two concurrent refresh requests using exactly the same
   * Refresh A and verify that only one succeeds.
   */
  @Test
  void shouldAllowOnlyOneConcurrentRefreshForSameToken() {
    //Given
    //create organization and user, so we can obtain a valid refresh token for the user
    Instant now = Instant.now();
    Organization organization = Organization.builder()
            .name("Refresh Concurrency Test Organization")
            .slug("refresh-concurrency-test-org")
            .createdAt(now)
            .updatedAt(now)
            .build();
    Organization savedOrganization = organizationRepository.save(organization).block();
    assertNotNull(savedOrganization);
    assertNotNull(savedOrganization.getId());

    User user = User.builder()
            .organizationId(savedOrganization.getId())
            .email(TEST_EMAIL)
            .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
            .firstName("Refresh")
            .lastName("Concurrency")
            .status(UserStatus.ACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .build();
    User savedUser = userRepository.save(user).block();
    assertNotNull(savedUser);
    assertNotNull(savedUser.getId());

    //give the user a role so that the login will succeed
    UserRole savedRole = userRoleRepository.save(savedUser.getId(), Role.MEMBER).block();
    assertNotNull(savedRole);

    //Login to obtain a valid refresh token A
    LoginRequest loginRequest = new LoginRequest(savedOrganization.getId(), TEST_EMAIL, TEST_PASSWORD);
    LoginResponse loginResponse = authService.login(loginRequest).block();
    assertNotNull(loginResponse);
    assertNotNull(loginResponse.refreshToken());
    assertNotNull(loginResponse.accessToken());

    String refreshTokenA = loginResponse.refreshToken();

    Mono<RefreshTokenResponse> refreshRequest1 = authService.refreshToken(new RefreshTokenRequest(refreshTokenA));
    Mono<RefreshTokenResponse> refreshRequest2 = authService.refreshToken(new RefreshTokenRequest(refreshTokenA));

    List<Signal<RefreshTokenResponse>> results = Flux.merge(

            refreshRequest1.subscribeOn(Schedulers.parallel()).materialize(),

            refreshRequest2.subscribeOn(Schedulers.parallel()).materialize()
    ).collectList().block();

    //Why capture errors as values? Normally, if one request correctly gets InvalidRefreshTokenException, Flux.merge()
    // would terminate with that error and make it harder for us to inspect the result of both requests.
    //Now we'll get something conceptually like:
    //results[0] → RefreshTokenResponse  ✓
    //results[1] → InvalidRefreshTokenException ✓
    assertNotNull(results);
    assertEquals(2, results.size());

    //assert that one request succeeded and the other failed with InvalidRefreshTokenException
    long successCount = results.stream().filter(signal -> signal.isOnNext()).count();
    long errorCount = results.stream().filter(signal -> signal.isOnError()).count();
    assertEquals(1, successCount);
    assertEquals(1, errorCount);

    //then assert that the error is indeed InvalidRefreshTokenException
    Throwable error = results
            .stream()
            .filter(signal -> signal.isOnError())
            .map(Signal::getThrowable)
            .findFirst()
            .orElseThrow();
    assertInstanceOf(InvalidRefreshTokenException.class, error);

    //assert one replacement session exists
    List<RefreshSession> refreshSessions = refreshSessionRepository.findAllByUserId(savedUser.getId()).collectList().block();
    assertNotNull(refreshSessions);

    //count active (not revoked) sessions
    long activeSessionsCount = refreshSessions
            .stream()
            .filter(session -> session.getRevokedAt() == null)
            .count();
    assertEquals(1, activeSessionsCount);
  }
}
