package teamsync;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Small dependency-free regression test for the MVP's attendance and roster rules. */
public final class TeamSyncSelfTest {
    public static void main(String[] args) {
        verifiesMissingDateIsRejected();
        verifiesRawCsvExportIsUsed();
        verifiesMixedAttendanceCodesArePreservedInTheUpdate();
        verifiesAttendanceCodesAreParsed();
        verifiesExpectedAttendanceUpdate();
        verifiesMonthlyPercentagesIncludeLateAndEarlyAttendance();
        verifiesMonthlyRosterHistoryGroupsAssignmentsByMemberAndDuty();
        verifiesImportantDateReminderConfiguration();
        verifiesSingleDutyBeforeRepeats();
        System.out.println("TeamSync self-test passed.");
    }

    private static void verifiesAttendanceCodesAreParsed() {
        AttendanceService service = new AttendanceService();
        List<AttendanceRecord> attendance = service.attendanceForDate(List.of(
                List.of("Member", "2026-08-20"),
                List.of("Amy", "1"), List.of("Ben", "\u00A0L\u00A0"), List.of("Chen", "\u200BE"), List.of("Dev", "0")),
                LocalDate.parse("2026-08-20"));
        if (attendance.size() != 4 || attendance.get(1).status() != AttendanceStatus.LATE
                || attendance.get(1).value() != 1.0 || attendance.get(2).status() != AttendanceStatus.LEFT_EARLY
                || attendance.get(3).attended()) {
            throw new AssertionError("The 1, 0, L, and E attendance codes must be retained.");
        }
    }

    private static void verifiesMonthlyPercentagesIncludeLateAndEarlyAttendance() {
        AttendanceService service = new AttendanceService();
        MonthlyAttendanceReport report = service.monthlyAttendanceForMonth(List.of(
                List.of("Member", "2026-08-03", "2026-08-10", "2026-09-01"),
                List.of("Amy", "1", "L", "0"), List.of("Ben", "E", "0", "1")),
                java.time.YearMonth.parse("2026-08"));
        if (report.sessions().size() != 2 || report.sessions().get(0).percentage() != 100
                || report.sessions().get(1).percentage() != 50 || report.members().get(0).percentage() != 100
                || report.members().get(1).percentage() != 50 || report.overallAttendancePercentage() != 75) {
            throw new AssertionError("Late and early attendance must count towards monthly attendance percentages.");
        }
    }

    private static void verifiesExpectedAttendanceUpdate() {
        LocalDate date = LocalDate.parse("2026-08-24");
        List<AttendanceRecord> attendance = List.of(
                new AttendanceRecord("Amy", AttendanceStatus.ABSENT),
                new AttendanceRecord("Ben", AttendanceStatus.LATE),
                new AttendanceRecord("Charlie", AttendanceStatus.ABSENT),
                new AttendanceRecord("Daniel", AttendanceStatus.ABSENT),
                new AttendanceRecord("Elizabeth", AttendanceStatus.ABSENT),
                new AttendanceRecord("Greta", AttendanceStatus.LEFT_EARLY),
                new AttendanceRecord("Hannah", AttendanceStatus.LATE),
                new AttendanceRecord("Julius", AttendanceStatus.ON_TIME));
        String expected = """
                Attendance update — 2026-08-24

                Attending (on time) (1):
                • Julius

                Coming late (2):
                • Ben
                • Hannah

                Leaving early (1):
                • Greta

                Absent (4):
                • Amy
                • Charlie
                • Daniel
                • Elizabeth""";
        String actual = AttendanceUpdateFormatter.format(date, attendance);
        if (!expected.equals(actual)) throw new AssertionError("Attendance update must match the expected L/E grouping.");
    }

    private static void verifiesRawCsvExportIsUsed() {
        String csvUrl = new AttendanceService().csvUri(
                "https://docs.google.com/spreadsheets/d/example-sheet-id/edit#gid=456789").toString();
        if (!csvUrl.equals("https://docs.google.com/spreadsheets/d/example-sheet-id/export?format=csv&gid=456789")) {
            throw new AssertionError("Attendance must use the raw CSV export so L and E values are preserved.");
        }
    }

