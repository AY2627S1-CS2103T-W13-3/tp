package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Checks dashboard feedback geometry and dynamic scrolling without using the system clipboard. */
public class ErrorFeedbackLayoutTest {
    // Default window, minimum window, and 1280x720 at 150% in logical scene pixels.
    private static final double[][] LOGICAL_SIZES = {{1000, 720}, {640, 600}, {853, 480}};
    private static final double ROUNDING_TOLERANCE = 1;
    private static final String ERROR_COMMAND = "layout-test-error";

    @TempDir
    public Path folder;

    @BeforeAll
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
        initialize.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void invalidClientId_errorLabelAndCompleteMessage_fitInsideFeedbackViewport() throws Exception {
        runOnFx(() -> {
            try (Dashboard dashboard = new Dashboard()) {
                for (double[] size : LOGICAL_SIZES) {
                    dashboard.resize(size);
                    dashboard.submit("view 0");
                    Label status = (Label) dashboard.node("#feedbackStatus");
                    assertTrue(status.isVisible());
                    assertEquals("Error", status.getText());
                    TextArea feedback = dashboard.feedback();
                    assertEquals("Client ID must be a positive integer from 1 to 2147483647.", feedback.getText());
                    assertFalse(feedback.getText().contains("\n"), "This regression needs a single text line");
                    Node text = feedback.lookup(".text");
                    Node viewport = feedback.lookup(".viewport");
                    Bounds renderedText = text.localToScene(text.getBoundsInLocal());
                    Bounds visibleArea = viewport.localToScene(viewport.getBoundsInLocal());
                    assertTrue(renderedText.getMinY() >= visibleArea.getMinY() - ROUNDING_TOLERANCE
                                    && renderedText.getMaxY() <= visibleArea.getMaxY() + ROUNDING_TOLERANCE,
                            "Complete error line " + renderedText + " must fit inside " + visibleArea
                                    + " at " + size[0] + "x" + size[1]);
                    Bounds statusBounds = status.localToScene(status.getBoundsInLocal());
                    assertTrue(statusBounds.getMaxY() <= visibleArea.getMinY(), "Error label must stay above message");
                    assertTrue(dashboard.node("#resultDisplayPlaceholder").getLayoutBounds().getHeight() < 100,
                            "Feedback must stay compact");
                }
            }
        });
    }

    @Test
    public void multilineError_fitsWithoutScrolling_remainsCompletelyReadable() throws Exception {
        String message = "Please retry.\nUse a valid command.";
        runOnFx(() -> {
            try (Dashboard dashboard = new Dashboard(message)) {
                for (double[] size : LOGICAL_SIZES) {
                    dashboard.resize(size);
                    dashboard.submit(ERROR_COMMAND);
                    TextArea feedback = dashboard.feedback();
                    assertEquals(message, feedback.getText());
                    assertTextRangeInsideViewport(feedback, 0, message.length());
                    assertFalse(getVerticalScrollBar(feedback).isVisible(),
                            "Two short lines should fit the feedback area's preferred two rows");
                    assertEquals(0, feedback.getScrollTop());
                }
            }
        });
    }

    @Test
    public void overflowingError_scrollsToCompleteLastLineAndReturnsToCompactSuccess() throws Exception {
        // Explicit lines guarantee overflow regardless of platform fonts or usage-message wrapping.
        String message = "Additional error details.\n".repeat(40) + "Last line: retry the command.";
        Dashboard dashboard = callOnFx(() -> {
            Dashboard shown = new Dashboard(message);
            shown.showForLayoutPulses();
            return shown;
        });
        try {
            for (double[] size : LOGICAL_SIZES) {
                runOnFx(() -> {
                    dashboard.resize(size);
                    dashboard.submit(ERROR_COMMAND);
                });
                // Dynamic skin layout requires a real scene pulse, not only a queued FX action.
                dashboard.awaitLayoutPulse();
                double errorHeight = callOnFx(() -> {
                    dashboard.layout();
                    TextArea feedback = dashboard.feedback();
                    assertEquals(message, feedback.getText());
                    assertFalse(feedback.isEditable());
                    assertTrue(feedback.isFocusTraversable());
                    assertCharacterInsideViewport(feedback, 0);
                    Text text = (Text) feedback.lookup(".text");
                    Node viewport = feedback.lookup(".viewport");
                    assertTrue(text.getLayoutBounds().getHeight() > viewport.getLayoutBounds().getHeight(),
                            "The fixture must actually overflow before requiring a scrollbar");
                    double height = dashboard.node("#resultDisplayPlaceholder").getLayoutBounds().getHeight();
                    assertTrue(height < 100, "Long messages must not expand the compact feedback strip");
                    feedback.selectAll();
                    assertEquals(message, feedback.getSelectedText(),
                            "The full error must remain selectable for copying");
                    assertTrue(getVerticalScrollBar(feedback).isVisible());
                    feedback.setScrollTop(text.getLayoutBounds().getHeight());
                    return height;
                });
                dashboard.awaitLayoutPulse();
                runOnFx(() -> {
                    dashboard.layout();
                    TextArea feedback = dashboard.feedback();
                    assertTrue(feedback.getScrollTop() > 0);
                    assertTextRangeInsideViewport(feedback, message.lastIndexOf('\n') + 1, message.length());
                    assertEquals(message, feedback.getSelectedText(), "Scrolling must preserve the full selection");
                    dashboard.assertCommandFocus();
                    dashboard.submit("list");
                });
                dashboard.awaitLayoutPulse();
                runOnFx(() -> {
                    dashboard.layout();
                    TextArea feedback = dashboard.feedback();
                    assertFalse(dashboard.node("#feedbackStatus").isManaged());
                    assertTrue(dashboard.node("#resultDisplayPlaceholder").getLayoutBounds().getHeight() < errorHeight,
                            "Success should recover the space used by the Error label");
                    assertCharacterInsideViewport(feedback, 0);
                    assertEquals("", feedback.getSelectedText());
                    assertEquals(0, feedback.getScrollTop());
                    assertTrue(dashboard.node("#workspace").getLayoutBounds().getHeight() >= 100,
                            "Feedback must leave a usable workspace at the smallest logical size");
                });
            }
        } finally {
            runOnFx(dashboard::close);
        }
    }

