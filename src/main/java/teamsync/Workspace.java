package teamsync;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Mutable aggregate root for the saved TeamSync workspace. */
public final class Workspace implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private String sheetUrl = "";
    private LocalDate sessionDate = LocalDate.now();
    private List<AttendanceRecord> attendance = new ArrayList<>();
    private final List<Duty> duties = new ArrayList<>();
    private final List<RosterAssignment> history = new ArrayList<>();
    // Not final so workspaces saved before important dates were introduced can initialise it after deserialisation.
    private List<ImportantDate> importantDates = new ArrayList<>();

    public String sheetUrl() { return sheetUrl; }
    public void setSheetUrl(String sheetUrl) { this.sheetUrl = sheetUrl; }
    public LocalDate sessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }
    public List<AttendanceRecord> attendance() { return List.copyOf(attendance); }
    public void setAttendance(List<AttendanceRecord> attendance) { this.attendance = new ArrayList<>(attendance); }
    public List<Duty> duties() { return duties; }
    public List<RosterAssignment> history() { return history; }
    public List<ImportantDate> importantDates() {
        if (importantDates == null) importantDates = new ArrayList<>();
        return importantDates;
    }
}
