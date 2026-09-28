package com.flowguard.identity_service.controller;

import com.flowguard.identity_service.dto.AssignRoleRequest;
import com.flowguard.identity_service.dto.CreateUserRequest;
import com.flowguard.identity_service.dto.UpdateProfileRequest;
import com.flowguard.identity_service.dto.UpdateUserStatusRequest;
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

  @PutMapping("/{userId}/status")
  public Mono<Result> updateUserStatus(@AuthenticationPrincipal Jwt jwt,
                                       @PathVariable UUID userId,
                                       @Valid @RequestBody UpdateUserStatusRequest request) {
      CurrentUser currentUser = CurrentUser.fromJwt(jwt);
      return userService.updateUserStatus(userId, currentUser.organizationId(), request.status())
              .flatMap(user -> userRoleRepository.findAllByUserId(user.getId())
                      .map(UserRole::getRole).collect(Collectors.toSet())
                      .map(roles -> {
                        var dto = IdentityMapper.mapFromUserToUserResponseDto(user, roles);
                        return new Result("User status updated successfully", true, dto, StatusCode.SUCCESS);
                      }));
  }

  /**
   * Update the profile of the currently authenticated user.
   * This endpoint does not accept a userId for security reasons because
   * it ensures that a user can only update their own profile.
   * The id is derived from the verified JWT token, which is a secure way to identify the user making the request.
   * This prevents a user from maliciously or accidentally updating another user's profile.
   * The organizationId is also derived from the JWT to ensure that the user belongs to the correct organization,
   * adding an extra layer of security in a multi-tenant environment.
   * For example if User A from a particular organization is planning to update another User B from same or another organization,
   * the server first checks findByIdAndOrganizationId(userId, organizationId) in the service layer.
   * This is a security measure to ensure that users can only update their own profiles and not those of others, even if they are in the same organization.
   * It enforces strict ownership and access control, which is crucial in a multi-tenant system where users
   * from different organizations may have access to the same application.
   * In summary, this endpoint is designed to be secure and user-specific, ensuring that users can only update their own profiles
   * and not those of others, even if they are in the same organization. The use of JWT for authentication and authorization,
   * along with the checks in the service layer, provides a robust security framework for managing user profiles in a multi-tenant environment.
   */
  @PutMapping("/me/profile")
  public Mono<Result> updateUserProfile(@AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody UpdateProfileRequest request) {
    CurrentUser currentUser = CurrentUser.fromJwt(jwt);
    return userService.updateUserProfile(currentUser.userId(), currentUser.organizationId(), request)
            .flatMap(user -> userRoleRepository.findAllByUserId(user.getId())
                    .map(UserRole::getRole).collect(Collectors.toSet())
                    .map(roles -> {
                      var dto = IdentityMapper.mapFromUserToUserResponseDto(user, roles);
                      return new Result("User profile updated successfully", true, dto, StatusCode.SUCCESS);
                    }));
  }

}
