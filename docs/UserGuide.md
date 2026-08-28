# TeamSync MVP User Guide

## Start the application

TeamSync now uses JavaFX. From the project root, run:

```bash
gradle run
```

The Gradle wrapper automatically provisions Java SE 25 and downloads the matching JavaFX 25 native libraries for Windows, Linux, or macOS on the first run. Use the platform-appropriate command: `./gradlew run` on macOS/Linux and `gradlew.bat run` on Windows.

On Linux, JavaFX 25 requires GTK 3.20 or later; install your distribution's GTK 3 runtime before launching TeamSync.

## MVP workflow

1. **Prepare and link an attendance sheet.** Share the Google Sheet as **Anyone with the link**, paste its URL, and select **Link sheet**. Its first column must be named `Member`; every remaining column header must use an ISO date such as `2026-08-20`. Use `1` for on time, `0` for absent, `L` for late, and `E` for leaving early.
2. **Load the session attendance.** Select a training date and choose **Load attendance**. TeamSync reads the matching date column directly from Google Sheets, displays a copy-ready attendance message, and shows session statistics that can be exported as a PNG. The export also includes individual attendance percentages and overall session attendance percentages for the selected month; `L` and `E` count as attended for these percentages. It displays an error when the date column is not present. Only members marked `1` are selected for full-session duties.
3. **Manage duties.** Add a duty name and the number of people needed. Use **Edit** or **Delete** whenever the duty list changes.
4. **Generate the roster.** TeamSync selects confirmed attendees and prefers members with fewer historical assignments for the same duty. A person is assigned to no more than one duty that day unless the total number of required duty slots exceeds the number of confirmed attendees.

TeamSync saves its local workspace in your user directory, so linked Sheet details, duties, attendance, and roster history remain available after restarting the application.

## Verify the MVP

Run the regression test with:

```bash
gradle selfTest
```
