package teamsync;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Attendance percentages calculated from every marked session in one calendar month. */
public record MonthlyAttendanceReport(YearMonth month, List<SessionAttendance> sessions,
                                      List<MemberAttendance> members) {
    public MonthlyAttendanceReport {
        sessions = List.copyOf(sessions);
        members = List.copyOf(members);
    }

    public int attendedMembersAcrossSessions() {
        return sessions.stream().mapToInt(SessionAttendance::attendedMembers).sum();
    }

    public int markedMembersAcrossSessions() {
        return sessions.stream().mapToInt(SessionAttendance::markedMembers).sum();
    }

    public int overallAttendancePercentage() {
        return percentageOf(attendedMembersAcrossSessions(), markedMembersAcrossSessions());
    }

    public record SessionAttendance(LocalDate date, int attendedMembers, int markedMembers) {
        public int percentage() { return percentageOf(attendedMembers, markedMembers); }
    }

    public record MemberAttendance(String memberName, int attendedSessions, int markedSessions) {
        public int percentage() { return percentageOf(attendedSessions, markedSessions); }
    }

    private static int percentageOf(int numerator, int denominator) {
        return denominator == 0 ? 0 : (int) Math.round(numerator * 100.0 / denominator);
    }
}
