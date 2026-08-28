package teamsync;

import java.time.LocalDate;
import java.util.List;

/** Creates the shareable attendance update from the exact states loaded from a sheet column. */
public final class AttendanceUpdateFormatter {
    private AttendanceUpdateFormatter() { }

    public static String format(LocalDate date, List<AttendanceRecord> attendance) {
        if (attendance.isEmpty()) return "Load a session to create a copy-ready attendance update.";
        StringBuilder message = new StringBuilder("Attendance update — ").append(date).append("\n\n");
        appendGroup(message, attendance, "Attending (on time)", AttendanceStatus.ON_TIME);
        appendGroup(message, attendance, "Coming late", AttendanceStatus.LATE);
        appendGroup(message, attendance, "Leaving early", AttendanceStatus.LEFT_EARLY);
        appendGroup(message, attendance, "Absent", AttendanceStatus.ABSENT);
        return message.toString().stripTrailing();
    }

    private static void appendGroup(StringBuilder message, List<AttendanceRecord> attendance, String heading,
                                    AttendanceStatus status) {
        List<String> names = attendance.stream().filter(record -> record.status() == status)
                .map(AttendanceRecord::memberName).sorted().toList();
        message.append(heading).append(" (").append(names.size()).append("):\n");
        if (names.isEmpty()) message.append("• None\n\n");
        else {
            names.forEach(name -> message.append("• ").append(name).append("\n"));
            message.append("\n");
        }
    }
}
