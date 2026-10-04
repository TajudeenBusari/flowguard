#### Test Containers will be used for the integration tests
    Dependencies:
        1. Docker
        2. Testcontainers library (for Java)

#### Token versioning:
    - We will implement token version so security-sensitive account changes (like password change, email change, etc.) will invalidate all previously issued tokens.
    - We will create a new Flyway migration to add another column to the user table called token_version (default value = 0).
    - The idea is simple: each JWT will eventually carry the user's current tokenVersion. When we need to revoke all existing tokens for that user, we will increment the database value.
    - New database users will start at token_version = 0.

                    ┌──────────────────────┐
                    │   identity-service   │
                    │                      │
    Login ────────────►│ Authenticate user    │
    │ Issue access token   │
    │ Issue refresh token  │
    └──────────┬───────────┘
    │
    ┌─────────┴─────────┐
    │                   │
    ▼                   ▼
    Access Token         Refresh Session
    JWT / RSA            Server-side state
    ~15 minutes          Revocable
    │
    ▼
    ┌───────────────────────────┐
    │ FlowGuard services        │
    │                           │
    │ workflow-service          │
    │ orchestrator-service      │
    │ approval-service          │
    │ agent-service             │
    │ tool-service              │
    └───────────────────────────┘
    │
    ▼
    RSA public key
    local validation
    NO database call
______________________________________________________________________
    Login
        ↓
    Access JWT ──────────────► 15 minutes
    Refresh token ───────────► longer-lived, server controlled
        
    Access JWT expires
        ↓
    Client sends refresh token
        ↓
    identity-service validates refresh session
        ↓
    new Access JWT

#### Refresh token revocation:
    - When a user logs out, we will delete the refresh session from the database.
    - When a user changes their password, we will delete all refresh sessions for that user from the database.
    - When a user changes their email, we will delete all refresh sessions for that user from the database.
    - When a user is deleted, we will delete all refresh sessions for that user from the database.

#### Refresh token rotation
    Refresh token A
      ↓
    POST /auth/refresh
    ↓
    validate A
    ↓
    revoke A
    ↓
    create refresh token B
    ↓
    new access JWT + B
    The same refresh token can be reused for 7 days. Each successful refresh should invalidate the old refresh token and issue a new one. 
    This prevents replay attacks and ensures that if a refresh token is compromised, it can only be used once before being revoked.

#### Flow:
    users
    → no token-version state

    Access JWT
        → 15 minutes
        → stateless/local RSA validation

    refresh_sessions
        → server-side
        → hashed
        → revocable
        → rotated

#### Revocation will be connected to password change and other security-sensitive changes.
    - **Test change password scenario**:
    1. Log in and save the returned refresh token as Refresh A.
    2. User changes their password using the access token from that login.
    3. After the password change succeeds, try: /refresh with Refresh A. It should fail because the refresh session was revoked when the password was changed.
    4. This will prove:
          Login → Refresh A active
             ↓
        Change password
             ↓
       Refresh A revoked
             ↓
       Try Refresh A
             ↓
           401
    5. prevent suspended /disabled users from refreshing their access tokens.

    - **Test the inactive user scenario**:
    1. Log in as ACTIVE user and save the returned refresh token as Refresh A.
    2. Suspend the user in the database using the OWNER account token.
    3. Try: /refresh with Refresh A. It should fail because the user is suspended with 401 Unauthorized.

#### docker exec -it flowguard-identity-postgres psql -U flowguard -d flowguard_identity

#### When role changes are made refresh sessions should be revoked. 
#### This is to ensure that users with elevated privileges cannot continue to use their old refresh tokens after their roles have been downgraded.
    - **Test role downgrade scenario**:
    1. Log in as a user with elevated privileges (e.g., ADMIN or MEMBER) and save the returned refresh token as Refresh A.
    2. Change the user's role to a lower privilege (e.g., USER) using the OWNER account token.
    3. Try: /refresh with Refresh A. It should fail because the user's role has been downgraded and the refresh session was revoked.

#### Role Assignments and Revocation:
    - When a user's role is changed (either upgraded or downgraded), all existing refresh sessions for that user will be revoked. 
      This ensures that any tokens issued under the previous role are no longer valid.
    - This is particularly important for security-sensitive operations where a user's permissions may change, 
      and we want to ensure that they cannot continue to access resources with their old privileges.
    - **Test role upgrade scenario**:
    1. Log in as a user with lower privileges (e.g., USER) and save the returned refresh token as Refresh A.
    2. Change the user's role to a higher privilege (e.g., ADMIN) using the OWNER account token.
    3. Try: /refresh with Refresh A. It should not succeed because the user's role has been upgraded and the refresh session was revoked. 
       This ensures that the user must log in again to obtain a new refresh token that reflects their new privileges.

#### Concurrent refresh token reuse:
    - If a user attempts to use the same refresh token concurrently from multiple clients, only the first request should succeed.
        Request 1 ── validate A ✓ ── revoke A ── create B
        Request 2 ── validate A ✓ ── revoke A ── create C
        This could leave both B and C valid.
    Subsequent requests using the same refresh token should fail, as the token would have been revoked after the first successful use.
    - **Test concurrent refresh token reuse scenario**:
    1. Log in and save the returned refresh token as Refresh A.
    2. Simulate two concurrent requests to /refresh using Refresh A.
    3. The first request should succeed and return a new access JWT and a new refresh token (Refresh B).
    4. The second request should fail with a 401 Unauthorized response, as Refresh A has been revoked after the first request.

#### Logout should be made to be idempotent (producing the same result even if called multiple times):
    - If a user logs out multiple times, the system should handle it gracefully without throwing errors.

#### you can decode the JWT Token in the PowerShell without posting the token anywhere 
    1. first assign the token variable $token = "YOUR_ACCESS_TOKEN"
    2. then run: $header=$token.Split('.')[0]; $header += "=" * ((4 - $header.Length % 4) % 4); [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($header.Replace('-', '+').Replace('_', '/')))
   

#### Kid must be the same in the Signing JWK, JWT header, and Public JWKS.
    Signing JWK     kid = flowguard-identity-key
    ↓
    JWT header      kid = flowguard-identity-key
    ↓
    Public JWKS     kid = flowguard-identity-key

