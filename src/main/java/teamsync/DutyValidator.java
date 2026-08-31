package teamsync;

import java.util.List;
import java.util.UUID;

/** Validation rules that apply to the workspace's complete duty list. */
public final class DutyValidator {
    private DutyValidator() { }

    /**
     * Rejects another duty with the same trimmed name, ignoring letter case.
     * Deliberately does not use fuzzy matching: differently spelled duties remain distinct.
     */
    public static void ensureUniqueName(List<Duty> duties, String proposedName, UUID dutyBeingEdited) {
        if (proposedName == null) return; // Duty.rename provides the required-name validation.
        String candidate = proposedName.trim();
        boolean duplicate = duties.stream()
                .anyMatch(duty -> !duty.id().equals(dutyBeingEdited) && duty.name().equalsIgnoreCase(candidate));
        if (duplicate) {
            throw new IllegalArgumentException("A duty with this name already exists. Update its people needed instead.");
        }
    }
}
