package seedu.address.commons.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests logging initialization in fresh JVMs, independently of the test runner's shared logger. */
public class LogsCenterTest {

    private static final String INFO_MESSAGE = "Logging probe info message";
    private static final String WARNING_MESSAGE = "Logging probe warning message";
    private static final String FINE_MESSAGE = "Logging probe fine message";
    private static final String FALLBACK_MESSAGE =
            "Unable to create diagnostic log file; continuing with console logging.";

    @TempDir
    public Path temporaryFolder;

    @Test
    public void initialize_logFileIsDirectory_continuesLoggingToConsole() throws Exception {
        Path logFile = Files.createDirectory(temporaryFolder.resolve("policyharbor.log.0"));

        String consoleOutput = runLoggingProbe();

        assertTrue(consoleOutput.contains(FALLBACK_MESSAGE), consoleOutput);
        assertLogMessages(consoleOutput);
        assertTrue(Files.isDirectory(logFile), "The obstructing directory must remain unchanged");
        try (var entries = Files.list(logFile)) {
            assertEquals(0, entries.count());
        }
    }

    @Test
    public void initialize_writableFolder_logsToConsoleAndFile() throws Exception {
        String consoleOutput = runLoggingProbe();

        assertFalse(consoleOutput.contains(FALLBACK_MESSAGE), consoleOutput);
        assertLogMessages(consoleOutput);
        assertLogMessages(Files.readString(temporaryFolder.resolve("policyharbor.log.0")));
    }

    @Test
    public void initialize_existingLog_appendsWithoutLosingEarlierMessages() throws Exception {
        Path logFile = temporaryFolder.resolve("policyharbor.log.0");
        String previousMessage = "Earlier diagnostic message\n";
        Files.writeString(logFile, previousMessage);

        runLoggingProbe();

        String fileOutput = Files.readString(logFile);
        assertTrue(fileOutput.startsWith(previousMessage), fileOutput);
        assertLogMessages(fileOutput);
    }

    /** Launches a new JVM so every scenario exercises the production static initializer exactly once. */
    private String runLoggingProbe() throws Exception {
        String javaName = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path javaExecutable = Path.of(System.getProperty("java.home"), "bin", javaName);
        // Gradle's worker classpath does not necessarily contain the application or test classes.
        String classPath = Path.of(LogsCenter.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                + File.pathSeparator
                + Path.of(LogsCenterTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path consoleOutput = temporaryFolder.resolve("stderr.txt");
        Process process = new ProcessBuilder(javaExecutable.toString(), "-cp", classPath, LoggingProbe.class.getName())
                .directory(temporaryFolder.toFile())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(consoleOutput.toFile())
                .start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Logging probe timed out");
            String output = Files.readString(consoleOutput);
            assertEquals(0, process.exitValue(), output);
            return output;
        } finally {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
        }
    }

    private void assertLogMessages(String output) {
        assertTrue(output.contains(INFO_MESSAGE), output);
        assertTrue(output.contains(WARNING_MESSAGE), output);
        assertFalse(output.contains(FINE_MESSAGE), output);
    }

    /** Minimal entry point that exercises both public logger lookup methods without launching JavaFX. */
    public static class LoggingProbe {

        public static void main(String[] args) {
            LogsCenter.getLogger("LoggingProbe").info(INFO_MESSAGE);
            LogsCenter.getLogger(LoggingProbe.class).warning(WARNING_MESSAGE);
            LogsCenter.getLogger(LoggingProbe.class).fine(FINE_MESSAGE);
        }
    }
}
