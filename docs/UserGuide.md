# TeamSync User Guide

## Quick start

TeamSync is supplied as `teamsync.jar` in the project's `release` folder. The JAR bundles TeamSync and its JavaFX runtime dependencies, so only Java 25 is required to run it.

1. Install a Java Development Kit (JDK) 25. Confirm the installed version with:

   ```bash
   java -version
   ```

2. Download `teamsync.jar` from the `release` folder or from [here](https://github.com/TaiaYovelaPang/CS3227-2610-MP1/releases/tag/v1.0) and copy it to the folder you want to use as the TeamSync home folder.
3. Open a command terminal and change into that folder:

   ```bash
   cd path/to/teamsync
   ```

4. Start TeamSync:

   ```bash
   java --enable-native-access=ALL-UNNAMED -jar teamsync.jar
   ```

The `--enable-native-access=ALL-UNNAMED` option lets JavaFX load its bundled native libraries without displaying a Java 25 warning.

Release JARs are platform-specific because JavaFX includes native libraries. Use a JAR built for your operating system and processor architecture. The included `release/teamsync.jar` is built on the current platform; to create a release JAR on another platform, run `./gradlew releaseJar` from a TeamSync source checkout. Linux users also require GTK 3.20 or later.

## Verify the application

From the project folder, run the regression checks with:

```bash
./gradlew selfTest
```

On Windows, use `gradlew.bat selfTest`.

## Features

### Overview

The **Overview** page provides a starting point for the normal workflow: load attendance, maintain duties, generate a roster, and review upcoming important dates. Select a card action to move directly to that area.

### Attendance

#### Link a Google Sheet

1. In Google Sheets, share the attendance sheet as **Anyone with the link**.
2. In TeamSync, open **Attendance**.
3. Paste the sheet URL into the attendance-source field and select **Link sheet**.

The sheet must use the following format:

| Column | Requirement |
| --- | --- |
| First column | Header must be `Member`; each row contains a member name. |
| Remaining columns | Header must be an ISO date in `YYYY-MM-DD` format, for example `2026-08-20`. |
| Attendance values | `1` for on time, `0` for absent, `L` for coming late, and `E` for leaving early. |

#### Load an attendance session

1. Select the session date.
2. Select **Load attendance**.

TeamSync reads the column for that date, groups members by attendance status, and shows a copy-ready attendance update. Select **Copy attendance message** to place that update on the system clipboard.

TeamSync shows an error if the sheet cannot be accessed, the `Member` column or selected date is missing, or an attendance value is blank or unsupported.

#### Export monthly attendance statistics

Under **Attendance statistics**, select any date in the month you want to report. Choose either **Attendance by session** or **Individual attendance**, then select **Export as image** or **Export as CSV**. Choose the destination in the file chooser to create the file.

Late and early-leaving members count as attended in monthly attendance percentages.

### Duties

Open **Duties** to define the jobs required for a training session.

1. Enter a duty name and the number of people required.
2. Select the attendance states that may be assigned to the duty: **On time**, **Coming late**, and/or **Leaving early**.
3. Select **Add duty**.

At least one eligible attendance state is required. Absent members cannot be assigned. Duty names must be unique regardless of capitalization.

To edit a duty, select it in **Current duties**, change the values in the form, and select **Update selected**. To remove it, select the duty and choose **Delete selected**.

### Roster

#### Generate a duty roster

Before generating a roster, load attendance for the relevant date and add at least one duty.

1. Open **Roster**.
2. Select **Generate duty roster**.

TeamSync assigns only members whose attendance state is permitted for each duty. It handles less-flexible duties first, gives eligible members one duty before assigning a second, and prefers people with fewer prior assignments for the same duty. When there are too few eligible members, the roster displays the number of unfilled slots. A roster regenerated for the same date replaces the prior roster history for that date.

#### Review and export roster history

Use **History month** to select any date in the month to review. The table groups every saved allocation by member and duty. Select **Export month as CSV**, choose a location, and TeamSync exports the selected month's allocations and totals.

### Important dates and reminders

Open **Important dates** to keep team events and deadlines in one place.

1. Enter the event name, date, start time, and end time in 24-hour `HH:mm` format.
2. Optionally select **Remind me** and choose a reminder time. A custom reminder is measured in hours before the event.
3. Select **Add event**.

Events must be in the future, end after they start, have unique names, and not overlap with another saved event. Select an existing event to populate the form, then use **Update selected** or **Delete selected** as needed.

Reminders appear as a TeamSync alert while the application is open. TeamSync checks reminders every 30 seconds and removes an event after its end time has passed.

### Saving the data
TeamSync data is saved in the hard disk automatically after any command that changes the data. There is no need to save manually.

### Editing the data file

TeamSync automatically saves its workspace in the user home directory as `.teamsync-mvp-workspace.ser`. It contains the linked sheet URL, current attendance, duties, roster history, and important dates.

Do not edit this file directly: it is a Java-serialized binary file, not a text or CSV file. Use the TeamSync pages to change data safely.

To start with a blank workspace, close TeamSync and rename, move, or delete `.teamsync-mvp-workspace.ser`. Keep a copy first if you may need to recover the data. TeamSync creates a new empty workspace the next time it starts.
