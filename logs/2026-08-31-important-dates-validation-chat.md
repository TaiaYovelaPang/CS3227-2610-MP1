# Important Dates Validation Conversation — 31 August 2026

## Event validation

The manager requested safeguards for important dates. The following rules were implemented for both adding and editing events:

- Event names must be unique after trimming whitespace and ignoring capitalisation.
- Events cannot start in the past.
- An event must have an end time after its start time.
- Events cannot overlap another saved event on the same date. Events that meet exactly at a boundary are allowed; for example, an event ending at 10:30 does not clash with one starting at 10:30.

`ImportantDateValidator` contains the workspace-wide name and schedule checks, while `ImportantDate` validates an event's own duration.

## Time and reminder experience

The important-dates form now has start and end time fields and explains the required 24-hour `HH:mm` format, for example `09:30` or `14:00`.

The saved-events table and Overview page display each event's start and end time. Custom reminder lead times are entered as a whole number of hours rather than minutes. Standard choices include at-event time, one hour before, and one day before. Older saved 15-minute reminders remain readable for compatibility.

## Expired events

Events are automatically removed from saved dates when their end time is reached. Cleanup runs when TeamSync starts and every 30 seconds while it remains open. Any removal is immediately saved to the local workspace. An event that is still in progress is retained until it ends.

## Verification

Regression coverage includes duplicate names, past events, overlapping events, valid back-to-back events, invalid durations, custom hour reminders, and removal of expired events.

```bash
./gradlew test selfTest
```

The tests passed after implementation.
