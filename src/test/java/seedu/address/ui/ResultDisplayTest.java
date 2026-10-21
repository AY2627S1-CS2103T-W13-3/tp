package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class ResultDisplayTest {

    @BeforeAll
    public static void initializeToolkit() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "JavaFX control tests require a graphical session");
        FutureTask<Void> startup = new FutureTask<>(() -> {
            Platform.setImplicitExit(false);
            return null;
        });
        try {
            Platform.startup(startup);
        } catch (IllegalStateException alreadyInitialized) {
            // Other component tests may already own the shared JavaFX runtime.
            Platform.runLater(startup);
        }
        startup.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void initializeToolkit_alreadyRunning_remainsUsable() throws Exception {
        initializeToolkit();
        initializeToolkit();
        runOnFxThread(() -> {
            assertTrue(Platform.isFxApplicationThread());
            assertFalse(Platform.isImplicitExit());
        });
    }

    @Test
    public void constructor_initialState_isNormal() throws Exception {
        runOnFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            assertFeedback(display, "", false);
        });
    }

    @Test
    public void constructor_missingResource_throwsClearException() throws Exception {
        runOnFxThread(() -> {
            IllegalStateException missingFxml = assertThrows(
                    IllegalStateException.class, () -> new ResultDisplay(null, getStylesheet()));
            assertTrue(missingFxml.getMessage().contains("ResultDisplay.fxml"));

            URL fxml = ResultDisplay.class.getResource("/view/ResultDisplay.fxml");
            IllegalStateException missingCss = assertThrows(
                    IllegalStateException.class, () -> new ResultDisplay(fxml, null));
            assertTrue(missingCss.getMessage().contains("ResultDisplay.css"));
        });
    }

    @Test
    public void constructor_missingInjectedControl_throwsClearException() throws Exception {
        runOnFxThread(() -> {
            String header = "<?import javafx.scene.layout.VBox?>\n"
                    + "<?import javafx.scene.control.TextArea?>\n"
                    + "<?import javafx.scene.control.Label?>\n"
                    + "<VBox xmlns:fx=\"http://javafx.com/fxml/1\">";
            for (String control : List.of("<TextArea fx:id=\"resultDisplay\"/>",
                    "<Label fx:id=\"feedbackStatus\"/>")) {
                URL incompleteFxml = createFxmlResource(header + control + "</VBox>");
                IllegalStateException exception = assertThrows(
                        IllegalStateException.class, () -> new ResultDisplay(incompleteFxml, getStylesheet()));
                assertTrue(exception.getMessage().contains("ResultDisplay.fxml"));
            }
        });
    }

    @Test
    public void setFeedbackToUser_errorText_keepsNormalState() throws Exception {
        runOnFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            String feedback = "Error: this is ordinary command output.\nNo error state was requested.";
            display.setFeedbackToUser(feedback);
            assertFeedback(display, feedback, false);
        });
    }

    @Test
    public void setErrorFeedbackToUser_ordinaryText_setsErrorState() throws Exception {
        runOnFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            String feedback = "Please retry.\nThe data could not be saved.";
            display.setErrorFeedbackToUser(feedback);
            assertFeedback(display, feedback, true);
        });
    }

    @Test
    public void setFeedbackToUser_afterError_resetsStateEvenWithSameText() throws Exception {
        runOnFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            String feedback = "Command feedback";
            display.setFeedbackToUser(feedback);
            assertFeedback(display, feedback, false);
            display.setErrorFeedbackToUser(feedback);
            assertFeedback(display, feedback, true);
            display.setFeedbackToUser(feedback);
            assertFeedback(display, feedback, false);
        });
    }

    @Test
    public void setFeedbackToUser_emptyTextAfterError_clearsFeedbackAndErrorState() throws Exception {
        runOnFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            display.setErrorFeedbackToUser("Please retry.");
            display.setFeedbackToUser("");
            assertFeedback(display, "", false);
        });
    }

    @Test
    public void setFeedback_null_rejectsWithoutChangingFeedbackOrState() throws Exception {
        runOnFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            display.setFeedbackToUser("Ready");
            assertThrows(NullPointerException.class, () -> display.setErrorFeedbackToUser(null));
            assertFeedback(display, "Ready", false);
            assertThrows(NullPointerException.class, () -> display.setFeedbackToUser(null));
            assertFeedback(display, "Ready", false);

            display.setErrorFeedbackToUser("Please retry.");
            assertThrows(NullPointerException.class, () -> display.setFeedbackToUser(null));
            assertFeedback(display, "Please retry.", true);
            assertThrows(NullPointerException.class, () -> display.setErrorFeedbackToUser(null));
            assertFeedback(display, "Please retry.", true);
        });
    }

    @Test
    public void setErrorFeedbackToUser_emptyMessage_keepsExplicitErrorIdentification() throws Exception {
        runOnFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            display.setErrorFeedbackToUser("");
            assertFeedback(display, "", true);
        });
    }

    @Test
    public void setFeedback_repeatedLayout_preservesSceneFocusAndInput() throws Exception {
        runOnFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            TextField command = new TextField("list");
            VBox root = new VBox(command, display.getRoot());
            Scene scene = new Scene(root, 320, 180);
            root.applyCss();
            root.layout();
            command.requestFocus();
            command.positionCaret(2);
            for (int i = 0; i < 3; i++) {
                display.setErrorFeedbackToUser("Please retry.");
                root.applyCss();
                root.layout();
                assertCommandInput(scene, command);
                display.setFeedbackToUser("Ready");
                root.applyCss();
                root.layout();
                assertCommandInput(scene, command);
            }
        });
    }

    @Test
    public void setFeedback_repeatedUpdates_preservesCommandFocusAndInput() throws Exception {
        boolean isMacActionsRunner = "true".equals(System.getenv("GITHUB_ACTIONS"))
                && "macOS".equals(System.getenv("RUNNER_OS"));
        boolean isNativeUiEnabled = Boolean.parseBoolean(System.getenv("POLICYHARBOR_NATIVE_UI_TESTS"));
        // This shown-window test aborts the hosted macOS worker; scene-level coverage runs everywhere.
        assumeFalse(isMacActionsRunner && !isNativeUiEnabled,
                "Native window focus is opt-in on macOS Actions after SIGABRT; "
                        + "set POLICYHARBOR_NATIVE_UI_TESTS=true on an interactive desktop runner");
        FocusFixture fixture = callOnFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            TextField command = new TextField("list");
            Scene scene = new Scene(new VBox(command, display.getRoot()), 320, 180);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.show();
            command.requestFocus();
            command.positionCaret(2);
            return new FocusFixture(stage, scene, command, display);
        });

        try {
            awaitLayoutPulse(fixture.scene());
            runOnFxThread(() -> assertCommandFocus(fixture));
            for (int i = 0; i < 3; i++) {
                runOnFxThread(() -> fixture.display().setErrorFeedbackToUser("Please retry."));
                awaitLayoutPulse(fixture.scene());
                runOnFxThread(() -> assertCommandFocus(fixture));
                runOnFxThread(() -> fixture.display().setFeedbackToUser("Ready"));
                awaitLayoutPulse(fixture.scene());
                runOnFxThread(() -> assertCommandFocus(fixture));
            }
        } finally {
            runOnFxThread(() -> fixture.stage().close());
        }
    }

    @Test
    public void focus_feedbackOutline_isVisibleInNormalAndErrorStates() throws Exception {
        runOnFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            layoutDisplay(display);
            TextArea feedback = (TextArea) display.getRoot().lookup("#resultDisplay");
            double normalHeight = display.getRoot().prefHeight(240);
            PseudoClass focused = PseudoClass.getPseudoClass("focused");
            Paint unfocusedBorder = feedback.getBorder() == null ? Color.TRANSPARENT
                    : feedback.getBorder().getStrokes().get(0).getTopStroke();
            feedback.pseudoClassStateChanged(focused, true);
            display.getRoot().applyCss();
            assertVisibleFocusOutline(feedback);
            assertEquals(normalHeight, display.getRoot().prefHeight(240));

            display.setErrorFeedbackToUser("Please retry.");
            display.getRoot().applyCss();
            assertVisibleFocusOutline(feedback);

            feedback.pseudoClassStateChanged(focused, false);
            display.getRoot().applyCss();
            assertEquals(unfocusedBorder, feedback.getBorder().getStrokes().get(0).getTopStroke());
        });
    }

    @Test
    public void setFeedbackToUser_afterScrolledError_clearsOldSelectionAndScrollPosition() throws Exception {
        runOnFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            String message = "Unable to save changes.\nPlease retry.\n".repeat(30);
            display.setErrorFeedbackToUser(message);
            assertCompleteMessageAccessible(display, message, true);
            TextArea feedback = (TextArea) display.getRoot().lookup("#resultDisplay");
            assertFalse(feedback.getSelectedText().isEmpty());

            display.setFeedbackToUser("Recovered");
            display.getRoot().getParent().layout();
            assertFeedback(display, "Recovered", false);
            assertEquals("", feedback.getSelectedText());
            assertEquals(0, feedback.getCaretPosition());
            assertEquals(0, feedback.getScrollTop());
        });
    }

    @Test
    public void setErrorFeedbackToUser_repeatedUpdates_showsOneErrorLabelWithoutChangingMessage() throws Exception {
        runOnFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            List<String> initialStyles = List.copyOf(display.getRoot().getStyleClass());
            for (int i = 0; i < 3; i++) {
                String message = "Save failed.\nPlease retry: " + i;
                display.setErrorFeedbackToUser(message);
                assertFeedback(display, message, true);
            }
            assertEquals(1, display.getRoot().lookupAll("#feedbackStatus").size());
            assertEquals(initialStyles, display.getRoot().getStyleClass());
        });
    }

    @Test
    public void setFeedbackToUser_afterError_restoresNormalStylesWithoutAffectingOtherControls() throws Exception {
        runOnFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            TextArea feedback = (TextArea) display.getRoot().lookup("#resultDisplay");
            TextArea otherFeedback = new TextArea("Unrelated feedback");
            otherFeedback.getStyleClass().add("result-display");
            VBox host = new VBox(display.getRoot(), otherFeedback);
            Scene scene = new Scene(host);
            scene.getStylesheets().add(getClass().getResource("/view/DarkTheme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/view/Extensions.css").toExternalForm());
            display.setFeedbackToUser("Ready");
            host.applyCss();
            Paint normalText = getTextFill(feedback);
            Paint normalBorder = display.getRoot().getBorder().getStrokes().get(0).getTopStroke();
            Paint otherText = getTextFill(otherFeedback);

            display.setErrorFeedbackToUser("Please retry.");
            host.applyCss();
            assertFeedback(display, "Please retry.", true);
            assertNotEquals(normalText, getTextFill(feedback));
            assertNotEquals(normalBorder, display.getRoot().getBorder().getStrokes().get(0).getTopStroke());
            assertEquals(otherText, getTextFill(otherFeedback));

            display.setFeedbackToUser("Recovered");
            host.applyCss();
            assertFeedback(display, "Recovered", false);
            assertEquals(normalText, getTextFill(feedback));
            assertEquals(normalBorder, display.getRoot().getBorder().getStrokes().get(0).getTopStroke());
            assertEquals(otherText, getTextFill(otherFeedback));
        });
    }

    @Test
    public void layout_normalAndErrorFeedback_keepsPreferredHeightCompact() throws Exception {
        runOnFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            display.setFeedbackToUser("Ready");
            layoutDisplay(display);
            assertTrue(display.getRoot().prefHeight(240) <= 80);
            assertTrue(display.getRoot().getHeight() < 100);

            display.setErrorFeedbackToUser("Please retry.");
            display.getRoot().getParent().layout();
            assertTrue(display.getRoot().prefHeight(240) <= 96);
            assertTrue(display.getRoot().getHeight() < 100);
        });
    }

    @Test
    public void setFeedbackToUser_longUnbrokenMessage_wrapsAndKeepsAllTextSelectable() throws Exception {
        runOnFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            String message = "LongFeedback".repeat(100);
            display.setFeedbackToUser(message);
            assertCompleteMessageAccessible(display, message, false);
        });
    }

    @Test
    public void setErrorFeedbackToUser_multilineMessage_scrollsAndKeepsAllTextSelectable() throws Exception {
        runOnFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            String message = "Invalid command.\nPlease check the supplied fields.\n".repeat(30)
                    + "Unable to save changes. Please retry.";
            display.setErrorFeedbackToUser(message);
            assertCompleteMessageAccessible(display, message, true);
        });
    }

    private static void assertCompleteMessageAccessible(ResultDisplay display, String message, boolean isError) {
        layoutDisplay(display);
        TextArea feedback = (TextArea) display.getRoot().lookup("#resultDisplay");
        assertFeedback(display, message, isError);
        assertTrue(feedback.isWrapText());
        assertFalse(feedback.isEditable());
        assertFalse(feedback.isDisabled());
        assertTrue(feedback.isFocusTraversable());
        feedback.selectAll();
        assertEquals(message, feedback.getSelectedText());
        assertCopiedText(feedback, message);

        ScrollBar vertical = getScrollBar(feedback, Orientation.VERTICAL);
        ScrollBar horizontal = getScrollBar(feedback, Orientation.HORIZONTAL);
        assertTrue(vertical.isVisible());
        assertTrue(vertical.getMax() > vertical.getMin());
        assertFalse(horizontal.isVisible());
        vertical.setValue(vertical.getMax());
        assertTrue(feedback.getScrollTop() > 0);
    }

    private static ScrollBar getScrollBar(TextArea feedback, Orientation orientation) {
        return feedback.lookupAll(".scroll-bar").stream()
                .map(node -> (ScrollBar) node)
                .filter(scrollBar -> scrollBar.getOrientation() == orientation)
                .findFirst().orElseThrow();
    }

    private static void layoutDisplay(ResultDisplay display) {
        StackPane host = new StackPane(display.getRoot());
        Scene scene = new Scene(host, 240, 100);
        scene.getStylesheets().add(ResultDisplayTest.class.getResource("/view/DarkTheme.css").toExternalForm());
        scene.getStylesheets().add(ResultDisplayTest.class.getResource("/view/Extensions.css").toExternalForm());
        host.applyCss();
        host.layout();
    }

    private static void assertFeedback(ResultDisplay display, String expectedText, boolean isError) {
        TextArea feedback = (TextArea) display.getRoot().lookup("#resultDisplay");
        assertEquals(expectedText, feedback.getText());
        assertEquals(isError, display.getRoot().getPseudoClassStates().contains(PseudoClass.getPseudoClass("error")));
        Label status = (Label) display.getRoot().lookup("#feedbackStatus");
        assertEquals(isError, status.isVisible());
        assertEquals(isError, status.isManaged());
        if (isError) {
            assertEquals("Error", status.getText());
        }
    }

    private static Paint getTextFill(TextArea feedback) {
        return ((Text) feedback.lookup(".text")).getFill();
    }

    private static void assertCopiedText(TextArea feedback, String message) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        Map<DataFormat, Object> originalContent = new HashMap<>();
        for (DataFormat format : clipboard.getContentTypes()) {
            Object content = clipboard.getContent(format);
            if (content != null) {
                originalContent.put(format, content);
            }
        }
        try {
            clipboard.clear();
            feedback.copy();
            assertEquals(message, clipboard.getString());
        } finally {
            if (originalContent.isEmpty()) {
                clipboard.clear();
            } else {
                assertTrue(clipboard.setContent(originalContent), "Could not restore clipboard contents");
            }
        }
    }

    private static void assertCommandFocus(FocusFixture fixture) {
        assertTrue(fixture.stage().isShowing());
        assertCommandInput(fixture.scene(), fixture.command());
    }

    private static void assertCommandInput(Scene scene, TextField command) {
        assertSame(command, scene.getFocusOwner());
        assertEquals("list", command.getText());
        assertEquals(2, command.getCaretPosition());
    }

    private static void assertVisibleFocusOutline(TextArea feedback) {
        assertNotNull(feedback.getBorder(), "Focused feedback must have a visible border");
        assertTrue(feedback.getBorder().getStrokes().get(0).getWidths().getTop() > 0);
        Color border = (Color) feedback.getBorder().getStrokes().get(0).getTopStroke();
        assertTrue(border.getOpacity() > 0, "Focus border must not be transparent");
    }

    private static void awaitLayoutPulse(Scene scene) throws Exception {
        CountDownLatch pulse = new CountDownLatch(1);
        Runnable listener = pulse::countDown;
        runOnFxThread(() -> {
            scene.addPostLayoutPulseListener(listener);
            scene.getRoot().requestLayout();
            Platform.requestNextPulse();
        });
        try {
            assertTrue(pulse.await(10, TimeUnit.SECONDS), "JavaFX layout pulse did not complete");
        } finally {
            runOnFxThread(() -> scene.removePostLayoutPulseListener(listener));
        }
    }

    private static URL getStylesheet() {
        return ResultDisplay.class.getResource("/view/ResultDisplay.css");
    }

    private static URL createFxmlResource(String fxml) {
        try {
            return URL.of(URI.create("memory:ResultDisplay.fxml"), new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL url) {
                    return new URLConnection(url) {
                        @Override
                        public void connect() {
                            // The fixture is already available in memory.
                        }

                        @Override
                        public InputStream getInputStream() {
                            return new ByteArrayInputStream(fxml.getBytes(StandardCharsets.UTF_8));
                        }
                    };
                }
            });
        } catch (MalformedURLException exception) {
            throw new AssertionError("Invalid in-memory FXML fixture URL", exception);
        }
    }

    private static void runOnFxThread(Runnable action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(action, null);
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    private static <T> T callOnFxThread(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    private record FocusFixture(Stage stage, Scene scene, TextField command, ResultDisplay display) {
    }
}
