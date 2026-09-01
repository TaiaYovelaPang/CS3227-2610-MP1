package teamsync;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Selects and launches the TeamSync runtime bundled for the current platform.
 *
 * <p>The bootstrap itself depends only on the JDK. Each supported JavaFX runtime
 * is stored as a nested executable JAR so that native libraries with identical
 * names from different processor architectures do not overwrite one another.
 */
public final class TeamSyncBootstrap {
    private static final String RUNTIME_ROOT = "/META-INF/teamsync/platforms/";

    private TeamSyncBootstrap() { }

    public static void main(String[] args) {
        Path runtimeJar = null;
        int exitCode;
        try {
            String platform = detectPlatform();
            runtimeJar = extractRuntime(platform);
            exitCode = launchRuntime(runtimeJar, args);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("TeamSync startup was interrupted.");
            exitCode = 1;
        } catch (IOException | IllegalStateException exception) {
            System.err.println("Unable to start TeamSync: " + exception.getMessage());
            exitCode = 1;
        } finally {
            if (runtimeJar != null) {
                try {
                    Files.deleteIfExists(runtimeJar);
                } catch (IOException ignored) {
                    runtimeJar.toFile().deleteOnExit();
                }
            }
        }
        if (exitCode != 0) System.exit(exitCode);
    }

    private static String detectPlatform() {
        return detectPlatform(System.getProperty("os.name", ""), System.getProperty("os.arch", ""));
    }

    static String detectPlatform(String operatingSystemName, String architectureName) {
        String operatingSystem = operatingSystemName.toLowerCase(Locale.ROOT);
        String architecture = architectureName.toLowerCase(Locale.ROOT);
        boolean x64 = architecture.equals("amd64") || architecture.equals("x86_64")
                || architecture.equals("x64");
        boolean arm64 = architecture.equals("aarch64") || architecture.equals("arm64");

        if (operatingSystem.contains("win")) {
            if (x64) return "windows-x64";
            if (arm64) {
                throw new IllegalStateException("Windows ARM is not supplied by JavaFX 25. "
                        + "Install an x64 Java 25 runtime and run TeamSync using that Java installation.");
            }
        } else if (operatingSystem.contains("mac")) {
            if (arm64) return "macos-aarch64";
            if (x64) return "macos-x64";
        } else if (operatingSystem.contains("linux")) {
            if (arm64) return "linux-aarch64";
            if (x64) return "linux-x64";
        }

        throw new IllegalStateException("unsupported operating system or processor: "
                + operatingSystemName + " / " + architectureName);
    }

    private static Path extractRuntime(String platform) throws IOException {
        String resourceName = RUNTIME_ROOT + platform + ".jar";
        Path runtimeJar = Files.createTempFile("teamsync-" + platform + "-", ".jar");
        try (InputStream input = TeamSyncBootstrap.class.getResourceAsStream(resourceName)) {
            if (input == null) {
                Files.deleteIfExists(runtimeJar);
                throw new IOException("the bundled " + platform + " runtime is missing");
            }
            Files.copy(input, runtimeJar, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            Files.deleteIfExists(runtimeJar);
            throw exception;
        }
        return runtimeJar;
    }

    private static int launchRuntime(Path runtimeJar, String[] args)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.add("--enable-native-access=ALL-UNNAMED");
        command.add("-jar");
        command.add(runtimeJar.toString());
        command.addAll(List.of(args));
        return new ProcessBuilder(command).inheritIO().start().waitFor();
    }

    private static String javaExecutable() {
        String executable = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                ? "java.exe" : "java";
        Path bundledJava = Path.of(System.getProperty("java.home"), "bin", executable);
        return Files.isRegularFile(bundledJava) ? bundledJava.toString() : executable;
    }
}
