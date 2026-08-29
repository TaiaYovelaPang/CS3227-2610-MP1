package teamsync;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** A member-by-duty view of the roster allocations saved for one month. */
public final class MonthlyRosterHistory {
    private final YearMonth month;
    private final List<String> dutyNames;
    private final List<MemberRow> members;

    private MonthlyRosterHistory(YearMonth month, List<String> dutyNames, List<MemberRow> members) {
        this.month = month;
        this.dutyNames = List.copyOf(dutyNames);
        this.members = List.copyOf(members);
    }

    public static MonthlyRosterHistory from(YearMonth month, List<RosterAssignment> history) {
        Set<String> duties = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Map<String, List<LocalDate>>> datesByMember = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        history.stream().filter(assignment -> YearMonth.from(assignment.date()).equals(month)).forEach(assignment -> {
            duties.add(assignment.dutyName());
            assignment.members().forEach(member -> datesByMember
                    .computeIfAbsent(member, ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(assignment.dutyName(), ignored -> new ArrayList<>())
                    .add(assignment.date()));
        });

        List<MemberRow> rows = datesByMember.entrySet().stream()
                .map(entry -> new MemberRow(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(MemberRow::memberName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new MonthlyRosterHistory(month, new ArrayList<>(duties), rows);
    }

    public YearMonth month() { return month; }
    public List<String> dutyNames() { return dutyNames; }
    public List<MemberRow> members() { return members; }

    /** One member's dates for each duty in the selected month. */
    public static final class MemberRow {
        private final String memberName;
        private final Map<String, List<LocalDate>> datesByDuty;

        private MemberRow(String memberName, Map<String, List<LocalDate>> datesByDuty) {
            this.memberName = memberName;
            Map<String, List<LocalDate>> copy = new LinkedHashMap<>();
            datesByDuty.forEach((duty, dates) -> copy.put(duty, dates.stream().sorted().toList()));
            this.datesByDuty = Map.copyOf(copy);
        }

        public String memberName() { return memberName; }
        public List<LocalDate> datesFor(String dutyName) { return datesByDuty.getOrDefault(dutyName, List.of()); }
        public int totalAssignments() { return datesByDuty.values().stream().mapToInt(List::size).sum(); }
    }
}
