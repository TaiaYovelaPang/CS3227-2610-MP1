package teamsync;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/** A dated team event, optionally with a lead-time reminder. */
public final class ImportantDate implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private String name;
    private LocalDate date;
    private LocalTime time;
    private LocalTime endTime;
    private ReminderOption reminderOption;
    private int customReminderMinutes;

    public ImportantDate(String name, LocalDate date, LocalTime time, ReminderOption reminderOption, int customReminderMinutes) {
        this(name, date, time, defaultEndTime(time), reminderOption, customReminderMinutes);
    }

    public ImportantDate(String name, LocalDate date, LocalTime time, LocalTime endTime,
                         ReminderOption reminderOption, int customReminderMinutes) {
        update(name, date, time, endTime, reminderOption, customReminderMinutes);
    }

    public String name() { return name; }
    public LocalDate date() { return date; }
    public LocalTime time() { return time; }
    /** Returns the end time; older saved events without one are treated as one-hour events. */
    public LocalTime endTime() { return endTime == null ? defaultEndTime(time) : endTime; }
    public ReminderOption reminderOption() { return reminderOption; }
    public int customReminderMinutes() { return customReminderMinutes; }
    public int customReminderHours() { return customReminderMinutes / 60; }
    public boolean hasReminder() { return reminderOption != null; }
    public LocalDateTime occursAt() { return LocalDateTime.of(date, time); }
    public LocalDateTime endsAt() { return LocalDateTime.of(date, endTime()); }
    public int reminderMinutesBefore() { return hasReminder() ? reminderOption.minutesBefore(customReminderMinutes) : 0; }

    public void update(String name, LocalDate date, LocalTime time, ReminderOption reminderOption, int customReminderMinutes) {
        update(name, date, time, defaultEndTime(time), reminderOption, customReminderMinutes);
    }

    public void update(String name, LocalDate date, LocalTime time, LocalTime endTime,
                       ReminderOption reminderOption, int customReminderMinutes) {
        String cleanedName = Objects.requireNonNull(name, "Event name is required.").trim();
        if (cleanedName.isEmpty()) throw new IllegalArgumentException("Enter an event name.");
        this.name = cleanedName;
        this.date = Objects.requireNonNull(date, "Choose an event date.");
        this.time = Objects.requireNonNull(time, "Choose an event time.");
        this.endTime = Objects.requireNonNull(endTime, "Choose an event end time.");
        if (!this.endTime.isAfter(this.time)) {
            throw new IllegalArgumentException("The event end time must be after its start time.");
        }
        if (reminderOption == ReminderOption.CUSTOM && customReminderMinutes < 1) {
            throw new IllegalArgumentException("A custom reminder must be at least 1 hour before the event.");
        }
        this.reminderOption = reminderOption;
        this.customReminderMinutes = reminderOption == ReminderOption.CUSTOM ? customReminderMinutes : 0;
    }

    private static LocalTime defaultEndTime(LocalTime startTime) {
        LocalTime oneHourLater = Objects.requireNonNull(startTime, "Choose an event time.").plusHours(1);
        return oneHourLater.isAfter(startTime) ? oneHourLater : LocalTime.MAX;
    }

    public String reminderLabel() {
        if (!hasReminder()) return "Off";
        return reminderOption == ReminderOption.CUSTOM ? "Custom: " + customReminderHours() + " hour(s) before" : reminderOption.toString();
    }
}
