# Roster History Conversation — 29 August 2026

## Initial request

The team manager asked for a roster-history section in the Roster tab that makes duty allocations more transparent and accountable, and can be exported.

Three representations were considered:

1. A chronological activity feed.
2. A session-based history table.
3. A member-by-duty allocation matrix.

The initial recommendation was a date-grouped table with supporting audit events, but the manager clarified that acknowledgement and completion information was out of scope. The feature only needed to show which members were assigned to duties over a month.

## Revised alternatives

The following monthly duty-allocation views were presented:

1. **Monthly allocation table** — one row per date and duty, with assigned members and unfilled slots.
2. **Monthly calendar view** — one card or calendar cell per session date.
3. **Member-by-duty allocation matrix** — rows for members, columns for duties, and dates/counts in each cell.

The manager selected the member-by-duty allocation matrix because the roster's primary purpose is member-to-member fairness, not reviewing a full roster for a single date.

## Implemented outcome

The feature was implemented on the existing `Roster` branch.

- `MonthlyRosterHistory` derives a selected month's member-by-duty allocation data from the persisted roster-assignment history.
- The Roster tab now has a **Monthly allocation history** section below the generated duty roster.
- The manager chooses any date in the target month.
- The matrix has member rows, dynamically generated duty columns, assignment dates/counts in each cell, and a total allocation column.
- The manager can export the selected month as a CSV file.

## CSV export refinement

The manager requested that total duty count be represented as a separate table rather than a column in the detailed allocation table.

The export now contains:

1. A detailed allocation table with `Month, Member, Duty, Date`.
2. A blank row.
3. A member-total table with `Month, Member, Total duty count`.

Each duty allocation contributes one to a member's total, independent of duty name.

## Verification

The project self-test completed successfully after the implementation and the export refinement:

```bash
./gradlew selfTest
```
