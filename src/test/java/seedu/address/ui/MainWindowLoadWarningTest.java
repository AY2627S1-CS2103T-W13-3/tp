package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.ListCommand;

public class MainWindowLoadWarningTest {
    @Test
    public void initialFeedback_invalidSavedData_showsRecoveryWarning() {
        assertEquals(Messages.MESSAGE_LOAD_WARNING, MainWindow.initialFeedback(true));
        assertTrue(MainWindow.initialFeedback(true).contains("restore a valid saved copy"));
        assertEquals(ListCommand.MESSAGE_SUCCESS, MainWindow.initialFeedback(false));
    }

    @Test
    public void feedbackAfterCommand_loadingBlocked_keepsWarningVisible() {
        String readOnlyFeedback = MainWindow.feedbackWithLoadWarning(true, ListCommand.MESSAGE_SUCCESS);
        assertTrue(readOnlyFeedback.startsWith(Messages.MESSAGE_LOAD_WARNING));
        assertTrue(readOnlyFeedback.contains(ListCommand.MESSAGE_SUCCESS));

        String errorFeedback = MainWindow.feedbackWithLoadWarning(true, Messages.MESSAGE_CHANGES_BLOCKED);
        assertTrue(errorFeedback.startsWith(Messages.MESSAGE_LOAD_WARNING));
        assertTrue(errorFeedback.contains(Messages.MESSAGE_CHANGES_BLOCKED));
        assertEquals(ListCommand.MESSAGE_SUCCESS,
                MainWindow.feedbackWithLoadWarning(false, ListCommand.MESSAGE_SUCCESS));
    }
}
