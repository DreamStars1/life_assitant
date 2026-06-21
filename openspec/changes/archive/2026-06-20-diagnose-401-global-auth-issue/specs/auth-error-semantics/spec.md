## ADDED Requirements

### Requirement: JWT validation errors return 401
The system SHALL return HTTP 401 Unauthorized for all authentication failures, including missing token, invalid token signature, expired token, and malformed token claims.

#### Scenario: Missing Authorization header
- **WHEN** a request to a protected endpoint has no `Authorization` header
- **THEN** the system returns HTTP 401 with JSON body `{"detail": "Not authenticated"}`

#### Scenario: Invalid JWT signature
- **WHEN** a request includes an `Authorization: Bearer <token>` header with a token signed with a wrong secret
- **THEN** the system returns HTTP 401 with JSON body `{"detail": "Could not validate credentials"}`

#### Scenario: Expired JWT token
- **WHEN** a request includes an `Authorization: Bearer <token>` header with an expired token
- **THEN** the system returns HTTP 401 with JSON body `{"detail": "Could not validate credentials"}`

#### Scenario: Malformed JWT claims
- **WHEN** a request includes a validly-signed JWT whose `sub` claim is missing or not a valid UUID
- **THEN** the system returns HTTP 401 with JSON body `{"detail": "Could not validate credentials"}`

### Requirement: Superuser privilege check returns 403
The system SHALL return HTTP 403 Forbidden when an authenticated user lacks superuser privileges for protected endpoints.

#### Scenario: Non-superuser accesses admin endpoint
- **WHEN** an authenticated non-superuser requests an endpoint protected by `get_current_active_superuser`
- **THEN** the system returns HTTP 403 with JSON body `{"detail": "The user doesn't have enough privileges"}`

### Requirement: Inactive user returns 400
The system SHALL return HTTP 400 Bad Request when an authenticated user's account is disabled.

#### Scenario: Inactive user accesses protected endpoint
- **WHEN** an authenticated user whose `is_active` is `False` requests a protected endpoint
- **THEN** the system returns HTTP 400 with JSON body `{"detail": "Inactive user"}`
