package com.flowguard.identity_service.service.impl;

import com.flowguard.identity_service.dto.CreateOrganizationRequest;
import com.flowguard.identity_service.dto.OrganizationBootstrapResponse;
import com.flowguard.identity_service.entity.Organization;
import com.flowguard.identity_service.entity.Role;
import com.flowguard.identity_service.entity.User;
import com.flowguard.identity_service.entity.UserStatus;
import com.flowguard.identity_service.exception.OrganizationAlreadyExistException;
import com.flowguard.identity_service.mapper.IdentityMapper;
import com.flowguard.identity_service.repository.OrganizationRepository;
import com.flowguard.identity_service.repository.UserRepository;
import com.flowguard.identity_service.repository.UserRoleRepository;
import com.flowguard.identity_service.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {

  private final OrganizationRepository organizationRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserRoleRepository userRoleRepository;
  private final TransactionalOperator transactionalOperator;


  private Mono<Organization> createOrganization(CreateOrganizationRequest request) {

    String normalizedSlug = normalizedSlug(request.slug());

    log.info("Creating organization with slug: {}", normalizedSlug);

    return organizationRepository.findBySlug(normalizedSlug)

            .flatMap(existing -> {
              log.warn(
                      "Organization already exists with id={} and slug={}",
                      existing.getId(),
                      existing.getSlug()
              );

              return Mono.<Organization>error(
                      new OrganizationAlreadyExistException(normalizedSlug)
              );
            })
            .switchIfEmpty(Mono.defer(() -> {
              Instant now = Instant.now();
              Organization organization = Organization.builder()
                      .name(request.name().trim())
                      .slug(normalizedSlug)
                      .createdAt(now)
                      .updatedAt(now)
                      .build();

              return organizationRepository.save(organization).doOnNext(saved ->
                      log.info("Organization created with id={} and slug={}", saved.getId(), saved.getSlug()));
            }));
  }

  /**
   * WebFlux event loop -> Mono.fromcallable (wraps the synchronous BCrypt password encoding) ->
   * boundedElastic scheduler (offload blocking task) -> save user -> assign role -> map to response DTO
   */
  @Override
  public Mono<OrganizationBootstrapResponse> bootstrapOrganization(CreateOrganizationRequest request) {
    return createOrganization(request)
            .flatMap(organization -> {

              Instant now = Instant.now();
              return Mono.fromCallable(() ->
                              //1. create owner first
                              //we move the password encoding to a boundedElastic scheduler because
                              // it is a synchronous, blocking and CPU intensive operation and can take time.
                              // This prevents blocking the main thread and allows other requests to be processed concurrently.
                              User.builder()
                                      .organizationId(organization.getId())
                                      .email(request.owner().email().trim().toLowerCase(Locale.ROOT))
                                      .passwordHash(passwordEncoder.encode(request.owner().password()))
                                      .firstName(request.owner().firstName().trim())
                                      .lastName(request.owner().lastName().trim())
                                      .status(UserStatus.ACTIVE)
                                      .createdAt(now)
                                      .updatedAt(now)
                                      .build()
                      ).subscribeOn(Schedulers.boundedElastic())
                      .flatMap(userRepository::save)

                      //2. assign role to owner
                      .flatMap(savedOwner ->
                        userRoleRepository.save(savedOwner.getId(), Role.OWNER)
                                .thenReturn(savedOwner))
                      .map(savedOwner -> {
                        var organizationDto = IdentityMapper.mapFromOrganizationToOrganizationResponseDto(organization);
                        var ownerDto = IdentityMapper.mapFromUserToUserResponseDto(savedOwner, Set.of(Role.OWNER));
                        return new OrganizationBootstrapResponse(organizationDto, ownerDto);
                      });


            })
            .as(transactionalOperator::transactional); //keeps all three database operations in one transaction. if one fails, all will be rolled back. This is important to avoid creating an organization without an owner or vice versa.
  }

  @Override
  public Mono<Organization> getOrganizationById(UUID id) {
    return organizationRepository.findById(id);
  }

  @Override
  public Mono<Organization> getOrganizationBySlug(String slug) {
    return organizationRepository.findBySlug(normalizedSlug(slug));
  }

  @Override
  public Flux<Organization> getAllOrganizations() {
    return organizationRepository.findAll();
  }

  private String normalizedSlug(String slug) {
    return slug.trim().toLowerCase(Locale.ROOT);
  }
}
