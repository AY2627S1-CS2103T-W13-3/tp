package seedu.address.commons.util;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;

/** Resolves the shared application folder without logging, creating files or changing user.dir. */
public final class ApplicationHome {
    private ApplicationHome() {
    }

    /**
     * Returns the absolute, normalized directory containing the packaged JAR.
     * Symbolic links in the packaged JAR path use the real JAR's directory, sharing its data and lock.
     * Exploded classes (IDE or Gradle) instead use the current working directory.
     *
     * @throws IllegalStateException if the application code location cannot be resolved.
     */
    public static Path getHomeDirectory() {
        ProtectionDomain domain = ApplicationHome.class.getProtectionDomain();
        CodeSource source = domain == null ? null : domain.getCodeSource();
        if (source == null || source.getLocation() == null) {
            throw new IllegalStateException("The PolicyHarbor application location is unavailable.");
        }
        try {
            return resolveHomeDirectory(source.getLocation().toURI(), Path.of(""));
        } catch (IOException | URISyntaxException | IllegalArgumentException e) {
            throw new IllegalStateException("Could not resolve the PolicyHarbor application folder.", e);
        }
    }

    /** Resolves an existing code source; only exploded classes may fall back to the working directory. */
    static Path resolveHomeDirectory(URI codeLocation, Path workingDirectory) throws IOException {
        requireNonNull(codeLocation);
        requireNonNull(workingDirectory);
        if (!"file".equalsIgnoreCase(codeLocation.getScheme())) {
            throw new IllegalArgumentException("The application code location must be a local file URI.");
        }
        Path location = Path.of(codeLocation).toRealPath();
        if (Files.isDirectory(location)) {
            return workingDirectory.toAbsolutePath().normalize();
        }
        if (!Files.isRegularFile(location)) {
            throw new IOException("The application code location is not a regular file or directory.");
        }
        return location.getParent();
    }
}
