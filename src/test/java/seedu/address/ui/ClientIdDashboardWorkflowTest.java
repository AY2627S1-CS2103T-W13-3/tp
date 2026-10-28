package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import seedu.address.logic.LogicManager;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Exercises ID presentation across the real transaction, filtering and restart boundaries. */
public class ClientIdDashboardWorkflowTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 29);

    @TempDir
    public Path folder;

    @BeforeAll
    public static void initializeToolkit() throws Exception {
        ClientIdPresentationTest.initializeToolkit();
    }

    @Test
    public void edit_failedSaveRetryAndRestart_keepIdsAndVisibleContext() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            Person alice = client("Alice Tan", "81112222");
            Person rachel = client("Rachel Lim", "82223333");
            Person bob = client("Bob Lim", "83334444");
            AddressBook book = new AddressBook();
            book.restorePersons(List.of(alice, rachel, bob),
                    List.of(new ClientId(2), new ClientId(17), new ClientId(41)), 42);
            Path data = folder.resolve("clients.json");
            AtomicBoolean failSave = new AtomicBoolean();
            JsonAddressBookStorage storage = new JsonAddressBookStorage(data) {
                @Override
                public void saveAddressBook(ReadOnlyAddressBook clients) throws IOException {
                    if (failSave.get()) {
                        throw new IOException("Synthetic save failure");
                    }
                    super.saveAddressBook(clients);
                }
            };
            storage.saveAddressBook(book);
            ModelManager model = new ModelManager(book, new UserPrefs(), TODAY, false);
            LogicManager logic = new LogicManager(model, new StorageManager(storage,
                    new JsonUserPrefsStorage(folder.resolve("preferences.json"))),
                    Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
            MainWindow window = new MainWindow(new Stage(), logic, data);
            try {
                window.fillInnerParts();
                Parent root = window.getRoot().getScene().getRoot();
                root.applyCss();
                root.layout();
                TextField input = (TextField) root.lookup("#commandTextField");
                Button run = (Button) root.lookup("#runButton");
                submit(input, run, "find Rachel");
                submit(input, run, "view 17");
                Person selected = model.selectedPersonProperty().get();
                assertEquals(new ClientId(17), logic.getClientId(selected).orElseThrow());
                assertTrue(logic.getClientId(new PersonBuilder(selected).build()).isEmpty());
                assertEquals(3, root.lookupAll(".pending-card-id").size());
                byte[] before = Files.readAllBytes(data);

                submit(input, run, "view 1");
                assertEquals("view 1", input.getText());
                assertSame(selected, model.selectedPersonProperty().get());
                assertArrayEquals(before, Files.readAllBytes(data));

                failSave.set(true);
                String edit = "edit 17 p/+65 9234 5678";
                submit(input, run, edit);
                assertEquals(edit, input.getText());
                assertSame(selected, model.selectedPersonProperty().get());
                assertEquals("Client ID 17", ((Label) root.lookup("#clientId")).getText());
                assertArrayEquals(before, Files.readAllBytes(data));
                assertEquals(42, model.getAddressBook().getNextClientId());

                failSave.set(false);
                run.fire();
                assertEquals("", input.getText());
                assertEquals(1, logic.getFilteredPersonList().size());
                assertEquals(new ClientId(17), logic.getClientId(model.selectedPersonProperty().get()).orElseThrow());
                assertTrue(logic.getClientId(selected).isEmpty());
                assertEquals("+65 9234 5678", ((Label) root.lookup("#phone")).getText());
                assertEquals(3, root.lookupAll(".pending-card-id").size());
                assertFalse(((Label) root.lookup("#feedbackStatus")).isVisible());

                ReadOnlyAddressBook reloaded = storage.readAddressBook().orElseThrow();
                assertEquals(42, reloaded.getNextClientId());
                assertEquals("+65 9234 5678", reloaded.getPersonById(new ClientId(17))
                        .orElseThrow().getPhone().value);
                assertEquals(new ClientId(41), reloaded.getClientId(reloaded.getPersonList().getLast()).orElseThrow());
                assertEquals(TODAY, reloaded.getPersonById(new ClientId(17)).orElseThrow()
                        .getFollowUp().orElseThrow().getDueDate());
                ModelManager restarted = new ModelManager(reloaded, new UserPrefs(), TODAY, false);
                Person restored = restarted.getAddressBook().getPersonById(new ClientId(17)).orElseThrow();
                ClientProfilePanel profile = new ClientProfilePanel(restarted.selectedPersonProperty(),
                        restarted.todayProperty(), restarted.getAddressBook()::getClientId);
                new Scene(profile.getRoot(), 320, 400);
                profile.getRoot().applyCss();
                restarted.selectPerson(restored);
                assertEquals("Client ID 17", ((Label) profile.getRoot().lookup("#clientId")).getText());
            } finally {
                window.getRoot().fireEvent(new WindowEvent(window.getRoot(), WindowEvent.WINDOW_CLOSE_REQUEST));
            }
            return null;
        });
        Platform.runLater(task);
        task.get(20, TimeUnit.SECONDS);
    }

    private static void submit(TextField input, Button run, String command) {
        input.setText(command);
        run.fire();
    }

    private static Person client(String name, String phone) {
        return new PersonBuilder().withName(name).withPhone(phone).withEmail(phone + "@example.com")
                .withFollowUp(new FollowUp(TODAY, "Call client")).build();
    }
}
