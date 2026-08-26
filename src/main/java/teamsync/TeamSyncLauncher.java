package teamsync;

import javafx.application.Application;

/**
 * Launches the JavaFX application without triggering the Java launcher's
 * module-path-only handling for classes that directly extend Application.
 */
public final class TeamSyncLauncher {
    private TeamSyncLauncher() { }

    public static void main(String[] args) {
        Application.launch(TeamSyncApp.class, args);
    }
}
