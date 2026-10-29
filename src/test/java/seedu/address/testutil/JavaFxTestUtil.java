package seedu.address.testutil;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;

/**
 * Shares JavaFX startup, thread dispatch, and native desktop test requirements.
 */
public final class JavaFxTestUtil {
    private static final int FX_TIMEOUT_SECONDS = 10;

    private JavaFxTestUtil() {}

    /**
     * Starts JavaFX or reuses its existing runtime, keeping it alive between component tests.
     */
    public static void initializeToolkit() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "JavaFX control tests require a graphical session");
        FutureTask<Void> startup = new FutureTask<>(() -> {
            Platform.setImplicitExit(false);
            return null;
        });
        try {
            Platform.startup(startup);
        } catch (IllegalStateException alreadyInitialized) {
            // Component tests share one JavaFX runtime within the test worker.
            Platform.runLater(startup);
        }
        startup.get(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Skips native desktop checks on hosted macOS unless native UI testing is explicitly enabled.
     */
    public static void assumeNativeUiAvailable() {
        boolean isMacActionsRunner = "true".equals(System.getenv("GITHUB_ACTIONS"))
                && "macOS".equals(System.getenv("RUNNER_OS"));
        boolean isNativeUiEnabled = Boolean.parseBoolean(System.getenv("POLICYHARBOR_NATIVE_UI_TESTS"));
        assumeFalse(isMacActionsRunner && !isNativeUiEnabled,
                "Native desktop tests are opt-in on macOS Actions after SIGABRT; "
                        + "set POLICYHARBOR_NATIVE_UI_TESTS=true on a runner configured for native GUI input");
    }

    /**
     * Runs an action on the JavaFX application thread and propagates failures to the test.
     */
    public static void runOnFxThread(Runnable action) throws Exception {
        callOnFxThread(() -> {
            action.run();
            return null;
        });
    }

    /**
     * Returns an action's result from the JavaFX application thread, propagating failures to the test.
     */
    public static <T> T callOnFxThread(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(FX_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /** Finds an FXML node, including content not yet attached to a ScrollPane skin. */
    public static Node findById(Node node, String id) {
        if (id.equals(node.getId())) {
            return node;
        }
        if (node instanceof ScrollPane scrollPane) {
            return findById(scrollPane.getContent(), id);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Node match = findById(child, id);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }
}
