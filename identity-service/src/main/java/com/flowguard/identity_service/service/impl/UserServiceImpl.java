package com.flowguard.identity_service.service.impl;

import com.flowguard.identity_service.dto.ChangeEmailRequest;
import com.flowguard.identity_service.dto.ChangePasswordRequest;
import com.flowguard.identity_service.dto.CreateUserRequest;
import com.flowguard.identity_service.dto.UpdateProfileRequest;
import com.flowguard.identity_service.entity.Role;
import com.flowguard.identity_service.entity.User;
import com.flowguard.identity_service.entity.UserStatus;
import com.flowguard.identity_service.exception.*;
import com.flowguard.identity_service.repository.OrganizationRepository;
import com.flowguard.identity_service.repository.UserRepository;
import com.flowguard.identity_service.repository.UserRoleRepository;
import com.flowguard.identity_service.service.RefreshTokenService;
import com.flowguard.identity_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
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

  private final RefreshTokenService refreshTokenService;

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

  /**
    * Organization with zero users naturally returns an empty list,
    * which is a valid response. So, no need to throw an exception for that case.
   */
  @Override
  public Flux<User> getUsersByOrganizationId(UUID organizationId) {
    return userRepository.findAllByOrganizationId(organizationId);
  }

  /**
   * This prevents an OWNER from organization A from assigning roles to a user in organization B.
   * For FlowGuard, OWNER is special: it represents organization ownership, not an ordinary role promotion
   * So this operation is only allowed for OWNERs to assign MEMBER or ADMIN roles to users within the same organization.
   * We also make the role assignment idempotent: if the user already has the role, we just return the user without error.
   * Assign role
   *     +
   * Revoke refresh sessions
   *     ↓
   * one transaction
   *     ↓
   * both succeed or both roll back
   */
  @Override
  public Mono<User> assignRoleToUser(UUID userId, UUID organizationId, Role role) {

    return userRepository.findByIdAndOrganizationId(userId, organizationId)
            .switchIfEmpty(Mono.error(new UserNotFoundException(userId)))
            .flatMap(user ->
                    userRoleRepository.existsByUserIdAndRole(user.getId(), role)
                            .flatMap(exists ->{
                              if (exists) {
                                return Mono.just(user);
                              }
                              //else, if the user doesn't have the role, we can safely assign the role and
                              // revoke all refresh sessions for this user to force re-login with new role
                              return userRoleRepository
                                      .save(user.getId(), role)
                                      .then(refreshTokenService.revokeAllRefreshSessionsForUser(user.getId()))
                                      .thenReturn(user);
                            })
            ).as(transactionalOperator::transactional);
  }

  /**
   * This prevents an OWNER from organization A from removing roles from a user in organization B.
   * For FlowGuard, OWNER is special: it represents organization ownership, not an ordinary role demotion
   * So this operation is only allowed for OWNERs to remove MEMBER or ADMIN roles from users within the same organization.
   * We also make the role removal idempotent: if the user doesn't have the role, we just return the user without error.
   * begin transaction
   *       ↓
   * lock organization row
   *       ↓
   * count ACTIVE OWNERs
   *       ↓
   * validate invariant
   *       ↓
   * remove OWNER role
   *       ↓
   * revoke refresh tokens
   * commit → release lock
   * The important concurrency protection is the combination of:
   * 1. locking the organization row for update (organizationRepository.findByIdForUpdate(organizationId))
   * 2. as(transactionalOperator::transactional) to ensure the lock is held for the duration of the transaction
   */
  @Override
  public Mono<User> removeRoleFromUser(UUID userId, UUID organizationId, Role role) {

    return userRepository.findByIdAndOrganizationId(userId, organizationId)

            .switchIfEmpty(Mono.error(new UserNotFoundException(userId)))

            .flatMap(user -> {
              if(role == Role.OWNER && user.getStatus() == UserStatus.ACTIVE){
                return organizationRepository.findByIdForUpdate(organizationId)

                        .switchIfEmpty(Mono.error(new OrganizationNotFoundException(organizationId)))

                        .then(userRoleRepository.countActiveOwnersByOrganizationId(organizationId))

                        .flatMap(activeOwnerCount -> {
                          if (activeOwnerCount <=1){
                            log.info("Active OWNER count for organization {} is {}, cannot remove the last active OWNER", organizationId, activeOwnerCount);
                            return Mono.error(new LastActiveOwnerException());
                          }
                          // else, if there are more than 1 active OWNERs, we can safely remove this OWNER role
                          //return userRoleRepository.deleteByUserIdAndRole(user.getId(), role)
                          return removeRoleAndHandleRefreshTokenRevocation(user, role);
                        });

              }
              // else, if the role is not OWNER or the user is not active, we can safely remove the role without any additional checks
              //OWNER can also be removed directly when the user is
              //already SUSPENDED or DISABLED because that user is
              //not counted as an ACTIVE OWNER
              //return userRoleRepository.deleteByUserIdAndRole(user.getId(), role).thenReturn(user);
              return removeRoleAndHandleRefreshTokenRevocation(user, role);
            }).as(transactionalOperator::transactional);
  }

  /**
   * This helper method will prevent writing duplicate code
   * after role is removed (method called twice in removeRoleFromUser method)
   */
  private Mono<User> removeRoleAndHandleRefreshTokenRevocation(User user, Role role) {
    return userRoleRepository.deleteByUserIdAndRole(user.getId(), role)
            .then(refreshTokenService.revokeAllRefreshSessionsForUser(user.getId()))
            .thenReturn(user);
  }

  /**
   *  passing organizationId ensures the operation is tenant scoped,
   *  so an OWNER from organization A cannot change the status of a user in organization B.
   *  Just like role assignment and removal, this operation is also idempotent:
   *  if the user already has the requested status, we just return the user without error.
   *  2 active OWNERs
   * → suspend one OWNER
   * → allowed
   * 1 active OWNER
   * → suspend that OWNER
   * → rejected
   * This: user.getStatus() == UserStatus.ACTIVE is because we only need the active count protection
   * when transitioning from ACTIVE to SUSPENDED or DISABLED. If the user is already SUSPENDED or DISABLED, we don't need to check the active count.
   * lock organization
   *        ↓
   * count active OWNERs
   *        ↓
   * validate invariant
   *        ↓
   * change status
   *        ↓
   * commit + release lock
   */
  @Override
  public Mono<User> updateUserStatus(UUID userId, UUID organizationId, UserStatus status) {

    return userRepository.findByIdAndOrganizationId(userId, organizationId)

            .switchIfEmpty(Mono.error(new UserNotFoundException(userId)))

            .flatMap(user ->

              userRoleRepository.existsByUserIdAndRole(user.getId(), Role.OWNER)

                      .flatMap(isOwner -> {

                        if (isOwner && user.getStatus() == UserStatus.ACTIVE && status != UserStatus.ACTIVE) {
                          return organizationRepository.findByIdForUpdate(organizationId)
                                  .switchIfEmpty(Mono.error(new OrganizationNotFoundException(organizationId)))
                                  .then(userRoleRepository.countActiveOwnersByOrganizationId(organizationId))

                                  .flatMap(activeOwnerCount -> {

                                    if (activeOwnerCount <= 1){
                                      log.info("Active OWNER count for organization {} is {}, cannot suspend or disable the last active OWNER", organizationId, activeOwnerCount);
                                      return Mono.error(new LastActiveOwnerException());
                                    }
                                    // else, if there are more than 1 active OWNERs, we can safely suspend this OWNER
                                    return saveStatusAndHandleRefreshTokenRevocation(user, status);
                                  });
                        }
                        // else, if the user is not an OWNER, we can safely update the status without any additional checks
                        return saveStatusAndHandleRefreshTokenRevocation(user, status);
                      })
            ).as(transactionalOperator::transactional);
  }

  /**
   * This helper method will prevent writing duplicate code
   * after user is saved (method called twice in updateUserStatus method)
   */
  private Mono<User> saveStatusAndHandleRefreshTokenRevocation(User user, UserStatus status) {
    user.setStatus(status);
    user.setUpdatedAt(Instant.now());
    return userRepository
            .save(user)
            .flatMap(savedUser -> {
              if (status == UserStatus.ACTIVE) {
                return Mono.just(savedUser);
              }
              // If the user is being suspended or disabled, revoke all refresh sessions for this user to force re-login
              return refreshTokenService
                      .revokeAllRefreshSessionsForUser(savedUser.getId())
                      .thenReturn(savedUser);
            });
  }

  /**
   *  Passing organizationId and userId derived from the JWT ensures the operation is tenant scoped,
   *  So an OWNER from organization A cannot change the profile of a user in organization B.
   *  Or different owners in the same organization cannot change each other's profile.
   *  This operation is idempotent: if the user already has the requested profile, we just return the user without error.
   */
  @Override
  public Mono<User> updateUserProfile(UUID userId, UUID organizationId, UpdateProfileRequest request) {

    return userRepository.findByIdAndOrganizationId(userId, organizationId)

            .switchIfEmpty(Mono.error(new UserNotFoundException(userId)))

            .flatMap(user -> {
              user.setFirstName(request.firstName().trim());
              user.setLastName(request.lastName().trim());
              user.setUpdatedAt(Instant.now());
              return userRepository.save(user);
            });
  }

  @Override
  public Mono<Void> changePassword(UUID userId, UUID organizationId, ChangePasswordRequest request) {
    return userRepository.findByIdAndOrganizationId(userId, organizationId)
            .switchIfEmpty(Mono.error(new UserNotFoundException(userId)))
            .flatMap(user ->
                    Mono.fromCallable(() -> {
                      if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())){
                        throw new IncorrectCurrentPasswordException();
                      }
                      if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())){
                        throw new PasswordUnchangedException();
                      }
                      return passwordEncoder.encode(request.newPassword());
                    })
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(encodedPassword -> {
              user.setPasswordHash(encodedPassword);
              user.setUpdatedAt(Instant.now());
              return userRepository.save(user)
                      //revoke all refresh sessions for this user after password change, to force re-login with new password
                      .flatMap(savedUser -> refreshTokenService
                              .revokeAllRefreshSessionsForUser(savedUser.getId())
                              .thenReturn(savedUser));

            })).then();
  }

  /**
   * Change-email flow:
   * Find user within organization
   *        ↓
   * Verify current password
   *        ↓
   * Is requested email the same as current email?
   *        ↓ yes
   * Return user unchanged
   *        ↓ no
   * Check whether requested email already exists
   * within the organization
   *        ↓ exists
   * Throw EmailAlreadyExistsException
   *        ↓ available
   * Update email and return user
   */
  @Override
  public Mono<User> changeEmail(UUID userId, UUID organizationId, ChangeEmailRequest request) {

    String normalizedEmail = request.newEmail().trim().toLowerCase(Locale.ROOT);
    return userRepository.findByIdAndOrganizationId(userId, organizationId)
            .switchIfEmpty(Mono.error(new UserNotFoundException(userId)))
            .flatMap(user -> Mono.fromCallable(() -> {
              if(!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())){
                throw new IncorrectCurrentPasswordException();
              }
              return user;

            }).subscribeOn(Schedulers.boundedElastic())
            ).flatMap(user -> {
              //if email is the same as current email, we just return the user without error, no need for database update when nothing has changed
              if(user.getEmail().equalsIgnoreCase(normalizedEmail)){
                return Mono.just(user);
              }
              // else, if the email is different, we need to check whether the new email already exists within the organization
              return userRepository.existsByOrganizationIdAndEmail(organizationId, normalizedEmail)
                      .flatMap(emailExists -> {
                        if (emailExists) {
                          return Mono.error(new EmailAlreadyExistsException(normalizedEmail));
                        }
                        // else, if the email is available, we can safely update the user's email
                        user.setEmail(normalizedEmail);
                        user.setUpdatedAt(Instant.now());
                        return userRepository.save(user);
                      });

            });
  }

}
