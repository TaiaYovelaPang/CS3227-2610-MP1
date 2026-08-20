package teamsync;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Serialises the local workspace so it survives application restarts. */
public final class WorkspaceStore {
    private final Path savePath;

    public WorkspaceStore() {
        this(Path.of(System.getProperty("user.home"), ".teamsync-mvp-workspace.ser"));
    }

    WorkspaceStore(Path savePath) { this.savePath = savePath; }

    public Workspace load() {
        if (!Files.exists(savePath)) return new Workspace();
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(savePath))) {
            return (Workspace) input.readObject();
        } catch (IOException | ClassNotFoundException ignored) {
            return new Workspace();
        }
    }

    public void save(Workspace workspace) throws IOException {
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(savePath))) {
            output.writeObject(workspace);
        }
    }
}
