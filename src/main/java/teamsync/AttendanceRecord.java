package teamsync;

import java.io.Serial;
import java.io.Serializable;

/** A member's attendance state for one selected training session. */
public final class AttendanceRecord implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private final String memberName;
    private final double value;
    private final AttendanceStatus status;

    public AttendanceRecord(String memberName, double value) {
        this(memberName, value == 1.0 ? AttendanceStatus.ON_TIME : AttendanceStatus.ABSENT);
    }

    public AttendanceRecord(String memberName, AttendanceStatus status) {
        this.memberName = memberName;
        this.status = status;
        // Keep the old numeric field for previously saved workspaces. Every form of attendance is 1.
        this.value = status.attended() ? 1.0 : 0.0;
    }

    public String memberName() { return memberName; }
    /** Compatibility attendance indicator: 1 for on-time, late, or early-leaving members; otherwise 0. */
    public double value() { return status().attended() ? 1.0 : 0.0; }
    public AttendanceStatus status() {
        // Workspaces saved before attendance states were added only contain value.
        return status == null ? (value == 1.0 ? AttendanceStatus.ON_TIME : AttendanceStatus.ABSENT) : status;
    }
    public boolean isConfirmed() { return status().isEligibleForFullSessionDuty(); }
    public boolean attended() { return status().attended(); }
}
