package seedu.address.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.testutil.JavaChildProcess;

/** Verifies ownership, cleanup, stale markers and cross-process exclusion of the application lock. */
public class AppInstanceLockTest {
    @TempDir
    public Path testFolder;

    @Test
    public void acquire_sameJvmOwnerActive_rejectsSecondWithoutReleasingOwner() throws Exception {
        Path file = testFolder.resolve(".policyharbor.lock");
        try (AppInstanceLock owner = AppInstanceLock.acquire(file)) {
            assertThrows(AppInstanceLock.AlreadyRunningException.class, () -> AppInstanceLock.acquire(file));
            assertThrows(AppInstanceLock.AlreadyRunningException.class, () -> AppInstanceLock.acquire(file));
        }
        try (AppInstanceLock next = AppInstanceLock.acquire(file)) {
            assertThrows(AppInstanceLock.AlreadyRunningException.class, () -> AppInstanceLock.acquire(file));
        }
    }

    @Test
    public void close_calledRepeatedly_keepsReacquiredOwnerProtected() throws Exception {
        Path file = testFolder.resolve(".policyharbor.lock");
        AppInstanceLock first = AppInstanceLock.acquire(file);
        first.close();
        assertTrue(Files.exists(file));
        try (AppInstanceLock second = AppInstanceLock.acquire(file)) {
            first.close();
            assertThrows(AppInstanceLock.AlreadyRunningException.class, () -> AppInstanceLock.acquire(file));
        }
    }

    @Test
    public void acquire_separateFolders_allowsBothOwners() throws Exception {
        Path otherFolder = Files.createDirectory(testFolder.resolve("other"));
        try (AppInstanceLock first = AppInstanceLock.acquire(testFolder.resolve(".policyharbor.lock"));
                AppInstanceLock second = AppInstanceLock.acquire(otherFolder.resolve(".policyharbor.lock"))) {
            assertThrows(AppInstanceLock.AlreadyRunningException.class, () ->
                    AppInstanceLock.acquire(otherFolder.resolve(".policyharbor.lock")));
        }
    }

    @Test
    public void acquire_leftoverMarker_usesLiveOwnershipInsteadOfFileExistence() throws Exception {
        Path file = testFolder.resolve(".policyharbor.lock");
        Files.writeString(file, "previous process marker");
        try (AppInstanceLock owner = AppInstanceLock.acquire(file)) {
            assertThrows(AppInstanceLock.AlreadyRunningException.class, () -> AppInstanceLock.acquire(file));
        }
        assertTrue(Files.exists(file));
    }

    @Test
    public void acquire_markerPathIsDirectory_reportsIoFailure() throws Exception {
        Path directory = Files.createDirectory(testFolder.resolve(".policyharbor.lock"));
        assertThrows(IOException.class, () -> AppInstanceLock.acquire(directory));
    }

    @Test
    public void acquire_otherJvmOwnsLock_rejectsUntilAbruptExit() throws Exception {
        Path file = testFolder.resolve(".policyharbor.lock");
        try (JavaChildProcess owner = JavaChildProcess.start(testFolder, Probe.class, file.toString())) {
            assertEquals("LOCKED", owner.readStatus());
            assertThrows(AppInstanceLock.AlreadyRunningException.class, () -> AppInstanceLock.acquire(file));
            owner.kill();
        }
        try (AppInstanceLock next = AppInstanceLock.acquire(file)) {
            assertThrows(AppInstanceLock.AlreadyRunningException.class, () -> AppInstanceLock.acquire(file));
        }
    }

    @Test
    public void acquire_sameJvmAttemptRejected_preservesOsLockAgainstOtherJvm() throws Exception {
        Path file = testFolder.resolve(".policyharbor.lock");
        try (AppInstanceLock owner = AppInstanceLock.acquire(file)) {
            assertThrows(AppInstanceLock.AlreadyRunningException.class, () -> AppInstanceLock.acquire(file));
            try (JavaChildProcess contender = JavaChildProcess.start(testFolder, Probe.class, file.toString())) {
                assertEquals("BLOCKED", contender.readStatus());
                assertEquals(0, contender.waitForExit());
            }
        }
    }

    @Test
    public void acquire_parentPathAlias_keepsExistingOwnerProtected() throws Exception {
        Path file = testFolder.resolve(".policyharbor.lock");
        Path childFolder = Files.createDirectory(testFolder.resolve("child"));
        Path alias = childFolder.resolve("..").resolve(".policyharbor.lock");
        try (AppInstanceLock owner = AppInstanceLock.acquire(file)) {
            assertThrows(AppInstanceLock.AlreadyRunningException.class, () -> AppInstanceLock.acquire(alias));
            try (JavaChildProcess contender = JavaChildProcess.start(testFolder, Probe.class, file.toString())) {
                assertEquals("BLOCKED", contender.readStatus());
                assertEquals(0, contender.waitForExit());
            }
        }
    }

    /** Holds an actual OS file lock until the parent terminates this JVM. */
    public static class Probe {
        public static void main(String[] args) throws Exception {
            try (AppInstanceLock owner = AppInstanceLock.acquire(Path.of(args[0]))) {
                System.out.println("LOCKED");
                System.in.read();
            } catch (AppInstanceLock.AlreadyRunningException e) {
                System.out.println("BLOCKED");
            }
        }
    }
}
