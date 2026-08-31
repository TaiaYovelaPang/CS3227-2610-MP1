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
2. **Load the session attendance.** Select a training date and choose **Load attendance**. TeamSync reads the matching date column directly from Google Sheets, displays a copy-ready attendance message, and shows session statistics that can be exported as a PNG. The export also includes individual attendance percentages and overall session attendance percentages for the selected month; `L` and `E` count as attended for these percentages. It displays an error when the date column is not present.
3. **Manage duties.** Add a duty name and the number of people needed. For each duty, choose which attendance states may be assigned: on time, late, and/or leaving early. At least one must be selected. Use **Edit** or **Delete** whenever the duty list changes.
4. **Generate the roster.** TeamSync selects only members permitted by each duty's attendance choices and prefers members with fewer historical assignments for the same duty. A person is assigned to no more than one duty that day unless the total number of required duty slots exceeds the number of members eligible for at least one duty.

TeamSync saves its local workspace in your user directory, so linked Sheet details, duties, attendance, and roster history remain available after restarting the application.

## Verify the MVP

Run the regression test with:

```bash
gradle selfTest
```
