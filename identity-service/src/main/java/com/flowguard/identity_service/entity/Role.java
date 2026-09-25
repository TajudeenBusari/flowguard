package com.flowguard.identity_service.entity;

/**
 * Enum representing the roles of a user within an organization.
 * OWNER: organization owner, highest tenant-level access
 * ADMIN: organization administrator, can manage users and settings
 * MEMBER: regular organization member, limited access
 * A user may need more than one role so we will create a separate table to
 * store the roles of a user within an organization.
 */
public enum Role {
  OWNER,
  ADMIN,
  MEMBER
}
