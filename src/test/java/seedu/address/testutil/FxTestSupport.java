package seedu.address.testutil;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;

/**
 * Shares bounded JavaFX toolkit startup and execution for component tests.
 * Call these methods outside the JavaFX Application Thread.
 */
public final class FxTestSupport {
    private static final long STARTUP_TIMEOUT_SECONDS = 10;

    private FxTestSupport() {
    }

    /**
     * Starts the toolkit, or reuses the toolkit initialized by another test class.
     */
    public static void initializeToolkit() throws Exception {
        FutureTask<Void> initialize = new FutureTask<>(() -> {
            Platform.setImplicitExit(false);
            return null;
        });
        try {
            Platform.startup(initialize);
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(initialize);
        }
        initialize.get(STARTUP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Executes an action on the JavaFX thread using the caller's timeout.
     */
    public static void runOnFx(ThrowingRunnable action, long timeoutSeconds) throws Exception {
        callOnFx(() -> {
            action.run();
            return null;
        }, timeoutSeconds);
    }

    /**
     * Returns an action's result, preserving execution failures and the caller's timeout.
     */
    public static <T> T callOnFx(Callable<T> action, long timeoutSeconds) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(timeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * An action that may fail while inspecting JavaFX controls.
     */
    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
