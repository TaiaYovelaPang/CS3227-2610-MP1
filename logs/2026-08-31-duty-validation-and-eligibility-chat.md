# Duty Validation and Eligibility Conversation — 31 August 2026

## Duty-name validation

The team manager requested that duty names be unique even when the required number of people differs. The rationale is that a manager should update an existing duty's headcount rather than create a duplicate duty.

The following rules were selected and implemented:

- Duty names are compared after trimming leading and trailing whitespace.
- Capitalisation is ignored, so `Set up`, `SET UP`, and `set up` are duplicates.
- Different spellings remain distinct duties. For example, `Set up` and `Setup` may both be created.
- The rule applies when adding a duty and when renaming an existing duty. Editing a duty without changing its name remains valid.

`DutyValidator` centralises the duplicate-name check and tells the user to update the existing duty's people-needed value instead.

## Duty eligibility by attendance status

The manager then requested that members who arrive late or leave early can be assigned to appropriate duties. For example, a member leaving early can still set up before training, while a late member can assist with a duty later in the session.

The selected design was per-duty eligibility checkboxes:

- **On time**
- **Late**
- **Leaving early**

New duties default to **On time** only, retaining the earlier roster behaviour. At least one checkbox must be selected for every duty; absent members cannot be made eligible.

The duty table displays the selected eligibility statuses. Existing saved duties that predate this feature safely default to on-time eligibility.

## Roster generation

Roster generation now filters candidates according to each duty's selected attendance statuses. It allocates the most restrictive duties first internally, reducing the chance that a flexible duty uses the only eligible member for another duty. The completed roster is still displayed in the manager's configured duty order.

The existing fairness rule remains: eligible members receive one duty before anyone receives a second, unless the required duty slots exceed the number of members eligible for at least one duty.

## Verification

Regression coverage was added for case-insensitive duplicate detection, deliberately distinct spellings, late/early eligibility, and the required-eligibility safeguard.

```bash
./gradlew selfTest
```

The self-test passed after the implementation.
