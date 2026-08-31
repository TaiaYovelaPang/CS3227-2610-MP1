package teamsync;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Serialises the local workspace so it survives application restarts. */
public final class WorkspaceStore {
    private static final String DEFAULT_WORKSPACE_RESOURCE = "/teamsync/default-workspace.properties";
    private final Path savePath;

    public WorkspaceStore() {
        this(Path.of(System.getProperty("user.home"), ".teamsync-mvp-workspace.ser"));
    }

    WorkspaceStore(Path savePath) { this.savePath = savePath; }

    public Workspace load() {
        if (!Files.exists(savePath)) return newDefaultWorkspace();
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(savePath))) {
            return (Workspace) input.readObject();
        } catch (IOException | ClassNotFoundException ignored) {
            return newDefaultWorkspace();
        }
    }

    public void save(Workspace workspace) throws IOException {
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(savePath))) {
            output.writeObject(workspace);
        }
    }

    /** Creates an otherwise-empty workspace with an optional release-configured attendance-sheet URL. */
    private Workspace newDefaultWorkspace() {
        Workspace workspace = new Workspace();
        try (InputStream input = WorkspaceStore.class.getResourceAsStream(DEFAULT_WORKSPACE_RESOURCE)) {
            if (input == null) return workspace;
            Properties properties = new Properties();
            properties.load(input);
            workspace.setSheetUrl(properties.getProperty("sheetUrl", "").trim());
        } catch (IOException ignored) {
            // A missing or unreadable optional default must not prevent TeamSync from starting.
        }
        return workspace;
    }
}
