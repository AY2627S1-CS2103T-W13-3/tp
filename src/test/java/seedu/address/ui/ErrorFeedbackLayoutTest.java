package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
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
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Checks feedback text inside the composed dashboard, without showing windows or using the system clipboard. */
public class ErrorFeedbackLayoutTest {
    // Default window, minimum window, and 1280x720 at 150% in logical scene pixels.
    private static final double[][] LOGICAL_SIZES = {{1000, 720}, {640, 600}, {853, 480}};
    private static final double ROUNDING_TOLERANCE = 1;

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
    public void invalidIndex_errorLabelAndCompleteMessage_fitInsideFeedbackViewport() throws Exception {
        runOnFx(() -> {
            try (Dashboard dashboard = new Dashboard()) {
                for (double[] size : LOGICAL_SIZES) {
                    dashboard.resize(size);
                    dashboard.submit("view 0");
                    Label status = (Label) dashboard.node("#feedbackStatus");
                    assertTrue(status.isVisible());
                    assertEquals("Error", status.getText());
                    TextArea feedback = dashboard.feedback();
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
    public void multilineError_scrollsToCompleteLastLineAndReturnsToCompactSuccess() throws Exception {
        Dashboard dashboard = callOnFx(Dashboard::new);
        try {
            for (double[] size : LOGICAL_SIZES) {
                runOnFx(() -> {
                    dashboard.resize(size);
                    dashboard.submit("followup");
                });
                // TextAreaSkin queues scroll/layout work; allow it to run between the user actions.
                double errorHeight = callOnFx(() -> {
                    dashboard.layout();
                    TextArea feedback = dashboard.feedback();
                    String message = feedback.getText();
                    assertTrue(message.contains("\n"), "Incomplete followup command should display multiline usage");
                    assertFalse(feedback.isEditable());
                    assertTrue(feedback.isFocusTraversable());
                    assertCharacterInsideViewport(feedback, 0);
                    double height = dashboard.node("#resultDisplayPlaceholder").getLayoutBounds().getHeight();
                    assertTrue(height < 100, "Long messages must not expand the compact feedback strip");
                    feedback.selectAll();
                    assertEquals(message, feedback.getSelectedText(),
                            "The full error must remain selectable for copying");
                    ScrollBar vertical = feedback.lookupAll(".scroll-bar").stream()
                            .map(node -> (ScrollBar) node)
                            .filter(bar -> bar.getOrientation() == Orientation.VERTICAL)
                            .findFirst().orElseThrow();
                    assertTrue(vertical.isVisible());
                    vertical.setValue(vertical.getMax());
                    return height;
                });
                runOnFx(() -> {
                    dashboard.layout();
                    TextArea feedback = dashboard.feedback();
                    assertTrue(feedback.getScrollTop() > 0);
                    assertCharacterInsideViewport(feedback, feedback.getLength() - 1);
                    dashboard.submit("list");
                });
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

    /** Uses the rendered character's line box, including its ascent and descent, inside the actual skin viewport. */
    private static void assertCharacterInsideViewport(TextArea feedback, int character) {
        Text text = (Text) feedback.lookup(".text");
        javafx.scene.shape.Path characterShape = new javafx.scene.shape.Path(text.rangeShape(character, character + 1));
        Bounds rendered = text.localToScene(characterShape.getBoundsInLocal());
        Node viewport = feedback.lookup(".viewport");
        Bounds visible = viewport.localToScene(viewport.getBoundsInLocal());
        assertTrue(rendered.getMinY() >= visible.getMinY() - ROUNDING_TOLERANCE
                        && rendered.getMaxY() <= visible.getMaxY() + ROUNDING_TOLERANCE,
                "Complete character line " + rendered + " must fit inside " + visible);
    }

    private class Dashboard implements AutoCloseable {
        private final MainWindow window;
        private final Region root;

        Dashboard() {
            Path data = folder.resolve("clients.json");
            LogicManager logic = new LogicManager(new ModelManager(new AddressBook(), new UserPrefs()),
                    new StorageManager(new JsonAddressBookStorage(data),
                            new JsonUserPrefsStorage(folder.resolve("preferences.json"))));
            window = new MainWindow(new Stage(), logic, data);
            window.fillInnerParts();
            root = (Region) window.getRoot().getScene().getRoot();
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
            assertSame(input, window.getRoot().getScene().getFocusOwner());
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
