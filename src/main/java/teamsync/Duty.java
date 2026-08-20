package teamsync;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** A task that must be covered for a training session. */
public final class Duty implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private final UUID id;
    private String name;
    private int peopleNeeded;

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

    public void rename(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("A duty name is required.");
        this.name = name.trim();
    }

    public void setPeopleNeeded(int peopleNeeded) {
        if (peopleNeeded < 1) throw new IllegalArgumentException("At least one person is required.");
        this.peopleNeeded = peopleNeeded;
    }
}
