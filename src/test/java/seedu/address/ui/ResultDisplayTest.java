package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.scene.control.TextArea;

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
            assertFalse(display.getRoot().getPseudoClassStates().contains(PseudoClass.getPseudoClass("error")));
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

    private static void assertFeedback(ResultDisplay display, String expectedText, boolean isError) {
        TextArea feedback = (TextArea) display.getRoot().lookup("#resultDisplay");
        assertEquals(expectedText, feedback.getText());
        assertEquals(isError, display.getRoot().getPseudoClassStates().contains(PseudoClass.getPseudoClass("error")));
    }

    private static void runOnFxThread(Runnable action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(action, null);
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }
}
