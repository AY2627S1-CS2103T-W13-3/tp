package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.testutil.JavaChildProcess;

/** Exercises startup exclusion through MainApp.init in independent JVMs, without opening a stage. */
public class MainAppInstanceTest {
    @TempDir
    public Path testFolder;

    @Test
    public void init_sameFolderWhileOwnerRuns_blocksBeforeLoadingOrSaving() throws Exception {
        try (JavaChildProcess owner = JavaChildProcess.start(testFolder, Probe.class, "hold")) {
            assertEquals("READY", owner.readStatus());
            byte[] data = Files.readAllBytes(testFolder.resolve("data/addressbook.json"));
            byte[] preferences = Files.readAllBytes(testFolder.resolve("preferences.json"));

            try (JavaChildProcess duplicate = JavaChildProcess.start(testFolder, Probe.class, "probe")) {
                assertEquals("BLOCKED", duplicate.readStatus());
                assertEquals(0, duplicate.waitForExit());
            }
            assertArrayEquals(data, Files.readAllBytes(testFolder.resolve("data/addressbook.json")));
            assertArrayEquals(preferences, Files.readAllBytes(testFolder.resolve("preferences.json")));
            owner.requestStop();
            assertEquals("STOPPED", owner.readStatus());
            assertEquals(0, owner.waitForExit());
        }
    }

    @Test
    public void init_differentFolders_allowsIndependentApplications() throws Exception {
        Path otherFolder = Files.createDirectory(testFolder.resolve("other"));
        try (JavaChildProcess first = JavaChildProcess.start(testFolder, Probe.class, "hold");
                JavaChildProcess second = JavaChildProcess.start(otherFolder, Probe.class, "hold")) {
            assertEquals("READY", first.readStatus());
            assertEquals("READY", second.readStatus());
            assertTrue(Files.exists(testFolder.resolve("data/addressbook.json")));
            assertTrue(Files.exists(otherFolder.resolve("data/addressbook.json")));
            first.requestStop();
            second.requestStop();
            assertEquals("STOPPED", first.readStatus());
            assertEquals("STOPPED", second.readStatus());
            assertEquals(0, first.waitForExit());
            assertEquals(0, second.waitForExit());
        }
    }

    @Test
    public void stop_ownerCloses_allowsRestartWithRetainedMarker() throws Exception {
        try (JavaChildProcess owner = JavaChildProcess.start(testFolder, Probe.class, "stop-hold")) {
            assertEquals("READY", owner.readStatus());
            owner.requestStop();
            assertEquals("STOPPED", owner.readStatus());
            assertTrue(Files.exists(testFolder.resolve(".policyharbor.lock")));
            assertRestartAllowed();
            owner.requestStop();
            assertEquals(0, owner.waitForExit());
        }
    }

    @Test
    public void init_ownerKilled_allowsRestartWithRetainedMarker() throws Exception {
        try (JavaChildProcess owner = JavaChildProcess.start(testFolder, Probe.class, "hold")) {
            assertEquals("READY", owner.readStatus());
            owner.kill();
        }
        assertTrue(Files.exists(testFolder.resolve(".policyharbor.lock")));
        assertRestartAllowed();
    }

    @Test
    public void init_lockCannotBeOpened_preservesFilesAndStopsSafely() throws Exception {
        Files.createDirectory(testFolder.resolve(".policyharbor.lock"));
        Path preferences = testFolder.resolve("preferences.json");
        Files.writeString(preferences, "null\n");
        Path data = Files.createDirectories(testFolder.resolve("data")).resolve("addressbook.json");
        Files.writeString(data, "{invalid data kept}");
        try (JavaChildProcess blocked = JavaChildProcess.start(testFolder, Probe.class, "probe")) {
            assertEquals("BLOCKED", blocked.readStatus());
            assertEquals(0, blocked.waitForExit());
        }
        assertEquals("null\n", Files.readString(preferences));
        assertEquals("{invalid data kept}", Files.readString(data));
    }

    private void assertRestartAllowed() throws Exception {
        byte[] data = Files.readAllBytes(testFolder.resolve("data/addressbook.json"));
        try (JavaChildProcess restarted = JavaChildProcess.start(testFolder, Probe.class, "probe")) {
            assertEquals("READY", restarted.readStatus());
            assertEquals("STOPPED", restarted.readStatus());
            assertEquals(0, restarted.waitForExit());
        }
        assertArrayEquals(data, Files.readAllBytes(testFolder.resolve("data/addressbook.json")));
    }

    /** Child entry point deliberately invokes the real application initialization and shutdown. */
    public static class Probe {
        public static void main(String[] args) throws Exception {
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            MainApp app = new MainApp();
            app.init();
            if (app.model == null) {
                if (app.storage != null || app.logic != null || app.ui != null) {
                    throw new AssertionError("Blocked startup initialized application services");
                }
                app.stop();
                System.out.println("BLOCKED");
                return;
            }
            if (!args[0].equals("probe")) {
                app.logic.execute("add n/Rachel Lim p/001 e/rachel@example.com a/Blk 123");
                app.logic.execute("followup 1 d/2099-12-15 m/Call about renewal");
            }
            System.out.println("READY");
            if (!args[0].equals("probe")) {
                input.readLine();
            }
            app.stop();
            System.out.println("STOPPED");
            if (args[0].equals("stop-hold")) {
                input.readLine();
            }
        }
    }
}
