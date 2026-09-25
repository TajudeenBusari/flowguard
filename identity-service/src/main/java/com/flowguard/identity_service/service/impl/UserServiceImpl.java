package com.flowguard.identity_service.service.impl;

import com.flowguard.identity_service.dto.CreateUserRequest;
import com.flowguard.identity_service.entity.Role;
import com.flowguard.identity_service.entity.User;
import com.flowguard.identity_service.entity.UserStatus;
import com.flowguard.identity_service.exception.OrganizationNotFoundException;
import com.flowguard.identity_service.exception.UserAlreadyExistsException;
import com.flowguard.identity_service.exception.UserNotFoundException;
import com.flowguard.identity_service.repository.OrganizationRepository;
import com.flowguard.identity_service.repository.UserRepository;
import com.flowguard.identity_service.repository.UserRoleRepository;
import com.flowguard.identity_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final OrganizationRepository organizationRepository;
  private final PasswordEncoder passwordEncoder;

  //used to connect role to the user in a transaction, so if the role creation fails, the user creation will be rolled back
  private final TransactionalOperator transactionalOperator;

  private final UserRoleRepository userRoleRepository;

  /**
   * Begin -> verify email isn't already used -> create and insert user -> insert user role: OWNER -> Commit
   * If inserting OWNER fails, the transaction will be rolled back and the user will not be created.
   */
  @Override
  public Mono<User> createUser(UUID organizationId, CreateUserRequest request) {

    String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
    return organizationRepository.findById(organizationId)
            .switchIfEmpty(Mono.error(new OrganizationNotFoundException(organizationId)))
            .flatMap(organization -> userRepository.findByOrganizationIdAndEmail(organizationId, normalizedEmail)
                    .flatMap(existing -> Mono.<User>error(new UserAlreadyExistsException(normalizedEmail)))
                    .switchIfEmpty(Mono.fromCallable(() -> {
                      Instant now = Instant.now();
                      //password encoding moved to BoundedElastic scheduler to avoid blocking the main thread, as password encoding can be CPU intensive
                      return User.builder()
                              .organizationId(organizationId)
                              .email(normalizedEmail)
                              .passwordHash(passwordEncoder.encode(request.password()))
                              .firstName(request.firstName().trim())
                              .lastName(request.lastName().trim())
                              .status(UserStatus.ACTIVE)
                              .createdAt(now)
                              .updatedAt(now)
                              .build();
                    }).subscribeOn(Schedulers.boundedElastic())
                                    .flatMap(userRepository::save)
                                    .flatMap(savedUser -> userRoleRepository.save(savedUser.getId(), Role.MEMBER).thenReturn(savedUser))
                                    .doOnNext(saved ->
                                            log.info("Created user with id {} for organization id {}", saved.getId(), organizationId)))
            ).as(transactionalOperator::transactional);

  }

  @Override
  public Mono<User> getCurrentUser(UUID userId, UUID organizationId) {
    return userRepository.findByIdAndOrganizationId(userId, organizationId)
            .switchIfEmpty(Mono.error(new UserNotFoundException(userId)));
  }

  /*
    * Organization with zero users naturally returns an empty list,
    * which is a valid response. So, no need to throw an exception for that case.
   */
  @Override
  public Flux<User> getUsersByOrganizationId(UUID organizationId) {
    return userRepository.findAllByOrganizationId(organizationId);
  }

}
