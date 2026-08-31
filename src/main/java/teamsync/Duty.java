package teamsync;

import java.io.Serial;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** A task that must be covered for a training session. */
public final class Duty implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private final UUID id;
    private String name;
    private int peopleNeeded;
    // Not final so duties saved before eligibility choices were introduced retain the on-time default.
    private Set<AttendanceStatus> eligibleStatuses = EnumSet.of(AttendanceStatus.ON_TIME);

    public Duty(String name, int peopleNeeded) {
        this(UUID.randomUUID(), name, peopleNeeded);
    }

    public Duty(UUID id, String name, int peopleNeeded) {
        this.id = Objects.requireNonNull(id);
        rename(name);
        setPeopleNeeded(peopleNeeded);
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public int peopleNeeded() { return peopleNeeded; }
    public Set<AttendanceStatus> eligibleStatuses() {
        if (eligibleStatuses == null) eligibleStatuses = EnumSet.of(AttendanceStatus.ON_TIME);
        return Set.copyOf(eligibleStatuses);
    }

    public boolean isEligible(AttendanceStatus status) {
        return status != AttendanceStatus.ABSENT && eligibleStatuses().contains(status);
    }

    public void rename(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("A duty name is required.");
        this.name = name.trim();
    }

    public void setPeopleNeeded(int peopleNeeded) {
        if (peopleNeeded < 1) throw new IllegalArgumentException("At least one person is required.");
        this.peopleNeeded = peopleNeeded;
    }

    public void setEligibleStatuses(Set<AttendanceStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            throw new IllegalArgumentException("Select at least one attendance status for this duty.");
        }
        if (statuses.contains(AttendanceStatus.ABSENT)) {
            throw new IllegalArgumentException("Absent members cannot be eligible for a duty.");
        }
        this.eligibleStatuses = EnumSet.copyOf(statuses);
    }
}
