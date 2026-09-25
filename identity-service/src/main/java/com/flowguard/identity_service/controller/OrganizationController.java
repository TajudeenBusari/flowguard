package com.flowguard.identity_service.controller;

import com.flowguard.identity_service.dto.CreateOrganizationRequest;
import com.flowguard.identity_service.mapper.IdentityMapper;
import com.flowguard.identity_service.service.OrganizationService;
import com.tjtechy.system.Result;
import com.tjtechy.system.StatusCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("${api.endpoint.base-url}/organizations")
@RequiredArgsConstructor
public class OrganizationController {
  private final OrganizationService organizationService;

  @PostMapping
  public Mono<Result> createOrganization(@Valid @RequestBody CreateOrganizationRequest request){

    /*
     * The bootstrapOrganization method is internally calling the createOrganization method and
     * then creating the owner user for that organization, so we can just
     * call bootstrapOrganization here and return the response directly
     * The controller no longer needs to perform the mapping because
     * bootstrapOrganization already returns the OrganizationBootstrapResponse: organization and owner
     */
    return organizationService.bootstrapOrganization(request)
            .map(response ->
                    new Result("Organization created successfully", true, response, StatusCode.CREATED)
            );
  }


}
