package seedu.address.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.abort;

import java.io.IOException;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies packaged and development home paths without changing process-wide state. */
public class ApplicationHomeTest {
    @TempDir
    public Path testFolder;

    @Test
    public void resolveHomeDirectory_regularFileWithOtherExtension_usesContainingDirectory() throws IOException {
        Path jarFolder = Files.createDirectory(testFolder.resolve("packaged"));
        Path codeLocation = Files.createFile(jarFolder.resolve("PolicyHarbor.binary"));
        Path workingDirectory = Files.createDirectory(testFolder.resolve("working"));

        assertEquals(jarFolder.toRealPath(),
                ApplicationHome.resolveHomeDirectory(codeLocation.toUri(), workingDirectory));
    }

    @Test
    public void resolveHomeDirectory_encodedUri_preservesLiteralPathCharacters() throws IOException {
        Path jarFolder = Files.createDirectory(testFolder.resolve("Policy Harbor 新加坡 %t %h %g %20"));
        Path jar = Files.createFile(jarFolder.resolve("PolicyHarbor %20.jar"));

        assertEquals(jarFolder.toRealPath(),
                ApplicationHome.resolveHomeDirectory(jar.toUri(), testFolder));
    }

    @Test
    public void resolveHomeDirectory_directoryNamedJar_usesDevelopmentWorkingDirectory() throws IOException {
        Path classes = Files.createDirectory(testFolder.resolve("classes.jar"));
        Path workingDirectory = Files.createDirectory(testFolder.resolve("working"));

        assertEquals(workingDirectory.toAbsolutePath().normalize(),
                ApplicationHome.resolveHomeDirectory(classes.toUri(), workingDirectory));
    }

    @Test
    public void resolveHomeDirectory_developmentWithRelativeWorkingDirectory_returnsNormalizedAbsolutePath()
            throws IOException {
        Path classes = Files.createDirectory(testFolder.resolve("classes"));
        Path workingDirectory = Path.of("unused-parent", "..", "working folder");

        assertEquals(Path.of("working folder").toAbsolutePath().normalize(),
                ApplicationHome.resolveHomeDirectory(classes.toUri(), workingDirectory));
    }

    @Test
    public void resolveHomeDirectory_missingCodeLocation_throwsInsteadOfUsingWorkingDirectory() {
        Path missingJar = testFolder.resolve("missing.jar");

        assertThrows(IOException.class, () ->
                ApplicationHome.resolveHomeDirectory(missingJar.toUri(), testFolder));
        assertFalse(Files.exists(missingJar));
    }

    @Test
    public void resolveHomeDirectory_nonFileUri_throwsInsteadOfUsingWorkingDirectory() {
        for (URI codeLocation : List.of(URI.create("https://example.com/PolicyHarbor.jar"),
                URI.create("jar:file:/PolicyHarbor.jar!/"))) {
            assertThrows(IllegalArgumentException.class, () ->
                    ApplicationHome.resolveHomeDirectory(codeLocation, testFolder));
        }
    }

    @Test
    public void resolveHomeDirectory_symbolicLink_usesPhysicalJarDirectory() throws IOException {
        Path jarFolder = Files.createDirectory(testFolder.resolve("physical"));
        Path jar = Files.createFile(jarFolder.resolve("PolicyHarbor.jar"));
        Path aliasFolder = Files.createDirectory(testFolder.resolve("alias"));
        Path alias = aliasFolder.resolve("PolicyHarbor.jar");
        try {
            Files.createSymbolicLink(alias, jar);
        } catch (UnsupportedOperationException | SecurityException e) {
            abort("Symbolic link creation is unavailable: " + e);
        } catch (FileSystemException e) {
            if (e instanceof AccessDeniedException
                    || "A required privilege is not held by the client".equals(e.getReason())) {
                abort("Symbolic link creation is not permitted: " + e);
            }
            throw e;
        }

        assertEquals(jarFolder.toRealPath(),
                ApplicationHome.resolveHomeDirectory(alias.toUri(), aliasFolder));
    }

    @Test
    public void resolveHomeDirectory_packagedAndDevelopment_doesNotCreateFilesOrChangeUserDirectory()
            throws IOException {
        Path jar = Files.writeString(testFolder.resolve("PolicyHarbor.jar"), "unchanged code source");
        Path classes = Files.createDirectory(testFolder.resolve("classes"));
        Path workingDirectory = testFolder.resolve("not-created");
        List<Path> originalPaths = pathsInTestFolder();
        String originalUserDirectory = System.getProperty("user.dir");

        ApplicationHome.resolveHomeDirectory(jar.toUri(), workingDirectory);
        ApplicationHome.resolveHomeDirectory(classes.toUri(), workingDirectory);

        assertEquals(originalPaths, pathsInTestFolder());
        assertEquals("unchanged code source", Files.readString(jar));
        assertEquals(originalUserDirectory, System.getProperty("user.dir"));
    }

    @Test
    public void getHomeDirectory_explodedClasses_usesWorkingDirectoryWithoutChangingIt() throws Exception {
        Path codeLocation = Path.of(ApplicationHome.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        assertTrue(Files.isDirectory(codeLocation));
        String originalUserDirectory = System.getProperty("user.dir");

        assertEquals(Path.of("").toAbsolutePath().normalize(), ApplicationHome.getHomeDirectory());
        assertEquals(originalUserDirectory, System.getProperty("user.dir"));
    }

    private List<Path> pathsInTestFolder() throws IOException {
        try (Stream<Path> paths = Files.walk(testFolder)) {
            return paths.sorted().toList();
        }
    }
}
