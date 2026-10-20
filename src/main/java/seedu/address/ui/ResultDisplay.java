package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import java.net.URL;

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
    private static final String STYLESHEET = "ResultDisplay.css";
    private static final PseudoClass ERROR_PSEUDO_CLASS = PseudoClass.getPseudoClass("error");

    @FXML
    private TextArea resultDisplay;

    @FXML
    private Label feedbackStatus;

    /**
     * Creates a feedback display with its required layout and stylesheet.
     *
     * @throws IllegalStateException If a required resource or injected control is missing.
     */
    public ResultDisplay() {
        this(ResultDisplay.class.getResource(FXML_FILE_FOLDER + FXML),
                ResultDisplay.class.getResource(FXML_FILE_FOLDER + STYLESHEET));
    }

    /**
     * Creates a feedback display from the supplied layout and stylesheet resources.
     *
     * @throws IllegalStateException If a required resource or injected control is missing.
     */
    ResultDisplay(URL fxmlResource, URL stylesheetResource) {
        super(requireResource(fxmlResource, FXML));
        URL stylesheet = requireResource(stylesheetResource, STYLESHEET);
        if (resultDisplay == null || feedbackStatus == null) {
            throw new IllegalStateException("ResultDisplay.fxml must inject resultDisplay and feedbackStatus");
        }
        getRoot().getStylesheets().add(stylesheet.toExternalForm());
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
        requireNonNull(feedbackToUser, "Feedback must not be null");
        resultDisplay.setText(feedbackToUser);
        feedbackStatus.setVisible(isError);
        feedbackStatus.setManaged(isError);
        getRoot().pseudoClassStateChanged(ERROR_PSEUDO_CLASS, isError);
    }

    private static URL requireResource(URL resource, String resourceName) {
        if (resource == null) {
            throw new IllegalStateException("Missing required feedback resource: " + resourceName);
        }
        return resource;
    }

}
