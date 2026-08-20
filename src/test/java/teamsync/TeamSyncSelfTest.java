package teamsync;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Small dependency-free regression test for the MVP's attendance and roster rules. */
public final class TeamSyncSelfTest {
    public static void main(String[] args) {
        verifiesMissingDateIsRejected();
        verifiesSingleDutyBeforeRepeats();
        System.out.println("TeamSync self-test passed.");
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
}
