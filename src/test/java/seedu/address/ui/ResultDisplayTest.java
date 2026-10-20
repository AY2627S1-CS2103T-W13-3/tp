package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

public class ResultDisplayTest {

    @BeforeAll
    public static void initializeToolkit() throws Exception {
        FutureTask<Void> startup = new FutureTask<>(() -> {
            Platform.setImplicitExit(false);
            return null;
        });
        Platform.startup(startup);
        startup.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void constructor_initialState_isNormal() throws Exception {
        runOnFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            assertFeedback(display, "", false);
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

            display.setErrorFeedbackToUser("Please retry.");
            assertThrows(NullPointerException.class, () -> display.setFeedbackToUser(null));
            assertFeedback(display, "Please retry.", true);
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

    private static void runOnFxThread(Runnable action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(action, null);
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }
}