    private static void verifiesMixedAttendanceCodesArePreservedInTheUpdate() {
        AttendanceService service = new AttendanceService();
        List<AttendanceRecord> attendance = service.attendanceForDate(List.of(
                List.of("Member", "2026-08-20"),
                List.of("Amy", "1"), List.of("Ben", "1"), List.of("Charlie", "1"),
                List.of("Daniel", "0"), List.of("Elizabeth", "0"), List.of("Greta", "L"),
                List.of("Hannah", "E"), List.of("Julius", "1")), LocalDate.parse("2026-08-20"));
        String update = AttendanceUpdateFormatter.format(LocalDate.parse("2026-08-20"), attendance);
        if (!update.contains("Coming late (1):\n• Greta") || !update.contains("Leaving early (1):\n• Hannah")) {
            throw new AssertionError("A sheet column containing 1, 0, L, and E must retain every attendance state.");
        }
    }

    private static void verifiesMissingDateIsRejected() {
        AttendanceService service = new AttendanceService();
        boolean rejected = false;
        try {
            service.attendanceForDate(List.of(List.of("Member", "2026-08-20"), List.of("Amy", "1")), LocalDate.parse("2026-08-21"));
        } catch (IllegalArgumentException error) {
            rejected = error.getMessage().contains("No attendance column");
        }
        if (!rejected) throw new AssertionError("A missing date column must be rejected.");
    }

    private static void verifiesSingleDutyBeforeRepeats() {
        Workspace workspace = new Workspace();
        workspace.setSessionDate(LocalDate.parse("2026-08-20"));
        workspace.setAttendance(List.of(new AttendanceRecord("Amy", 1), new AttendanceRecord("Ben", 1), new AttendanceRecord("Chen", 1)));
        workspace.duties().add(new Duty("Set up", 1));
        workspace.duties().add(new Duty("Pack down", 1));
        workspace.duties().add(new Duty("Lock up", 1));
        List<RosterAssignment> noRepeats = new RosterService().generate(workspace);
        List<String> firstPass = noRepeats.stream().flatMap(assignment -> assignment.members().stream()).toList();
        if (firstPass.size() != Set.copyOf(firstPass).size()) throw new AssertionError("Members should not be assigned twice when there are enough attendees.");

        workspace.duties().add(new Duty("Equipment check", 1));
        List<RosterAssignment> repeatsRequired = new RosterService().generate(workspace);
        List<String> secondPass = repeatsRequired.stream().flatMap(assignment -> assignment.members().stream()).toList();
        if (secondPass.size() != 4 || Set.copyOf(secondPass).size() != 3) throw new AssertionError("Exactly one repeated assignment is expected when four slots need three attendees.");
    }

    private static void verifiesMonthlyRosterHistoryGroupsAssignmentsByMemberAndDuty() {
        MonthlyRosterHistory history = MonthlyRosterHistory.from(java.time.YearMonth.parse("2026-08"), List.of(
                new RosterAssignment(LocalDate.parse("2026-08-03"), "Set up", List.of("Amy", "Ben"), 0),
                new RosterAssignment(LocalDate.parse("2026-08-10"), "Set up", List.of("Amy"), 0),
                new RosterAssignment(LocalDate.parse("2026-08-10"), "Pack down", List.of("Ben"), 0),
                new RosterAssignment(LocalDate.parse("2026-09-01"), "Set up", List.of("Chen"), 0)));
        MonthlyRosterHistory.MemberRow amy = history.members().getFirst();
        if (!history.dutyNames().equals(List.of("Pack down", "Set up")) || history.members().size() != 2
                || !amy.memberName().equals("Amy") || amy.datesFor("Set up").size() != 2
                || amy.totalAssignments() != 2) {
            throw new AssertionError("Monthly roster history must group each member's allocations by duty and omit other months.");
        }
    }

    private static void verifiesImportantDateReminderConfiguration() {
        ImportantDate event = new ImportantDate("Team briefing", LocalDate.parse("2026-09-01"), LocalTime.of(9, 30),
                ReminderOption.CUSTOM, 45);
        if (!event.hasReminder() || event.reminderMinutesBefore() != 45
                || !event.reminderLabel().equals("Custom: 45 min before")) {
            throw new AssertionError("Important dates must retain their custom reminder lead time.");
        }
        event.update("Team briefing", LocalDate.parse("2026-09-01"), LocalTime.of(9, 30), null, 0);
        if (event.hasReminder() || !event.reminderLabel().equals("Off")) {
            throw new AssertionError("Important dates must allow reminders to be turned off.");
        }
    }
}
