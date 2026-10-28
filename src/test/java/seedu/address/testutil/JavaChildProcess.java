package seedu.address.testutil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Runs a real JVM in an isolated working folder, with bounded protocol reads and cleanup. */
public final class JavaChildProcess implements AutoCloseable {
    private final Process process;
    private final BufferedReader output;
    private final Writer input;

    private JavaChildProcess(Process process) {
        this.process = process;
        output = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        input = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8);
    }

    /** Starts a helper main class with the complete test runtime classpath. */
    public static JavaChildProcess start(Path folder, Class<?> mainClass, String... arguments) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-cp");
        command.add(runtimeClasspath());
        command.add(mainClass.getName());
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).directory(folder.toFile())
                .redirectError(folder.resolve("child-" + System.nanoTime() + ".log").toFile()).start();
        return new JavaChildProcess(process);
    }

    private static String runtimeClasspath() throws Exception {
        Set<String> entries = new LinkedHashSet<>();
        for (String entry : System.getProperty("java.class.path").split(java.io.File.pathSeparator)) {
            entries.add(Path.of(entry).toAbsolutePath().toString());
        }
        for (ClassLoader loader = Thread.currentThread().getContextClassLoader(); loader != null;
                loader = loader.getParent()) {
            if (loader instanceof URLClassLoader urlLoader) {
                for (URL url : urlLoader.getURLs()) {
                    if (url.getProtocol().equals("file")) {
                        entries.add(Path.of(url.toURI()).toAbsolutePath().toString());
                    }
                }
            }
        }
        return String.join(java.io.File.pathSeparator, entries);
    }

    /** Reads one helper status, failing promptly if startup hangs. */
    public String readStatus() throws Exception {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return output.readLine();
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        }).get(15, TimeUnit.SECONDS);
    }

    /** Requests the helper's normal shutdown. */
    public void requestStop() throws IOException {
        input.write("stop\n");
        input.flush();
    }

    /** Terminates the process without running application shutdown hooks. */
    public void kill() throws InterruptedException {
        process.destroyForcibly();
        if (!process.waitFor(15, TimeUnit.SECONDS)) {
            throw new AssertionError("Child JVM did not terminate");
        }
    }

    /** Waits for a normal exit and returns its status. */
    public int waitForExit() throws InterruptedException {
        if (!process.waitFor(15, TimeUnit.SECONDS)) {
            throw new AssertionError("Child JVM did not exit");
        }
        return process.exitValue();
    }

    @Override
    public void close() throws Exception {
        if (process.isAlive()) {
            kill();
        }
        input.close();
        output.close();
    }
}
