# TeamSync MVP UI Summary — 28 August 2026

## UI direction

Three possible blue JavaFX designs were considered:

1. A command centre with navigation and an overview dashboard.
2. A guided single-page workflow.
3. A split configuration-and-roster workspace.

The command-centre design was selected because TeamSync is expected to gain further modules. It provides a stable navigation structure while preserving the existing attendance, duties, and roster workflow.

## JavaFX conversion

The Swing interface was converted to JavaFX.

- The application now uses a JavaFX `Application`, `Stage`, `Scene`, `BorderPane`, navigation sidebar, overview cards, `TableView`, `DatePicker`, `Spinner`, and JavaFX background `Task` for attendance loading.
- A blue visual theme was added through `src/main/resources/teamsync/theme.css`.
- The command centre contains `Overview`, `Attendance`, `Duties`, and `Roster` pages.
- Existing Google Sheets loading, duty management, roster generation, persistence, and validation logic were retained.

## Java 25 and operating-system support

- The Gradle build uses a Java SE 25 toolchain and compiles with `--release 25`.
- JavaFX 25.0.2 dependencies are selected for the current operating system and architecture: Windows, Linux, Intel macOS, Apple Silicon macOS, and Linux ARM64.
- The Gradle 9.1 wrapper (`gradlew` and `gradlew.bat`) was added, along with automatic Java toolchain provisioning.
- The build treats deprecation and unchecked warnings as errors to keep later code changes aligned with the Java 25 baseline.
- Linux users require GTK 3.20 or later for JavaFX.

## Responsive UI and text handling

- The main content area is scrollable when vertical space is limited.
- Overview cards reflow from three columns to two or one column as the window narrows.
- The application minimum window size was reduced to 640 by 480.
- Labels and duty table cells wrap their text; long table text is clipped rather than rendered with an ellipsis.

## JavaFX startup fix

Launching `TeamSyncApp` directly caused the Java launcher error: "JavaFX runtime components are missing".

`TeamSyncLauncher` was added as a standard Java entry point. It invokes `Application.launch(TeamSyncApp.class, args)`, and the Gradle application task now launches `TeamSyncLauncher`. This allows JavaFX dependencies on Gradle's classpath to be used correctly.

## Verification

The Java 25 build and regression checks succeeded with:

```bash
./gradlew clean selfTest installDist
```

Use `./gradlew run` on macOS/Linux or `gradlew.bat run` on Windows.

## Deferred attendance work

The following enhancement was requested but was not implemented in this branch summary:

- Introduce distinct attendance inputs for present, absent, late arrival, and early departure.
- Display a copyable attendance message after loading attendance.
- Add sports-team attendance statistics and provide image export when requested.
