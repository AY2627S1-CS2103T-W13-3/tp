package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.abort;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.core.GuiSettings;
import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.Person;

/** Exercises real packaged startup without a stage; diagnostic logging remains a separate integration task. */
public class JarHomeStorageIntegrationTest {
    @TempDir
    public Path testFolder;

    @Test
    public void init_jarPathHasSpacesPercentAndUnicode_createsCoreFilesBesideJar() throws Exception {
        Path jar = createApplicationJar("app home % 新加坡");
        // Keep launcher arguments ASCII while retaining the Unicode code source and a different caller folder.
        Path workingFolder = Files.createDirectory(jar.getParent().resolve("other working folder"));

        runProbe(workingFolder.relativize(jar), workingFolder, "create", "CREATED");

        assertTrue(Files.exists(jar.getParent().resolve("data/addressbook.json")));
        assertTrue(Files.exists(jar.getParent().resolve("preferences.json")));
        assertTrue(Files.exists(jar.getParent().resolve(".policyharbor.lock")));
        assertNoCoreFiles(workingFolder);
    }

    @Test
    public void init_restartFromOtherFolder_usesJarDataPreferencesAndNextId() throws Exception {
        Path jar = createApplicationJar("application");
        runProbe(jar, jar.getParent(), "create", "CREATED");
        Path jarData = jar.getParent().resolve("data/addressbook.json");
        byte[] savedData = Files.readAllBytes(jarData);
        Path workingFolder = Files.createDirectory(testFolder.resolve("previous working folder"));
        Path oldData = Files.createDirectories(workingFolder.resolve("data")).resolve("addressbook.json");
        Path oldPreferences = workingFolder.resolve("preferences.json");
        byte[] dataCanary = "{unrelated working-folder data}\n".getBytes(StandardCharsets.UTF_8);
        byte[] preferencesCanary = "null\n".getBytes(StandardCharsets.UTF_8);
        Files.write(oldData, dataCanary);
        Files.write(oldPreferences, preferencesCanary);

        runProbe(jar, workingFolder, "verify-one", "VERIFIED");
        assertArrayEquals(savedData, Files.readAllBytes(jarData));
        runProbe(jar, workingFolder, "verify-and-add", "VERIFIED");
        runProbe(jar, jar.getParent(), "verify-two", "VERIFIED");

        assertArrayEquals(dataCanary, Files.readAllBytes(oldData));
        assertArrayEquals(preferencesCanary, Files.readAllBytes(oldPreferences));
        assertFalse(Files.exists(workingFolder.resolve(".policyharbor.lock")));
    }

    @Test
    public void init_writableJarHomeWithUnwritableWorkingFolder_startsAndSavesBesideJar() throws Exception {
        Path jar = createApplicationJar("writable application");
        Path workingFolder = Files.createDirectory(testFolder.resolve("read only caller"));
        if (Files.getFileAttributeView(workingFolder, PosixFileAttributeView.class) == null) {
            abort("POSIX directory permissions are unavailable on this filesystem");
        }
        Set<PosixFilePermission> originalPermissions = Files.getPosixFilePermissions(workingFolder);
        try {
            Files.setPosixFilePermissions(workingFolder, PosixFilePermissions.fromString("r-x------"));
            if (Files.isWritable(workingFolder)) {
                abort("This account bypasses POSIX write restrictions");
            }
            runProbe(jar, workingFolder, "create", "CREATED");
            assertTrue(Files.exists(jar.getParent().resolve("data/addressbook.json")));
            assertTrue(Files.exists(jar.getParent().resolve("preferences.json")));
            assertNoCoreFiles(workingFolder);
        } finally {
            Files.setPosixFilePermissions(workingFolder, originalPermissions);
        }
    }

