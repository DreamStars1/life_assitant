## ADDED Requirements

### Requirement: Login endpoint for admin panel

The backend SHALL provide a `POST /api/v1/auth/login` endpoint that accepts JSON body `{username, password}` and returns `{access_token, token_type, refresh_token, expires_in}`.

#### Scenario: Successful admin login
- **WHEN** client sends `POST /api/v1/auth/login` with valid `{username: "admin@example.com", password: "changethis"}`
- **THEN** server returns 200 with `access_token`, `refresh_token`, `token_type: "bearer"`, `expires_in: 7200`

#### Scenario: Invalid credentials
- **WHEN** client sends `POST /api/v1/auth/login` with wrong password
- **THEN** server returns 400 with `{"detail": "Incorrect email or password"}`

### Requirement: Token refresh endpoint

The backend SHALL provide a `POST /api/v1/auth/refresh-token` endpoint that accepts a valid refresh token and returns a new access token pair.

#### Scenario: Successful token refresh
- **WHEN** client sends `POST /api/v1/auth/refresh-token` with valid `refreshToken` query parameter
- **THEN** server returns 200 with new `access_token` and `refresh_token`

#### Scenario: Expired refresh token
- **WHEN** client sends expired or invalid refresh token
- **THEN** server returns 403 with error detail

### Requirement: Logout endpoint

The backend SHALL provide a `DELETE /api/v1/auth/logout` endpoint that accepts the current access token and returns success.

#### Scenario: Successful logout
- **WHEN** authenticated client sends `DELETE /api/v1/auth/logout`
- **THEN** server returns 200 with success message

### Requirement: Standard Bearer token for mobile client

The mobile client SHALL send authentication token using the standard `Authorization: Bearer <token>` header instead of custom `Access-Token` header.

#### Scenario: Authenticated mobile request
- **WHEN** mobile client makes API request with stored token
- **THEN** request includes header `Authorization: Bearer <token>`

### Requirement: Registration endpoint correction in mobile client

The mobile client's `api/user.ts` register function SHALL call `POST /users/signup` instead of `POST /users/register`.

#### Scenario: User registration from mobile
- **WHEN** user fills registration form and submits
- **THEN** client sends `POST /api/v1/users/signup` with `{email, password, full_name}`

### Requirement: Admin panel response handling

The admin panel's request interceptor SHALL handle FastAPI direct JSON responses without expecting `{code, data, msg}` wrapper format.

#### Scenario: Successful API response
- **WHEN** server returns 200 with `{access_token, token_type}`
- **THEN** admin panel correctly extracts `access_token` without wrapping errors

#### Scenario: HTTP error response
- **WHEN** server returns 400 with `{detail: "..."}`
- **THEN** admin panel displays the error detail to the user
