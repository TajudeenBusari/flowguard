package com.flowguard.identity_service.service;

import com.flowguard.identity_service.dto.CreateOrganizationRequest;
import com.flowguard.identity_service.dto.OrganizationBootstrapResponse;
import com.flowguard.identity_service.entity.Organization;
import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface OrganizationService {
  Mono<OrganizationBootstrapResponse> bootstrapOrganization(@Valid CreateOrganizationRequest request);
  Mono<Organization> getOrganizationById(UUID id);
  Mono<Organization> getOrganizationBySlug(String slug);
  Flux<Organization> getAllOrganizations();
}
