## ADDED Requirements

### Requirement: Partner info row on bind
When two users bind as partners, the system SHALL create one `partner_info` row for the ordered pair, with `partner_since` equal to the server's current local date and `points_balance` equal to 0, while retaining bidirectional `partner_id` on both users.

#### Scenario: Bind creates partner_info
- **WHEN** user A successfully binds with user B via `POST /identity/bind-partner`
- **THEN** a `partner_info` row SHALL exist for the sorted pair (A, B)
- **AND** both users SHALL have each other's `partner_id`
- **AND** `partner_since` SHALL be the bind-day date and `points_balance` SHALL be 0

### Requirement: User can update partner since date
The system SHALL allow a bound user to update the shared `partner_since` on the couple's `partner_info` without partner confirmation.

#### Scenario: Successful update
- **WHEN** a bound user sends `PUT /identity/partner-since` with a date on or before today
- **THEN** that couple's `partner_info.partner_since` SHALL be updated
- **AND** the response SHALL include the caller's updated `partnerSince`

#### Scenario: Update without partner
- **WHEN** a user without a bound partner sends `PUT /identity/partner-since`
- **THEN** the system SHALL respond with 400

#### Scenario: Future date rejected
- **WHEN** a bound user sends a `partnerSince` after today
- **THEN** the system SHALL respond with 400 and leave `partner_since` unchanged

### Requirement: Unbind and account delete remove partner_info
Unbinding or deleting an account with a partner SHALL remove the couple's `partner_info`, clear `partner_id` as today, and delete both users' `partner_points` ledger rows.

#### Scenario: Unbind clears relationship assets
- **WHEN** user A unbinds from user B
- **THEN** `partner_info` for (A, B) SHALL be deleted
- **AND** both `partner_id` values SHALL be null
- **AND** `partner_points` rows with `created_by` in (A, B) SHALL be deleted

#### Scenario: Account delete cleans partner_info
- **WHEN** user A (partner of B) deletes their account
- **THEN** B's `partner_id` SHALL be null and the couple's `partner_info` SHALL be deleted

### Requirement: Points balance cache
The system SHALL expose partner points balance from `partner_info.points_balance` and keep it consistent with new ledger writes.

#### Scenario: Balance read from cache
- **WHEN** a bound user requests points balance
- **THEN** the value SHALL come from `partner_info.points_balance` (not a fresh SUM of all history at read time)

#### Scenario: Add points updates cache
- **WHEN** a bound user adds a points change of N
- **THEN** a `partner_points` row SHALL be inserted
- **AND** `points_balance` SHALL increase by N in the same transaction

### Requirement: Days together uses partner since
The partner dashboard SHALL compute「在一起」days as calendar days from `partner_since` to today inclusive (today with since=today yields 1).

#### Scenario: Inclusive day count
- **WHEN** `partnerSince` is set
- **THEN** days together SHALL equal `(today - partnerSince).days + 1`

#### Scenario: Missing partner_since
- **WHEN** `partnerSince` is null
- **THEN** days together SHALL display as 0

### Requirement: UI to edit together-since date
The dashboard anniversary card and the partner detail page SHALL allow editing the together-since date via `PUT /identity/partner-since`.

#### Scenario: Edit from dashboard and detail
- **WHEN** a bound user saves a new date from either entry point
- **THEN** the client SHALL call `PUT /identity/partner-since` and refresh user info so both surfaces show the updated days

### Requirement: Migration backfill for existing couples
The schema migration SHALL create `partner_info` for each bidirectional partner pair with `partner_since` equal to the earlier of the two users' `created_at` dates and `points_balance` equal to the SUM of their existing points ledger.

#### Scenario: Existing pair backfilled
- **WHEN** migration runs for users A and B who already reference each other via `partner_id`
- **THEN** a `partner_info` row SHALL exist with `partner_since = DATE(LEAST(A.created_at, B.created_at))`
- **AND** `points_balance` SHALL equal `COALESCE(SUM(points_change) for created_by in (A,B), 0)`
