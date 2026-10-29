package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.ListCommand;
import seedu.address.model.Model;
import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.FxTestSupport;
import seedu.address.ui.UiManager;

/** Exercises the real failed-data startup path through the composed dashboard. */
public class DashboardStartupIntegrationTest {
    @TempDir
    public Path folder;

    @BeforeAll
    public static void initializeToolkit() throws Exception {
        FxTestSupport.initializeToolkit();
    }

    @Test
    public void invalidSavedData_queryAndRejectedMutation_preserveWarningSummaryAndFile() throws Exception {
        byte[] original = "{not valid JSON}".getBytes(StandardCharsets.UTF_8);
        Path data = folder.resolve("clients.json");
        Files.write(data, original);
        Storage storage = new StorageManager(new JsonAddressBookStorage(data),
                new JsonUserPrefsStorage(folder.resolve("preferences.json")));
        Model model = new MainApp().initModelManager(storage, new UserPrefs());
        assertTrue(model.isDataLoadingBlocked());
        LogicManager logic = new LogicManager(model, storage,
                Clock.fixed(Instant.parse("2026-10-21T00:00:00Z"), ZoneOffset.UTC));

        FxTestSupport.runOnFx(() -> {
            Stage stage = new Stage();
            try {
                new UiManager(logic, data).start(stage);
                Parent root = stage.getScene().getRoot();
                root.applyCss();
                root.layout();
                assertProtectedDashboard(root);

                submit(root, "list");
                assertEquals(ListCommand.MESSAGE_SUCCESS, feedback(root).getText());
                assertFalse(root.lookup("#feedbackStatus").isVisible());
                assertProtectedDashboard(root);

                submit(root, "add n/Ada p/123 e/ada@example.com a/Street");
                assertEquals(Messages.MESSAGE_CHANGES_BLOCKED, feedback(root).getText());
                assertTrue(root.lookup("#feedbackStatus").isVisible());
                TextField input = (TextField) root.lookup("#commandTextField");
                assertEquals("add n/Ada p/123 e/ada@example.com a/Street", input.getText());
                assertProtectedDashboard(root);

                submit(root, "list");
                assertFalse(root.lookup("#feedbackStatus").isVisible());
                assertProtectedDashboard(root);
            } finally {
                stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
            }
        }, 20);
        assertTrue(model.isDataLoadingBlocked());
        assertTrue(logic.getAllPersonList().isEmpty());
        assertArrayEquals(original, Files.readAllBytes(data));
    }

    private static void assertProtectedDashboard(Parent root) {
        Label warning = (Label) root.lookup("#loadWarning");
        assertTrue(warning.isVisible());
        assertTrue(warning.isManaged());
        assertEquals(Messages.MESSAGE_LOAD_WARNING, warning.getText());
        TableView<?> table = (TableView<?>) root.lookup("#personTable");
        assertTrue(table.getItems().isEmpty());
        Parent pending = (Parent) root.lookup("#pendingFollowUpPlaceholder");
        assertTrue(pending.isVisible());
        assertTrue(pending.isManaged());
        assertTrue(pending.lookupAll(".pending-card").isEmpty());
        Label empty = (Label) pending.lookup("#emptyMessage");
        assertNotNull(empty);
        assertTrue(empty.isVisible());
        assertTrue(empty.isManaged());
    }

    private static TextArea feedback(Parent root) {
        return (TextArea) root.lookup("#resultDisplay");
    }

    private static void submit(Parent root, String command) {
        TextField input = (TextField) root.lookup("#commandTextField");
        input.setText(command);
        Button run = (Button) root.lookup("#runButton");
        run.fire();
    }
}
