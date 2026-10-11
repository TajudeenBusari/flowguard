package com.flowguard.identity_service.mapper;

import com.flowguard.identity_service.dto.OrganizationResponseDto;
import com.flowguard.identity_service.dto.UserResponseDto;
import com.flowguard.identity_service.entity.Organization;
import com.flowguard.identity_service.entity.Role;
import com.flowguard.identity_service.entity.User;

import java.util.Set;

//final → don't extend this utility class.
public final class IdentityMapper {

  //private IdentityMapper() → don't instantiate it.
  private IdentityMapper() {}

  public static OrganizationResponseDto mapFromOrganizationToOrganizationResponseDto(Organization organization) {
    return new OrganizationResponseDto(
            organization.getId(),
            organization.getName(),
            organization.getCreatedAt()
    );
  }

  public static UserResponseDto mapFromUserToUserResponseDto(User user, Set<Role> roles) {
    return new UserResponseDto(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getStatus(),
            user.getCreatedAt(),
            roles

    );
  }
}
