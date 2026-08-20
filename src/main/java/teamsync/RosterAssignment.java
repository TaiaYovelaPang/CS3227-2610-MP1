package teamsync;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/** The selected members for one duty on one date. */
public record RosterAssignment(LocalDate date, String dutyName, List<String> members, int unfilledSlots) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    public RosterAssignment {
        members = List.copyOf(members);
    }
}
