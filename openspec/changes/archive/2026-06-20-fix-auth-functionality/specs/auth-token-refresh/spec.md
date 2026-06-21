## ADDED Requirements

### Requirement: Access token expiration handling

The admin panel SHALL automatically attempt token refresh when receiving a 401 Unauthorized response, using the stored refresh token.

#### Scenario: Automatic token refresh on 401
- **WHEN** an API request returns 401 and a valid refresh token is stored
- **THEN** the client silently refreshes the token and retries the original request

#### Scenario: Refresh token also expired
- **WHEN** token refresh fails (refresh token expired or invalid)
- **THEN** the client redirects to login page with appropriate message

### Requirement: Refresh token issuance

The backend SHALL issue a refresh token alongside the access token during login, with a longer expiry period (7 days).

#### Scenario: Login returns refresh token
- **WHEN** user successfully logs in via `/auth/login`
- **THEN** response includes `refresh_token` field with 7-day expiry JWT

#### Scenario: Refresh token reuse prevention
- **WHEN** a refresh token is used successfully
- **THEN** the server SHALL issue a new refresh token and the previous one SHALL be invalidated
