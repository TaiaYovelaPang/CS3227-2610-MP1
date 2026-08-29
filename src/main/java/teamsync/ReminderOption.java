package teamsync;

/** The lead time for an in-app reminder attached to an important date. */
public enum ReminderOption {
    AT_EVENT_TIME("At event time", 0),
    FIFTEEN_MINUTES_BEFORE("15 minutes before", 15),
    ONE_DAY_BEFORE("1 day before", 24 * 60),
    CUSTOM("Custom", -1);

    private final String label;
    private final int minutesBefore;

    ReminderOption(String label, int minutesBefore) {
        this.label = label;
        this.minutesBefore = minutesBefore;
    }

    public int minutesBefore(int customMinutes) {
        return this == CUSTOM ? customMinutes : minutesBefore;
    }

    @Override public String toString() { return label; }
}