    @Test
    public void init_sameJarDifferentWorkingFolders_sharesLockButOtherJarIsIndependent() throws Exception {
        Path jar = createApplicationJar("first application");
        Path otherJar = createApplicationJar("second application");
        Path firstWorkingFolder = Files.createDirectory(testFolder.resolve("first caller"));
        Path secondWorkingFolder = Files.createDirectory(testFolder.resolve("second caller"));
        try (PackagedProcess owner = startProbe(jar, firstWorkingFolder, "hold")) {
            assertEquals("READY", owner.readStatus(), owner.diagnostics());
            runProbe(jar, secondWorkingFolder, "inspect", "BLOCKED");
            runProbe(otherJar, secondWorkingFolder, "inspect", "READY");
            owner.requestStop();
            assertEquals("STOPPED", owner.readStatus(), owner.diagnostics());
            assertEquals(0, owner.waitForExit(), owner.diagnostics());
        }
        runProbe(jar, secondWorkingFolder, "inspect", "READY");
        assertNoCoreFiles(firstWorkingFolder);
        assertNoCoreFiles(secondWorkingFolder);
    }

    @Test
    public void init_invalidJarHomeData_entersProtectedModeWithoutRewritingIt() throws Exception {
        Path jar = createApplicationJar("protected application");
        Path data = Files.createDirectories(jar.getParent().resolve("data")).resolve("addressbook.json");
        byte[] original = "{invalid saved client data}\n".getBytes(StandardCharsets.UTF_8);
        Files.write(data, original);
        Path workingFolder = Files.createDirectory(testFolder.resolve("protected caller"));

        runProbe(jar, workingFolder, "protected", "PROTECTED");

        assertArrayEquals(original, Files.readAllBytes(data));
        assertNoCoreFiles(workingFolder);
    }

    @Test
    public void execute_saveFailsBesideJar_preservesClientSelectionAndSavedBytes() throws Exception {
        Path jar = createApplicationJar("save failure application");
        runProbe(jar, jar.getParent(), "create", "CREATED");
        Path data = jar.getParent().resolve("data/addressbook.json");
        byte[] original = Files.readAllBytes(data);
        Path workingFolder = Files.createDirectory(testFolder.resolve("save failure caller"));

        runProbe(jar, workingFolder, "save-failure", "UNCHANGED");

        assertArrayEquals(original, Files.readAllBytes(data));
        assertNoCoreFiles(workingFolder);
    }

    private void assertNoCoreFiles(Path folder) {
        assertFalse(Files.exists(folder.resolve("data/addressbook.json")));
        assertFalse(Files.exists(folder.resolve("preferences.json")));
        assertFalse(Files.exists(folder.resolve(".policyharbor.lock")));
    }

