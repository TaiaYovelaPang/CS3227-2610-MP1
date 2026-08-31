package teamsync;

import java.util.Locale;

/** The four supported attendance states in a session column. */
public enum AttendanceStatus {
    ON_TIME("1", "On time"),
    ABSENT("0", "Absent"),
    LATE("L", "Coming late"),
    LEFT_EARLY("E", "Leaving early");

    private final String sheetCode;
    private final String displayName;

    AttendanceStatus(String sheetCode, String displayName) {
        this.sheetCode = sheetCode;
        this.displayName = displayName;
    }

    public String sheetCode() { return sheetCode; }
    public String displayName() { return displayName; }

    public boolean attended() { return this != ABSENT; }
    public boolean isEligibleForFullSessionDuty() { return this == ON_TIME; }

    public static AttendanceStatus fromSheetValue(String value) {
        String normalised = value.replace("\u00A0", "").replace("\u200B", "")
                .replaceAll("[\\s\\p{Z}]+", "").toUpperCase(Locale.ROOT);
        return switch (normalised) {
            case "1", "P", "PRESENT" -> ON_TIME;
            case "0", "A", "ABSENT" -> ABSENT;
            case "L", "LATE", "LATECOMER" -> LATE;
            case "E", "EARLY", "LEFTEARLY", "LEAVINGEARLY" -> LEFT_EARLY;
            default -> throw new IllegalArgumentException("Unsupported attendance value: " + value);
        };
    }
}
