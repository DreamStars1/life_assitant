## ADDED Requirements

### Requirement: Partner info stores shared relationship attributes
The system SHALL persist one `partner_info` row per bound couple, identified by lexicographically ordered `(user_a_id, user_b_id)`, holding `partner_since` and `points_balance`.

#### Scenario: Ordered pair uniqueness
- **WHEN** a partner_info row is created for users A and B
- **THEN** `user_a_id` SHALL be the lexicographically smaller id and `user_b_id` the larger
- **AND** a second row for the same unordered pair SHALL be rejected by uniqueness

### Requirement: Lookup partner_info by either user
Given a bound user, the system SHALL resolve the couple's `partner_info` using that user and their `partner_id`.

#### Scenario: Resolve by member
- **WHEN** user A has `partner_id = B`
- **THEN** loading A's partner attributes SHALL return the single `partner_info` for sorted(A, B)
