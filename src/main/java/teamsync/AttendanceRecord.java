package teamsync;

import java.io.Serial;
import java.io.Serializable;

/** A member's attendance value for one selected training session. */
public final class AttendanceRecord implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private final String memberName;
    private final double value;

    public AttendanceRecord(String memberName, double value) {
        this.memberName = memberName;
        this.value = value;
    }

    public String memberName() { return memberName; }
    public double value() { return value; }
    public boolean isConfirmed() { return value == 1.0; }
}
