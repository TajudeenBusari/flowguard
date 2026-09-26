package com.flowguard.identity_service.controller;

import com.flowguard.identity_service.dto.AssignRoleRequest;
import com.flowguard.identity_service.dto.CreateUserRequest;
import com.flowguard.identity_service.entity.Role;
import com.flowguard.identity_service.entity.UserRole;
import com.flowguard.identity_service.mapper.IdentityMapper;
import com.flowguard.identity_service.repository.UserRoleRepository;
import com.flowguard.identity_service.security.CurrentUser;
import com.flowguard.identity_service.service.UserService;
import com.tjtechy.system.Result;
import com.tjtechy.system.StatusCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${api.endpoint.base-url}/organizations/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final UserRoleRepository userRoleRepository;

  @PostMapping
  public Mono<Result> createUser(@AuthenticationPrincipal Jwt jwt,
                                 @Valid @RequestBody CreateUserRequest request){
    CurrentUser currentUser = CurrentUser.fromJwt(jwt);
    UUID authenticatedOrganizationId = currentUser.organizationId();
    return userService.createUser(authenticatedOrganizationId, request)
            .flatMap(user -> userRoleRepository.findAllByUserId(user.getId())
                    .map(UserRole::getRole).collect(Collectors.toSet())
            .map(roles -> {
              var dto = IdentityMapper.mapFromUserToUserResponseDto(user, roles);
              return new Result("User created successfully", true, dto, StatusCode.CREATED);
            }));
  }

  @GetMapping("/me")
  public Mono<Result> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
    CurrentUser currentUser = CurrentUser.fromJwt(jwt);
    return userService.getCurrentUser(currentUser.userId(), currentUser.organizationId())
            .flatMap(user -> userRoleRepository.findAllByUserId(user.getId())
                    .map(UserRole::getRole).collect(Collectors.toSet())
                    .map(roles -> {
                      var dto = IdentityMapper.mapFromUserToUserResponseDto(user, roles);
                      return new Result("Current user retrieved successfully", true, dto, StatusCode.SUCCESS);
                    }));
  }

  @GetMapping
  public Mono<Result> getUsersByOrganizationId(@AuthenticationPrincipal Jwt jwt) {
    CurrentUser currentUser = CurrentUser.fromJwt(jwt);
    return userService.getUsersByOrganizationId(currentUser.organizationId())
            .flatMap(user -> userRoleRepository.findAllByUserId(user.getId())
                    .map(UserRole::getRole).collect(Collectors.toSet())
                    .map( roles ->
                            IdentityMapper.mapFromUserToUserResponseDto(user, roles)))
            .collectList()
            .map(dtos ->
                    new Result("Users retrieved successfully", true, dtos, StatusCode.SUCCESS));
  }

  @PutMapping("/{userId}/roles")
  public Mono<Result> assignUserRoles(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable UUID userId,
                                     @Valid @RequestBody AssignRoleRequest request) {
    CurrentUser currentUser = CurrentUser.fromJwt(jwt);
    return userService.assignRoleToUser(userId, currentUser.organizationId(), request.role())
            .flatMap(user -> userRoleRepository.findAllByUserId(user.getId())
                    .map(UserRole::getRole).collect(Collectors.toSet())
                    .map(roles -> {
                      var dto = IdentityMapper.mapFromUserToUserResponseDto(user, roles);
                      return new Result("Roles assigned successfully", true, dto, StatusCode.SUCCESS);
                    }));

  }

  @DeleteMapping("/{userId}/roles/{role}")
  public Mono<Result> removeUserRoles(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable UUID userId,
                                      @PathVariable Role role) {
    CurrentUser currentUser = CurrentUser.fromJwt(jwt);
    return userService.removeRoleFromUser(userId, currentUser.organizationId(), role)
            .flatMap(user -> userRoleRepository.findAllByUserId(user.getId())
                    .map(UserRole::getRole).collect(Collectors.toSet())
                    .map(roles -> {
                      var dto = IdentityMapper.mapFromUserToUserResponseDto(user, roles);
                      return new Result("Roles removed successfully", true, dto, StatusCode.SUCCESS);
                    }));
  }
}
