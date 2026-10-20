package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;

/**
 * A UI component that displays the result of a command execution.
 */
public class ResultDisplay extends UiPart<Region> {

    private static final String FXML = "ResultDisplay.fxml";
    private static final PseudoClass ERROR_PSEUDO_CLASS = PseudoClass.getPseudoClass("error");

    @FXML
    private TextArea resultDisplay;

    @FXML
    private Label feedbackStatus;

    public ResultDisplay() {
        super(FXML);
    }

    /**
     * Displays normal feedback, clearing any previous error state.
     *
     * @throws NullPointerException If {@code feedbackToUser} is null.
     */
    public void setFeedbackToUser(String feedbackToUser) {
        setFeedback(feedbackToUser, false);
    }

    /**
     * Displays feedback in the error state without inferring its state from the text.
     *
     * @throws NullPointerException If {@code feedbackToUser} is null.
     */
    public void setErrorFeedbackToUser(String feedbackToUser) {
        setFeedback(feedbackToUser, true);
    }

    private void setFeedback(String feedbackToUser, boolean isError) {
        requireNonNull(feedbackToUser);
        resultDisplay.setText(feedbackToUser);
        feedbackStatus.setVisible(isError);
        feedbackStatus.setManaged(isError);
        getRoot().pseudoClassStateChanged(ERROR_PSEUDO_CLASS, isError);
    }

}
