## ADDED Requirements

### Requirement: Public health check endpoint
The system SHALL expose a public health check endpoint that does not require authentication and can be used by load balancers and monitoring tools.

#### Scenario: Anonymous health check
- **WHEN** an unauthenticated client sends GET request to `/api/v1/utils/health-check/`
- **THEN** the system returns HTTP 200 with JSON body `true`

#### Scenario: Authenticated health check
- **WHEN** an authenticated client sends GET request to `/api/v1/utils/health-check/`
- **THEN** the system returns HTTP 200 with JSON body `true`

### Requirement: Health check endpoint does not modify state
The system SHALL ensure the health check endpoint is read-only and has no side effects.

#### Scenario: Multiple health checks are idempotent
- **WHEN** the health check endpoint is called multiple times in succession
- **THEN** all responses are identical and no database or application state is modified
