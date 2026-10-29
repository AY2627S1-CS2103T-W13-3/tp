package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import java.util.function.Consumer;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.Region;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * The UI component that is responsible for receiving user command inputs.
 */
public class CommandBox extends UiPart<Region> {

    public static final String ERROR_STYLE_CLASS = "error";
    private static final String FXML = "CommandBox.fxml";

    private final Logger logger = LogsCenter.getLogger(CommandBox.class);
    private final CommandExecutor commandExecutor;

    @FXML
    private TextField commandTextField;

    @FXML
    private Button runButton;

    /**
     * Creates a {@code CommandBox} with the given {@code CommandExecutor}.
     *
     * @throws NullPointerException If the executor or an FXML-injected control is null.
     */
    public CommandBox(CommandExecutor commandExecutor) {
        this(commandExecutor, message -> { });
    }

    /**
     * Creates a command box that reports rejected input without submitting a command.
     *
     * @param commandExecutor Executes submitted commands.
     * @param inputErrorHandler Displays validation feedback for rejected input.
     * @throws NullPointerException If either callback or an FXML-injected control is null.
     */
    public CommandBox(CommandExecutor commandExecutor, Consumer<String> inputErrorHandler) {
        super(FXML);
        this.commandExecutor = requireNonNull(commandExecutor, "commandExecutor");
        requireNonNull(inputErrorHandler, "inputErrorHandler");
        requireNonNull(commandTextField, "commandTextField must be injected by CommandBox.fxml");
        requireNonNull(runButton, "runButton must be injected by CommandBox.fxml");
        // calls #setStyleToDefault() whenever there is a change to the text of the command box.
        commandTextField.textProperty().addListener((unused1, unused2, unused3) -> setStyleToDefault());
        // TextFormatter sees raw replacements from paste(), including shortcut and context-menu paste,
        // before TextField removes newlines, tabs and other control characters.
        commandTextField.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getText().codePoints().anyMatch(character -> Character.isISOControl(character)
                    || character == 0x2028 || character == 0x2029)) {
                setStyleToIndicateCommandFailure();
                inputErrorHandler.accept(Messages.MESSAGE_INVALID_CHARACTERS);
                return null;
            }
            return change;
        }));
    }

    /**
     * Submits the current command text when Enter or Run is activated.
     */
    @FXML
    private void handleCommandSubmitted() {
        String commandText = commandTextField.getText();
        try {
            executeCommand(commandExecutor, commandText);
            logger.fine("Command submission succeeded.");
            commandTextField.clear();
        } catch (CommandException | ParseException e) {
            // Exception messages and causes can contain client data, so log only the failure category.
            logger.fine("Command submission failed: " + e.getClass().getSimpleName());
            setStyleToIndicateCommandFailure();
        } finally {
            commandTextField.requestFocus();
        }
    }

    /**
     * Executes the supplied text, including empty text, through the shared command executor.
     *
     * @throws NullPointerException If the executor or command text is null.
     */
    static CommandResult executeCommand(CommandExecutor commandExecutor, String commandText)
            throws CommandException, ParseException {
        requireNonNull(commandExecutor, "commandExecutor");
        requireNonNull(commandText, "commandText");
        return commandExecutor.execute(commandText);
    }

    /**
     * Sets the command box style to use the default style.
     */
    private void setStyleToDefault() {
        commandTextField.getStyleClass().remove(ERROR_STYLE_CLASS);
    }

    /**
     * Sets the command box style to indicate a failed command.
     */
    private void setStyleToIndicateCommandFailure() {
        ObservableList<String> styleClass = commandTextField.getStyleClass();

        if (styleClass.contains(ERROR_STYLE_CLASS)) {
            return;
        }

        styleClass.add(ERROR_STYLE_CLASS);
    }

    /**
     * Represents a function that can execute commands.
     */
    @FunctionalInterface
    public interface CommandExecutor {
        /**
         * Executes the command and returns the result.
         *
         * @see seedu.address.logic.Logic#execute(String)
         */
        CommandResult execute(String commandText) throws CommandException, ParseException;
    }

}
