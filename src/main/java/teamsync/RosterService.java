package teamsync;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Applies attendance, fairness, and same-day assignment rules to create a roster. */
public final class RosterService {
    public List<RosterAssignment> generate(Workspace workspace) {
        List<String> available = workspace.attendance().stream().filter(AttendanceRecord::isConfirmed)
                .map(AttendanceRecord::memberName).toList();
        if (available.isEmpty()) throw new IllegalStateException("Load confirmed attendance before generating a roster.");
        if (workspace.duties().isEmpty()) throw new IllegalStateException("Add at least one duty before generating a roster.");

        LocalDate date = workspace.sessionDate();
        workspace.history().removeIf(assignment -> assignment.date().equals(date));
        int requiredSlots = workspace.duties().stream().mapToInt(Duty::peopleNeeded).sum();
        boolean repeatsAllowed = available.size() < requiredSlots;
        Set<String> assignedToday = new HashSet<>();
        Map<String, Integer> dailyCounts = new HashMap<>();
        available.forEach(member -> dailyCounts.put(member, 0));
        List<RosterAssignment> roster = new ArrayList<>();

        for (Duty duty : workspace.duties()) {
            List<String> selected = ordered(available.stream().filter(member -> !assignedToday.contains(member)).toList(), duty.name(), dailyCounts, workspace.history())
                    .stream().limit(duty.peopleNeeded()).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            if (repeatsAllowed && selected.size() < duty.peopleNeeded()) {
                List<String> repeats = ordered(available.stream().filter(member -> !selected.contains(member)).toList(), duty.name(), dailyCounts, workspace.history());
                selected.addAll(repeats.stream().limit(duty.peopleNeeded() - selected.size()).toList());
            }
            selected.forEach(member -> { assignedToday.add(member); dailyCounts.merge(member, 1, Integer::sum); });
            roster.add(new RosterAssignment(date, duty.name(), selected, duty.peopleNeeded() - selected.size()));
        }
        workspace.history().addAll(roster);
        return roster;
    }

    private List<String> ordered(List<String> members, String dutyName, Map<String, Integer> dailyCounts, List<RosterAssignment> history) {
        return members.stream().sorted(Comparator.<String>comparingInt(dailyCounts::get)
                .thenComparingInt(member -> priorAssignments(member, dutyName, history)).thenComparing(String::compareToIgnoreCase)).toList();
    }

    private int priorAssignments(String member, String dutyName, List<RosterAssignment> history) {
        return (int) history.stream().filter(item -> item.dutyName().equals(dutyName) && item.members().contains(member)).count();
    }
}
