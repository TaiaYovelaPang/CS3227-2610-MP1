# UI Design and Warning Cleanup Conversation — 31 August 2026

## Attendance statistics and exports

The Attendance page's statistics hierarchy was clarified:

- The main section is titled **Attendance statistics**.
- **Session statistics** appears as its subtitle.
- The currently loaded session is presented as a **Selected session summary**.
- Monthly data is separated under **Monthly reports**.

Monthly attendance can be exported as two independent statistics:

- **Attendance by session** as a PNG image or CSV file.
- **Individual attendance** as a PNG image or CSV file.

This provides four distinct export buttons. Each exported file contains only the chosen statistic. Users can also select the report month independently of the currently loaded attendance session by choosing any date within the desired month.

## UI review and usability improvements

The interface was reviewed from a senior UI-design perspective, focusing on ease of use, functionality, appearance, responsive behaviour, information hierarchy, and empty states.

The resulting improvements include:

- Form controls, duty eligibility options, roster toolbars, date inputs, attendance cards, and export actions wrap cleanly when horizontal space is limited.
- Attendance statistic cards reflow according to the available width.
- Overview cards display as three equal cards on wide screens, two on medium screens, and one on narrow screens.
- Whole-pixel card widths prevent the final card from unexpectedly wrapping after a window is maximised or resized.
- Empty attendance summaries explain how to load data.
- Disabled copy and report-export actions explain how to unlock them.
- Duty-roster generation remains unavailable until both attendance and duties are present.

## IDE warning cleanup

The warnings reported for `TeamSyncApp.java` were resolved without changing the application's functionality or visual design:

- Unused JavaFX lambda parameters were replaced with Java 25 unnamed parameters.
- The unused `confirmedCount()` method was removed.
- Helpers whose parameters always received the same values were simplified.
- Stylesheet loading now explicitly validates that `theme.css` exists before converting its URL to an external form, removing possible-null warnings and producing a clearer failure if the packaged resource is ever missing.

## Documentation and verification

The user guide was updated to describe the separate monthly exports and report-month selection.

The project was cleanly compiled and the regression suite passed after the final warning cleanup:

```bash
./gradlew clean selfTest
```

The final changes primarily affect:

- `src/main/java/teamsync/TeamSyncApp.java`
- `src/main/resources/teamsync/theme.css`
- `docs/UserGuide.md`