    /** Packages current production classes first, retaining the test runtime only for dependencies. */
    private Path createApplicationJar(String folderName) throws Exception {
        Path jar = Files.createDirectory(testFolder.resolve(folderName)).resolve("PolicyHarbor.jar");
        Path classes = Path.of(MainApp.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        assertTrue(Files.isDirectory(classes), "This regression packages the compiled production class directory");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, Probe.class.getName());
        manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, runtimeClasspath());
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar), manifest);
                var paths = Files.walk(classes)) {
            for (Path file : paths.filter(Files::isRegularFile).toList()) {
                String entry = classes.relativize(file).toString().replace(File.separatorChar, '/');
                output.putNextEntry(new JarEntry(entry));
                Files.copy(file, output);
                output.closeEntry();
            }
            String probeEntry = Probe.class.getName().replace('.', '/') + ".class";
            output.putNextEntry(new JarEntry(probeEntry));
            try (var input = Probe.class.getResourceAsStream("/" + probeEntry)) {
                input.transferTo(output);
            }
            output.closeEntry();
        }
        return jar;
    }

    private static String runtimeClasspath() throws Exception {
        Set<String> entries = new LinkedHashSet<>();
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            entries.add(Path.of(entry).toAbsolutePath().toUri().toString());
        }
        for (ClassLoader loader = Thread.currentThread().getContextClassLoader(); loader != null;
                loader = loader.getParent()) {
            if (loader instanceof URLClassLoader urlLoader) {
                for (URL url : urlLoader.getURLs()) {
                    if (url.getProtocol().equals("file")) {
                        entries.add(Path.of(url.toURI()).toAbsolutePath().toUri().toString());
                    }
                }
            }
        }
        return String.join(" ", entries);
    }

    private PackagedProcess startProbe(Path jar, Path workingFolder, String mode) throws IOException {
        String javaName = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", javaName);
        Path diagnostics = Files.createTempFile(testFolder, "child-", ".log");
        Path expectedJar = workingFolder.resolve(jar).normalize();
        List<String> command = List.of(java.toString(), "-ea", "-jar", jar.toString(),
                mode, expectedJar.toUri().toASCIIString());
        Process process = new ProcessBuilder(command).directory(workingFolder.toFile())
                .redirectError(diagnostics.toFile()).start();
        return new PackagedProcess(process, diagnostics);
    }

    private void runProbe(Path jar, Path workingFolder, String mode, String expected) throws Exception {
        try (PackagedProcess child = startProbe(jar, workingFolder, mode)) {
            assertEquals(expected, child.readStatus(), child.diagnostics());
            assertEquals(0, child.waitForExit(), child.diagnostics());
        }
    }

    /** Bounds every child wait and kills a remaining process even when a test assertion fails. */
    private static final class PackagedProcess implements AutoCloseable {
        private final Process process;
        private final Path log;
        private final BufferedReader output;

        private PackagedProcess(Process process, Path log) {
            this.process = process;
            this.log = log;
            output = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        }

        private String readStatus() throws Exception {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return output.readLine();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).get(20, TimeUnit.SECONDS);
        }

        private String diagnostics() throws IOException {
            return Files.readString(log);
        }

        private void requestStop() throws IOException {
            process.getOutputStream().write('\n');
            process.getOutputStream().flush();
        }

        private int waitForExit() throws InterruptedException {
            assertTrue(process.waitFor(20, TimeUnit.SECONDS), "Packaged probe did not exit");
            return process.exitValue();
        }

        @Override
        public void close() throws Exception {
            if (process.isAlive()) {
                process.destroyForcibly();
                assertTrue(process.waitFor(20, TimeUnit.SECONDS), "Packaged probe could not be terminated");
            }
            process.getOutputStream().close();
            output.close();
        }
    }

    /** Runs real MainApp startup/commands/shutdown from an executable JAR without opening JavaFX windows. */
    public static class Probe {
        private static final String ADD_RACHEL = "add n/Rachel Lim p/001 e/rachel@example.com a/Blk 123";

        public static void main(String[] args) throws Exception {
            Path loadedJar = Path.of(MainApp.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            require(Files.isSameFile(loadedJar, Path.of(URI.create(args[1]))),
                    "Production classes did not load from the JAR");
            String workingDirectory = System.getProperty("user.dir");
            MainApp app = new MainApp();
            try {
                app.init();
                require(workingDirectory.equals(System.getProperty("user.dir")), "Startup changed user.dir");
                if (app.model == null) {
                    require(app.storage == null && app.logic == null && app.ui == null, "Startup partly initialized");
                    System.out.println("BLOCKED");
                    return;
                }
                switch (args[0]) {
                    case "create":
                        require(app.model.getAddressBook().getPersonList().isEmpty(),
                                "Imported another folder's clients");
                        app.logic.execute(ADD_RACHEL);
                        app.logic.execute("followup 1 d/2099-12-15 m/Call about renewal");
                        app.model.setGuiSettings(new GuiSettings(913, 713, 12, 34));
                        System.out.println("CREATED");
                        break;
                    case "verify-and-add":
                        verifySavedClient(app);
                        app.logic.execute("add n/Bob Tan p/002 e/bob@example.com a/Blk 2");
                        require(app.model.getAddressBook().getPersonById(new ClientId(2)).isPresent(),
                                "Next ID was lost");
                        System.out.println("VERIFIED");
                        break;
                    case "verify-one":
                        verifySavedClient(app);
                        require(app.model.getAddressBook().getPersonList().size() == 1,
                                "Startup changed saved clients");
                        require(app.model.getAddressBook().getNextClientId() == 2, "Startup changed ID allocation");
                        app.logic.execute("view 1");
                        System.out.println("VERIFIED");
                        break;
                    case "verify-two":
                        verifySavedClient(app);
                        require(app.model.getAddressBook().getPersonList().size() == 2, "New client was not saved");
                        require(app.model.getAddressBook().getNextClientId() == 3, "Saved ID counter was lost");
                        System.out.println("VERIFIED");
                        break;
                    case "protected":
                        require(app.model.isDataLoadingBlocked(), "Invalid JAR-home data was ignored");
                        verifyProtectedMutation(app);
                        System.out.println("PROTECTED");
                        break;
                    case "save-failure":
                        verifySaveFailure(app);
                        System.out.println("UNCHANGED");
                        break;
                    case "hold":
                        System.out.println("READY");
                        new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
                        break;
                    case "inspect":
                        System.out.println("READY");
                        break;
                    default:
                        throw new AssertionError("Unknown probe mode");
                }
            } finally {
                app.stop();
            }
            if (args[0].equals("hold")) {
                System.out.println("STOPPED");
            }
        }

        private static void verifySavedClient(MainApp app) {
            Person rachel = app.model.getAddressBook().getPersonById(new ClientId(1)).orElseThrow();
            require(rachel.getName().fullName.equals("Rachel Lim"), "Loaded another folder's client");
            require(rachel.getFollowUp().orElseThrow().getDescription().equals("Call about renewal"),
                    "Follow-up did not survive restart");
            require(app.model.getUserPrefs().getGuiSettings().equals(new GuiSettings(913, 713, 12, 34)),
                    "Preferences did not follow the JAR");
        }

        /** A directory at the save destination produces a portable write failure without OS permission assumptions. */
        private static void verifySaveFailure(MainApp app) throws Exception {
            verifySavedClient(app);
            app.logic.execute("view 1");
            Person selected = app.model.selectedPersonProperty().get();
            long nextId = app.model.getAddressBook().getNextClientId();
            Path data = app.storage.getAddressBookFilePath();
            byte[] original = Files.readAllBytes(data);
            Path backup = data.resolveSibling("original-addressbook.json");
            Files.move(data, backup);
            try {
                Files.createDirectory(data);
                requireSaveFailure(app, "edit 1 p/999");
                require(app.model.selectedPersonProperty().get() == selected, "Failed edit changed selection");
                require(app.model.getAddressBook().getPersonById(new ClientId(1)).orElseThrow() == selected,
                        "Failed edit changed the client");
                require(app.model.getAddressBook().getNextClientId() == nextId, "Failed edit changed ID allocation");
                require(Arrays.equals(original, Files.readAllBytes(backup)), "Failed save changed original bytes");
            } finally {
                Files.deleteIfExists(data);
                Files.move(backup, data);
            }
        }

        private static void verifyProtectedMutation(MainApp app) throws Exception {
            try {
                app.logic.execute(ADD_RACHEL);
                throw new AssertionError("Protected startup allowed a mutation");
            } catch (ParseException expected) {
                require(Messages.MESSAGE_CHANGES_BLOCKED.equals(expected.getMessage()),
                        "Mutation failed for a reason other than protected loading");
            }
        }

        private static void requireSaveFailure(MainApp app, String command) throws Exception {
            try {
                app.logic.execute(command);
                throw new AssertionError("Mutation unexpectedly succeeded");
            } catch (CommandException expected) {
                require(expected.getCause() instanceof IOException, "Command failed before reaching storage");
                require(String.format(LogicManager.MESSAGE_SAVE_FAILED, app.storage.getAddressBookFilePath())
                        .equals(expected.getMessage()), "Failed save reported the wrong recovery guidance");
            }
        }

        private static void require(boolean isSatisfied, String message) {
            if (!isSatisfied) {
                throw new AssertionError(message);
            }
        }
    }
}
