package teamsync;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

/** Validates scheduling rules that apply across a workspace's important dates. */
public final class ImportantDateValidator {
    private ImportantDateValidator() { }

    /**
     * Validates an event before it is added or updated. The event being edited may be supplied as
     * {@code eventBeingEdited} so it is not compared with itself.
     */
    public static void validate(ImportantDate candidate, Collection<ImportantDate> existingEvents,
                                ImportantDate eventBeingEdited, LocalDateTime now) {
        Objects.requireNonNull(candidate, "Event is required.");
        Objects.requireNonNull(existingEvents, "Existing events are required.");
        Objects.requireNonNull(now, "Current time is required.");

        if (candidate.occursAt().isBefore(now)) {
            throw new IllegalArgumentException("Choose a future date and time for the event.");
        }

        String candidateName = normaliseName(candidate.name());
        for (ImportantDate event : existingEvents) {
            if (event == eventBeingEdited) continue;
            if (normaliseName(event.name()).equals(candidateName)) {
                throw new IllegalArgumentException("An event named \"" + candidate.name()
                        + "\" already exists. Event names must be unique.");
            }
            if (candidate.occursAt().isBefore(event.endsAt()) && event.occursAt().isBefore(candidate.endsAt())) {
                throw new IllegalArgumentException("This event overlaps with \"" + event.name() + "\" ("
                        + event.occursAt().format(java.time.format.DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm"))
                        + "–" + event.endTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                        + "). Choose a different time.");
            }
        }
    }

    private static String normaliseName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
