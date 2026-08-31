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
        List<AttendanceRecord> attendees = workspace.attendance().stream().filter(AttendanceRecord::attended).toList();
        if (attendees.isEmpty()) throw new IllegalStateException("Load attendance before generating a roster.");
        if (workspace.duties().isEmpty()) throw new IllegalStateException("Add at least one duty before generating a roster.");

        LocalDate date = workspace.sessionDate();
        workspace.history().removeIf(assignment -> assignment.date().equals(date));
        int requiredSlots = workspace.duties().stream().mapToInt(Duty::peopleNeeded).sum();
        Set<String> eligibleMembers = attendees.stream()
                .filter(record -> workspace.duties().stream().anyMatch(duty -> duty.isEligible(record.status())))
                .map(AttendanceRecord::memberName).collect(java.util.stream.Collectors.toSet());
        boolean repeatsAllowed = eligibleMembers.size() < requiredSlots;
        Set<String> assignedToday = new HashSet<>();
        Map<String, Integer> dailyCounts = new HashMap<>();
        eligibleMembers.forEach(member -> dailyCounts.put(member, 0));
        List<Duty> assignmentOrder = new ArrayList<>(workspace.duties());
        assignmentOrder.sort(Comparator.comparingInt(duty -> eligibleMembersFor(duty, attendees).size()));
        Map<Duty, RosterAssignment> assignments = new HashMap<>();

        for (Duty duty : assignmentOrder) {
            List<String> eligibleForDuty = eligibleMembersFor(duty, attendees);
            List<String> selected = ordered(eligibleForDuty.stream().filter(member -> !assignedToday.contains(member)).toList(), duty.name(), dailyCounts, workspace.history())
                    .stream().limit(duty.peopleNeeded()).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            if (repeatsAllowed && selected.size() < duty.peopleNeeded()) {
                List<String> repeats = ordered(eligibleForDuty.stream().filter(member -> !selected.contains(member)).toList(), duty.name(), dailyCounts, workspace.history());
                selected.addAll(repeats.stream().limit(duty.peopleNeeded() - selected.size()).toList());
            }
            selected.forEach(member -> { assignedToday.add(member); dailyCounts.merge(member, 1, Integer::sum); });
            assignments.put(duty, new RosterAssignment(date, duty.name(), selected, duty.peopleNeeded() - selected.size()));
        }
        List<RosterAssignment> roster = workspace.duties().stream().map(assignments::get).toList();
        workspace.history().addAll(roster);
        return roster;
    }

    private List<String> eligibleMembersFor(Duty duty, List<AttendanceRecord> attendees) {
        return attendees.stream().filter(record -> duty.isEligible(record.status()))
                .map(AttendanceRecord::memberName).toList();
    }

    private List<String> ordered(List<String> members, String dutyName, Map<String, Integer> dailyCounts, List<RosterAssignment> history) {
        return members.stream().sorted(Comparator.<String>comparingInt(dailyCounts::get)
                .thenComparingInt(member -> priorAssignments(member, dutyName, history)).thenComparing(String::compareToIgnoreCase)).toList();
    }

    private int priorAssignments(String member, String dutyName, List<RosterAssignment> history) {
        return (int) history.stream().filter(item -> item.dutyName().equals(dutyName) && item.members().contains(member)).count();
    }
}