    private static void assertCharacterInsideViewport(TextArea feedback, int character) {
        assertTextRangeInsideViewport(feedback, character, character + 1);
    }

    /** Checks complete line boxes, including ascent and descent, inside the actual skin viewport. */
    private static void assertTextRangeInsideViewport(TextArea feedback, int start, int end) {
        Text text = (Text) feedback.lookup(".text");
        javafx.scene.shape.Path rangeShape = new javafx.scene.shape.Path(text.rangeShape(start, end));
        Bounds rendered = text.localToScene(rangeShape.getBoundsInLocal());
        Node viewport = feedback.lookup(".viewport");
        Bounds visible = viewport.localToScene(viewport.getBoundsInLocal());
        assertTrue(rendered.getMinX() >= visible.getMinX() - ROUNDING_TOLERANCE
                        && rendered.getMaxX() <= visible.getMaxX() + ROUNDING_TOLERANCE
                        && rendered.getMinY() >= visible.getMinY() - ROUNDING_TOLERANCE
                        && rendered.getMaxY() <= visible.getMaxY() + ROUNDING_TOLERANCE,
                "Complete text range " + rendered + " must fit inside " + visible);
    }

    private static ScrollBar getVerticalScrollBar(TextArea feedback) {
        return feedback.lookupAll(".scroll-bar").stream()
                .map(node -> (ScrollBar) node)
                .filter(bar -> bar.getOrientation() == Orientation.VERTICAL)
                .findFirst().orElseThrow();
    }

    private class Dashboard implements AutoCloseable {
        private final MainWindow window;
        private final Region root;

        Dashboard() {
            this(null);
        }

        Dashboard(String errorMessage) {
            Path data = folder.resolve("clients.json");
            LogicManager logic = new LogicManager(new ModelManager(new AddressBook(), new UserPrefs()),
                    new StorageManager(new JsonAddressBookStorage(data),
                            new JsonUserPrefsStorage(folder.resolve("preferences.json")))) {
                @Override
                public CommandResult execute(String commandText) throws CommandException, ParseException {
                    if (ERROR_COMMAND.equals(commandText) && errorMessage != null) {
                        throw new ParseException(errorMessage);
                    }
                    return super.execute(commandText);
                }
            };
            window = new MainWindow(new Stage(), logic, data);
            window.fillInnerParts();
            root = (Region) window.getRoot().getScene().getRoot();
        }

        void showForLayoutPulses() {
            // Keep logical fixture sizes independent of the OS window's minimum dimensions.
            Group container = new Group();
            container.setAutoSizeChildren(false);
            root.getScene().setRoot(container);
            container.getChildren().add(root);
            window.getRoot().show();
        }

        void awaitLayoutPulse() throws Exception {
            CountDownLatch pulse = new CountDownLatch(1);
            Runnable listener = pulse::countDown;
            runOnFx(() -> {
                root.getScene().addPostLayoutPulseListener(listener);
                root.requestLayout();
                Platform.requestNextPulse();
            });
            try {
                assertTrue(pulse.await(10, TimeUnit.SECONDS), "JavaFX layout pulse must complete");
            } finally {
                runOnFx(() -> root.getScene().removePostLayoutPulseListener(listener));
            }
        }

        void resize(double[] size) {
            root.resize(size[0], size[1]);
            layout();
        }

        void submit(String command) {
            TextField input = (TextField) node("#commandTextField");
            input.requestFocus();
            input.setText(command);
            Button run = (Button) node("#runButton");
            run.fire();
            layout();
            assertCommandFocus();
        }

        void assertCommandFocus() {
            assertSame(node("#commandTextField"), window.getRoot().getScene().getFocusOwner());
        }

        void layout() {
            root.applyCss();
            root.layout();
            root.applyCss();
            root.layout();
        }

        Node node(String selector) {
            return root.lookup(selector);
        }

        TextArea feedback() {
            return (TextArea) node("#resultDisplay");
        }

        @Override
        public void close() {
            window.getRoot().fireEvent(new WindowEvent(window.getRoot(), WindowEvent.WINDOW_CLOSE_REQUEST));
        }
    }

    private static void runOnFx(ThrowingRunnable action) throws Exception {
        callOnFx(() -> {
            action.run();
            return null;
        });
    }

    private static <T> T callOnFx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(30, TimeUnit.SECONDS);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
