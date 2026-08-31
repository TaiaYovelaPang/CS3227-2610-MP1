# Documentation, Release JAR, and Diagram Rendering Conversation — 1 September 2026

## Developer Guide

The Developer Guide was expanded to document the current TeamSync implementation rather than planned functionality. It now covers:

- A high-level application architecture.
- Sequence diagrams for loading attendance, generating a roster, and saving an important date with reminders.
- Class diagrams for the UI/application coordinator, attendance/reporting, duty-roster, and important-dates components.
- User stories and detailed use cases with main success scenarios and extensions.
- Performance, reliability, usability, and compatibility requirements.

GitHub renders Mermaid diagrams natively. The sequence diagrams initially failed because Mermaid interprets a semicolon in a message label as a statement separator. All affected labels were rewritten to use plain language without semicolons, preventing the GitHub parse errors.

## User Guide

The User Guide was rewritten with a Quick Start and feature-oriented instructions for:

- Linking a Google Sheets attendance source and loading a session.
- Copying attendance updates and exporting monthly attendance statistics.
- Adding, editing, and deleting duties with attendance eligibility choices.
- Generating a balanced duty roster and exporting monthly roster history.
- Managing important dates and optional in-app reminders.
- Safely managing the local serialized workspace file.

## Executable release JAR

A `releaseJar` Gradle task was added to `build.gradle`. It creates `release/teamsync.jar` with:

- TeamSync classes and resources.
- All runtime dependencies, including JavaFX classes and native libraries.
- `teamsync.TeamSyncLauncher` as the executable `Main-Class` manifest entry.

The generated JAR is platform-specific because JavaFX includes native libraries. It can be rebuilt for another target platform with:

```bash
./gradlew releaseJar
```

With JDK 25, the packaged application starts with:

```bash
java --enable-native-access=ALL-UNNAMED -jar teamsync.jar
```

The native-access option avoids Java 25 warnings while JavaFX loads its bundled native libraries.

## Verification

The release JAR's manifest and bundled JavaFX native libraries were inspected, and the regression suite passed:

```bash
./gradlew selfTest
```
