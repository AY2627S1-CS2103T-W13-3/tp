package seedu.address;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.util.ApplicationHome;

/** Exercises early logging from executable JARs in fresh JVMs, without JavaFX or shared logger state. */
public class JarHomeLoggingIntegrationTest {
    private static final String INFO_MESSAGE = "Packaged logging probe info";
    private static final String WARNING_MESSAGE = "Packaged logging probe warning";
    private static final String FINE_MESSAGE = "Packaged logging probe fine";
    private static final String FALLBACK_MESSAGE =
            "Unable to create diagnostic log file; continuing with console logging.";

    @TempDir
    public Path testFolder;

    @Test
    public void initialize_otherWorkingFolder_appendsOnlyBesideJar() throws Exception {
        Path jar = createLoggingJar("application");
        Path workingFolder = Files.createDirectory(testFolder.resolve("caller"));
        Path callerLog = Files.writeString(workingFolder.resolve("policyharbor.log.0"), "Caller log canary\n");
        Path log = jar.getParent().resolve("policyharbor.log.0");
        Files.writeString(log, "Earlier application diagnostic\n");

        runProbe(jar, jar.getParent(), "once");
        runProbe(jar, workingFolder, "once");

        String content = Files.readString(log);
        assertTrue(content.startsWith("Earlier application diagnostic\n"));
        assertEquals(2, content.lines().filter(line -> line.contains(INFO_MESSAGE)).count());
        assertTrue(content.contains(WARNING_MESSAGE));
        assertFalse(content.contains(FINE_MESSAGE));
        assertEquals("Caller log canary\n", Files.readString(callerLog));
        assertEquals(List.of(callerLog), filesIn(workingFolder));
    }

    @Test
    public void initialize_percentTokensAndUnicodeInJarHome_keepsLiteralPathAndLock() throws Exception {
        Path jar = createLoggingJar("app home %t %h %g %u %% %20 新加坡");
        // The launcher arguments stay ASCII even on Windows; the production code source still contains Unicode.
        Path workingFolder = Files.createDirectory(jar.getParent().resolve("caller"));

        try (LoggingProcess child = startProbe(jar, workingFolder, "hold")) {
            child.awaitReady();
            assertTrue(Files.readString(jar.getParent().resolve("policyharbor.log.0")).contains(INFO_MESSAGE));
            assertEquals(1, logLocks(jar.getParent()).size());
            assertTrue(filesIn(workingFolder).isEmpty());
            child.finish();
        }
        assertTrue(logLocks(jar.getParent()).isEmpty(), "Normal shutdown should release the log lock");
    }

    @Test
    public void initialize_jarHomeLogIsDirectory_continuesOnConsoleWithoutUsingCaller() throws Exception {
        Path jar = createLoggingJar("blocked log");
        Path obstruction = Files.createDirectory(jar.getParent().resolve("policyharbor.log.0"));
        Path workingFolder = Files.createDirectory(testFolder.resolve("fallback caller"));

        String console = runProbe(jar, workingFolder, "once");

        assertTrue(console.contains(FALLBACK_MESSAGE), console);
        assertTrue(console.contains(INFO_MESSAGE), console);
        assertTrue(console.contains(WARNING_MESSAGE), console);
        assertFalse(console.contains(FINE_MESSAGE), console);
        assertTrue(Files.isDirectory(obstruction));
        assertTrue(filesIn(obstruction).isEmpty());
        assertTrue(filesIn(workingFolder).isEmpty());
    }

    @Test
    public void initialize_concurrentProcesses_keepsLogLocksIndependentOfWorkingFolder() throws Exception {
        Path jar = createLoggingJar("shared application");
        Path otherJar = createLoggingJar("independent application");
        Path firstCaller = Files.createDirectory(testFolder.resolve("first caller"));
        Path secondCaller = Files.createDirectory(testFolder.resolve("second caller"));

        try (LoggingProcess first = startProbe(jar, firstCaller, "hold");
                LoggingProcess second = startProbe(jar, secondCaller, "hold");
                LoggingProcess independent = startProbe(otherJar, firstCaller, "hold")) {
            first.awaitReady();
            second.awaitReady();
            independent.awaitReady();
            assertEquals(2, logLocks(jar.getParent()).size());
            assertEquals(1, logLocks(otherJar.getParent()).size());
            assertTrue(filesIn(firstCaller).isEmpty());
            assertTrue(filesIn(secondCaller).isEmpty());
            first.finish();
            second.finish();
            independent.finish();
        }
        assertTrue(logLocks(jar.getParent()).isEmpty());
        assertTrue(logLocks(otherJar.getParent()).isEmpty());
    }

