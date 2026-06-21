## ADDED Requirements

### Requirement: User can unbind partner
The system SHALL allow a user who has a bound partner to unbind the relationship, clearing `partner_id` for both parties.

#### Scenario: Successful unbind
- **WHEN** user A (with partner B) sends `POST /identity/unbind-partner`
- **THEN** `partner_id` for both user A and user B SHALL be set to `null` in a single transaction

#### Scenario: Unbind without existing partner
- **WHEN** user without a bound partner sends `POST /identity/unbind-partner`
- **THEN** system SHALL respond with 400 and message "尚未绑定伴侣"

### Requirement: Shared records deleted upon unbind
Upon unbinding, all shared records created by either partner during the relationship SHALL be deleted.

#### Scenario: All shared records deleted after unbind
- **WHEN** user A unbinds from user B
- **THEN** all records WHERE `created_by IN (A, B)` SHALL be deleted in the same transaction

### Requirement: User deletion cleans up partner relationship
When a user deletes their account, the system SHALL clear the `partner_id` of their bound partner.

#### Scenario: Deleted user's partner gets cleaned up
- **WHEN** user A (partner of user B) sends `DELETE /users/me`
- **THEN** user B's `partner_id` SHALL be set to `null`
