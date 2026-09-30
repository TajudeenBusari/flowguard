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