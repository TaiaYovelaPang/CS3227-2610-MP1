# Important Dates Feature Summary — 29 August 2026

## Selected design

The list-first design was selected for TeamSync because it matches the existing form-and-table interaction used for duties. It provides a dedicated **Important dates** navigation tab and a compact **Upcoming important dates** section on the Overview page.

## Delivered functionality

- Users can add an event name, date, and 24-hour time.
- Saved events are shown in chronological order in a table with event, date, time, and reminder columns.
- Selecting an event loads it into the form for updating or deletion.
- Events are saved in the local workspace and remain available after restarting TeamSync.
- The Overview page shows the next three future events.

## Reminders

Each event has an optional **Remind me** toggle. When enabled, users can choose one of the following options:

- At event time
- 15 minutes before
- 1 day before
- Custom lead time, entered in minutes (from 1 minute to 30 days before the event)

TeamSync checks for due reminders about every 30 seconds and displays an in-app alert while the application is open. A reminder is shown only once during a running session.

## Implementation notes

- `ImportantDate` stores the event details and optional reminder configuration.
- `ReminderOption` defines the standard reminder lead times and custom selection.
- `Workspace` now persists important dates while retaining compatibility with existing locally saved workspaces.
- The self-test includes coverage for custom reminder storage and disabling a reminder.

## Future enhancements

- Use operating-system notifications so reminders can be delivered even when TeamSync is minimised or closed.
- Add recurring events, such as weekly meetings or monthly deadlines.
- Provide calendar and agenda views in addition to the current list view.
- Allow event categories, colours, descriptions, locations, and attached links.
- Support multiple reminders per event, for example one day and 15 minutes before.
- Add filters for upcoming, completed, and date-range-specific events, plus search by event name.
- Highlight upcoming events and overdue items in the navigation or Overview page.
- Export and import events in CSV or iCalendar (`.ics`) format for calendar interoperability.
- Allow team members to receive or subscribe to shared event reminders when TeamSync gains collaborative storage.

## Verification

The regression suite passed after implementation:

```bash
./gradlew selfTest
```
