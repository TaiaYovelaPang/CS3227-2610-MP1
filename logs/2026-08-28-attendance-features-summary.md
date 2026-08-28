# Attendance Features Summary — 28 August 2026

## Attendance codes

TeamSync now supports four values in every dated attendance column:

| Value | Meaning | Counts as attending | Eligible for a full-session duty |
| --- | --- | --- | --- |
| `1` | On time | Yes | Yes |
| `0` | Absent | No | No |
| `L` | Late | Yes | No |
| `E` | Left early | Yes | No |

The parser also accepts readable equivalents such as `Late` and `Left early`, and normalises common hidden whitespace copied from Sheets.

## Attendance update

After a session is loaded, the Attendance page creates a copy-ready update grouped into:

1. Attending (on time)
2. Coming late
3. Leaving early
4. Absent

The update is shown in a read-only text area and can be copied to the system clipboard. Names are sorted alphabetically within each group.

Example:

```text
Attendance update — 2026-08-20

Attending (on time) (4):
• Amy
• Ben
• Charlie
• Julius

Coming late (1):
• Greta

Leaving early (1):
• Hannah

Absent (2):
• Daniel
• Elizabeth
```

## Attendance statistics and export

The Attendance page displays current-session statistics for attendance rate, on-time arrivals, late arrivals, and early departures.

Selecting **Export statistics as image** creates a PNG containing:

- Current-session statistics
- Monthly team attendance percentage
- Attendance percentage for every session in the selected month
- Individual attendance percentage for every member with marked sessions in that month

Monthly percentages count `1`, `L`, and `E` as attendance. They only use sessions where the member has a valid attendance value, so future or unmarked sessions do not reduce a percentage.

## Google Sheets loading fix

The first implementation used Google Sheets' `gviz` CSV query endpoint. That endpoint can infer a column containing mostly `1` and `0` as numeric, causing text values such as `L` and `E` to arrive as blank cells.

TeamSync now uses the raw Google Sheets CSV export endpoint instead. It follows Google's normal download redirect before parsing the returned CSV, preserving mixed values in a single attendance column. This loader is shared by both normal session loading and monthly-statistics export.

If a named member has a blank or unsupported attendance value, TeamSync now reports the member name, sheet row, date, and required codes rather than silently omitting the member from the update.

## Verification

- The saved linked Sheet was read through TeamSync after the final CSV-export and redirect handling changes.
- It returned 8 records, including 1 late arrival and 1 early departure.
- The self-test covers missing dates, the `1`/`0`/`L`/`E` codes, the expected attendance update grouping, monthly percentages, the raw CSV URL, and roster fairness.
- `./gradlew --no-daemon selfTest jar` completed successfully.

## Current limitation

TeamSync reads public Sheets through a shared link. It does not write statistics back into the Sheet because Google Sheets edits require authenticated Google OAuth/API access. Statistics are therefore calculated during export and included in the PNG image.
