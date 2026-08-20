# TeamSync MVP User Guide

## Start the application

From the project root, compile the Java source:

```bash
javac -d out $(find src/main/java -name '*.java')
```

Then start the desktop application:

```bash
java -cp out teamsync.TeamSyncApp
```

## MVP workflow

1. **Prepare and link an attendance sheet.** Share the Google Sheet as **Anyone with the link**, paste its URL, and select **Link sheet**. Its first column must be named `Member`; every remaining column header must use an ISO date such as `2026-08-20`. Attendance values are `1`, `0.5`, or `0`.
2. **Load the session attendance.** Select a training date and choose **Load attendance**. TeamSync reads the matching date column directly from Google Sheets. It displays an error when the date column is not present.
3. **Manage duties.** Add a duty name and the number of people needed. Use **Edit** or **Delete** whenever the duty list changes.
4. **Generate the roster.** TeamSync selects confirmed attendees and prefers members with fewer historical assignments for the same duty. A person is assigned to no more than one duty that day unless the total number of required duty slots exceeds the number of confirmed attendees.

TeamSync saves its local workspace in your user directory, so linked Sheet details, duties, attendance, and roster history remain available after restarting the application.

## Verify the MVP

Run the dependency-free regression test with:

```bash
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out teamsync.TeamSyncSelfTest
```
