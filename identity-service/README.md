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