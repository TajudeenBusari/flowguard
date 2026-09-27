package com.flowguard.identity_service.controller;

import com.flowguard.identity_service.entity.Organization;
import com.flowguard.identity_service.entity.Role;
import com.flowguard.identity_service.entity.User;
import com.flowguard.identity_service.entity.UserStatus;
import com.flowguard.identity_service.exception.LastActiveOwnerException;
import com.flowguard.identity_service.repository.OrganizationRepository;
import com.flowguard.identity_service.repository.UserRepository;
import com.flowguard.identity_service.repository.UserRoleRepository;
import com.flowguard.identity_service.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OwnerConcurrencyIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private OrganizationRepository organizationRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserRoleRepository userRoleRepository;

  @Autowired
  private UserService userService;

  @Test
  @DisplayName("Should prevent concurrent removal of last active owners")
  void shouldPreventConcurrentRemovalOfLastActiveOwners() {

    Organization organization = Organization.builder()
            .name("Concurrent Test Organization")
            .slug("concurrent-test-org")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    Organization savedOrganization = organizationRepository.save(organization).block();
    assertNotNull(savedOrganization);
    assertNotNull(savedOrganization.getId());

    Instant now = Instant.now();
    User ownerOne = User.builder()
            .organizationId(savedOrganization.getId())
            .email("owner1@concurrency-test.com")
            .passwordHash("not-used-in-this-test")
            .firstName("Owner")
            .lastName("One")
            .status(UserStatus.ACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .build();

    User ownerTwo = User.builder()
            .organizationId(savedOrganization.getId())
            .email("owner2@concurrency-test.com")
            .passwordHash("not-used-in-this-test")
            .firstName("Owner")
            .lastName("Two")
            .status(UserStatus.ACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .build();

    User savedOwnerOne = userRepository.save(ownerOne).block();
    User savedOwnerTwo = userRepository.save(ownerTwo).block();

    assertNotNull(savedOwnerOne);
    assertNotNull(savedOwnerOne.getId());

    assertNotNull(savedOwnerTwo);
    assertNotNull(savedOwnerTwo.getId());

    //now assign the OWNER role to both users
    userRoleRepository.save(savedOwnerOne.getId(), Role.OWNER).block();
    userRoleRepository.save(savedOwnerTwo.getId(), Role.OWNER).block();

    Long activeOwnerCounts = userRoleRepository.countActiveOwnersByOrganizationId(savedOrganization.getId()).block();
    assertEquals(2L, activeOwnerCounts);

    User verifiedOwnerOne = userRepository.findByIdAndOrganizationId(savedOwnerOne.getId(), savedOrganization.getId()).block();
    User verifiedOwnerTwo = userRepository.findByIdAndOrganizationId(savedOwnerTwo.getId(), savedOrganization.getId()).block();
    assertNotNull(verifiedOwnerOne);
    assertNotNull(verifiedOwnerTwo);

    //the actual concurrent removal test would require multi-threading or async calls to simulate real concurrency.
    //we will not use .block() here to simulate the logic
    //That's important because Reactor publishers are lazy
    Mono<User> removeOwnerOne = userService.removeRoleFromUser(savedOwnerOne.getId(), savedOrganization.getId(), Role.OWNER);
    Mono<User> removeOwnerTwo = userService.removeRoleFromUser(savedOwnerTwo.getId(), savedOrganization.getId(), Role.OWNER);

    //attempt to remove the two owners concurrently
    // assert that the concurrentRemoval completes successfully.
    Mono<User> concurrentRemoval = Mono.whenDelayError(removeOwnerOne.subscribeOn(Schedulers.parallel()),
            removeOwnerTwo.subscribeOn(Schedulers.parallel()))
            .then(Mono.empty());

    StepVerifier.create(concurrentRemoval)
            .expectError(LastActiveOwnerException.class)
    .verify();

    Long finalActiveOwnerCounts = userRoleRepository.countActiveOwnersByOrganizationId(savedOrganization.getId())
            .block();
    assertEquals(1L, finalActiveOwnerCounts);
  }


  @Test
  @DisplayName("Should prevent concurrent suspension of last active owners")
  void shouldPreventConcurrentSuspensionOfLastActiveOwners(){

    Organization organization = Organization.builder()
            .name("Concurrent Test Organization")
            .slug("status-concurrent-test-org")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    Organization savedOrganization = organizationRepository.save(organization).block();
    assertNotNull(savedOrganization);
    assertNotNull(savedOrganization.getId());

    Instant now = Instant.now();
    User ownerOne = User.builder()
            .organizationId(savedOrganization.getId())
            .email("status-owner1@concurrency-test.com")
            .passwordHash("not-used-in-this-test")
            .firstName("Owner")
            .lastName("One")
            .status(UserStatus.ACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .build();

    User ownerTwo = User.builder()
            .organizationId(savedOrganization.getId())
            .email("status-owner2@concurrency-test.com")
            .passwordHash("not-used-in-this-test")
            .firstName("Owner")
            .lastName("Two")
            .status(UserStatus.ACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .build();

    User savedOwnerOne = userRepository.save(ownerOne).block();
    User savedOwnerTwo = userRepository.save(ownerTwo).block();

    assertNotNull(savedOwnerOne);
    assertNotNull(savedOwnerOne.getId());

    assertNotNull(savedOwnerTwo);
    assertNotNull(savedOwnerTwo.getId());

    //now assign the OWNER role to both users
    userRoleRepository.save(savedOwnerOne.getId(), Role.OWNER).block();
    userRoleRepository.save(savedOwnerTwo.getId(), Role.OWNER).block();

    Long activeOwnerCounts = userRoleRepository.countActiveOwnersByOrganizationId(savedOrganization.getId()).block();
    assertEquals(2L, activeOwnerCounts);

    //suspend both owners concurrently
    Mono<User> suspendOwnerOne = userService.updateUserStatus(savedOwnerOne.getId(), savedOrganization.getId(), UserStatus.SUSPENDED);
    Mono<User> suspendOwnerTwo = userService.updateUserStatus(savedOwnerTwo.getId(), savedOrganization.getId(), UserStatus.SUSPENDED);

    //attempt to suspend the two owners concurrently
    Mono<User> concurrentSuspension = Mono.whenDelayError(
            suspendOwnerOne.subscribeOn(Schedulers.parallel()),
            suspendOwnerTwo.subscribeOn(Schedulers.parallel()))
            .then(Mono.empty());

    StepVerifier.create(concurrentSuspension)
            .expectError(LastActiveOwnerException.class)
            .verify();

    Long finalActiveOwnerCounts = userRoleRepository.countActiveOwnersByOrganizationId(savedOrganization.getId())
            .block();
    assertEquals(1L, finalActiveOwnerCounts);
  }
}