    @Test
    public void initialize_logExceedsSizeLimit_rotatesBesideJar() throws Exception {
        Path jar = createLoggingJar("rotating application");
        Path workingFolder = Files.createDirectory(testFolder.resolve("rotation caller"));

        runProbe(jar, workingFolder, "rotate");

        assertTrue(Files.size(jar.getParent().resolve("policyharbor.log.0")) > 0);
        assertTrue(Files.size(jar.getParent().resolve("policyharbor.log.1")) >= 5 * 1024 * 1024);
        assertTrue(filesIn(workingFolder).isEmpty());
        assertTrue(logLocks(jar.getParent()).isEmpty());
    }

    /** Only the real home resolver, logger and test entry point are needed for this executable JAR. */
    private Path createLoggingJar(String folderName) throws Exception {
        Path jar = Files.createDirectory(testFolder.resolve(folderName)).resolve("PolicyHarbor.jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, Probe.class.getName());
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar), manifest)) {
            for (Class<?> type : List.of(LogsCenter.class, ApplicationHome.class, Probe.class)) {
                String entry = type.getName().replace('.', '/') + ".class";
                output.putNextEntry(new JarEntry(entry));
                try (var input = type.getResourceAsStream("/" + entry)) {
                    input.transferTo(output);
                }
                output.closeEntry();
            }
        }
        return jar;
    }

    private List<Path> filesIn(Path folder) throws IOException {
        try (var files = Files.list(folder)) {
            return files.sorted().toList();
        }
    }

    private List<Path> logLocks(Path folder) throws IOException {
        return filesIn(folder).stream().filter(path -> path.getFileName().toString().endsWith(".lck")).toList();
    }

    private LoggingProcess startProbe(Path jar, Path workingFolder, String mode) throws IOException {
        String javaName = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", javaName);
        Path diagnostics = Files.createTempFile(testFolder, "logging-probe-", ".log");
        // Contain possible FileHandler token expansions within disposable folders, even in a failing regression.
        List<String> command = List.of(java.toString(), "-ea", "-Duser.home=" + testFolder,
                "-Djava.io.tmpdir=" + testFolder, "-jar", workingFolder.relativize(jar).toString(), mode);
        Process child = new ProcessBuilder(command).directory(workingFolder.toFile())
                .redirectError(diagnostics.toFile()).start();
        return new LoggingProcess(child, diagnostics);
    }

    private String runProbe(Path jar, Path workingFolder, String mode) throws Exception {
        try (LoggingProcess child = startProbe(jar, workingFolder, mode)) {
            child.awaitReady();
            child.finish();
            return Files.readString(child.diagnostics);
        }
    }

    /** Bounds subprocess waits and releases files even when a test fails. */
    private static class LoggingProcess implements AutoCloseable {
        private final Process process;
        private final Path diagnostics;
        private final BufferedReader output;

        LoggingProcess(Process process, Path diagnostics) {
            this.process = process;
            this.diagnostics = diagnostics;
            output = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        }

        void awaitReady() throws Exception {
            String status = CompletableFuture.supplyAsync(() -> {
                try {
                    return output.readLine();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).get(30, TimeUnit.SECONDS);
            assertEquals("READY", status, diagnostics());
        }

        void finish() throws Exception {
            if (process.isAlive()) {
                process.getOutputStream().write('\n');
                process.getOutputStream().flush();
            }
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Logging probe timed out");
            assertEquals(0, process.exitValue(), diagnostics());
        }

        private String diagnostics() throws IOException {
            String text = Files.readString(diagnostics);
            return text.substring(Math.max(0, text.length() - 8192));
        }

        @Override
        public void close() throws Exception {
            if (process.isAlive()) {
                process.destroyForcibly();
                assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Logging probe could not be terminated");
            }
            process.getOutputStream().close();
            output.close();
        }
    }

    /** Runs the production static logging initializer before any application initialization. */
    public static class Probe {
        public static void main(String[] args) throws Exception {
            String workingDirectory = System.getProperty("user.dir");
            Logger logger = LogsCenter.getLogger("PackagedLoggingProbe");
            logger.info(INFO_MESSAGE);
            logger.warning(WARNING_MESSAGE);
            logger.fine(FINE_MESSAGE);
            if (args[0].equals("rotate")) {
                String payload = "Rotation probe " + "x".repeat(100_000);
                for (int i = 0; i < 70; i++) {
                    logger.info(payload);
                }
            }
            if (!workingDirectory.equals(System.getProperty("user.dir"))) {
                throw new AssertionError("Logging changed user.dir");
            }
            System.out.println("READY");
            System.in.read();
        }
    }
}
