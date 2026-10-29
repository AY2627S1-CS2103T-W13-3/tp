package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static seedu.address.testutil.JavaFxTestUtil.findById;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import seedu.address.logic.LogicManager;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.FxTestSupport;
import seedu.address.testutil.JavaFxTestUtil;
import seedu.address.testutil.PersonBuilder;

/** Verifies that a closed window releases its profile's observers. */
public class ClientProfileLifecycleTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 29);

    @TempDir
    public Path folder;

    @BeforeAll
    public static void initializeToolkit() throws Exception {
        JavaFxTestUtil.initializeToolkit();
    }

    @Test
    public void windowHidden_laterModelChanges_doNotRefreshProfile() throws Exception {
        FxTestSupport.runOnFx(() -> {
            Person person = new PersonBuilder().withName("Rachel Lim")
                    .withFollowUp(new FollowUp(TODAY, "Call client")).build();
            AddressBook book = new AddressBook();
            book.addPerson(person);
            ModelManager model = new ModelManager(book, new UserPrefs(), TODAY, false);
            Path file = folder.resolve("clients.json");
            StorageManager storage = new StorageManager(new JsonAddressBookStorage(file),
                    new JsonUserPrefsStorage(folder.resolve("prefs.json")));
            LogicManager logic = new LogicManager(model, storage,
                    Clock.fixed(Instant.parse("2026-10-29T00:00:00Z"), ZoneOffset.UTC));
            Stage stage = new Stage();
            try {
                MainWindow window = new MainWindow(stage, logic, file);
                window.fillInnerParts();
                logic.execute("view 1");
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();
                Label name = (Label) findById(stage.getScene().getRoot(), "name");
                Label status = (Label) findById(stage.getScene().getRoot(), "followUpStatus");
                assertEquals("Rachel Lim", name.getText());
                assertEquals("DUE TODAY", status.getText());

                stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_HIDDEN));
                logic.execute("edit 1 n/Amanda Lee");
                model.updateToday(TODAY.plusDays(1));

                assertEquals("Rachel Lim", name.getText());
                assertEquals("DUE TODAY", status.getText());
            } finally {
                stage.close();
            }
        }, 10);
    }

    @Test
    public void dispose_duringNotification_ignoresCapturedAndFutureCallbacks() throws Exception {
        FxTestSupport.runOnFx(() -> {
            for (DisposalSource source : DisposalSource.values()) {
                Person person = new PersonBuilder().withName("Rachel Lim")
                        .withFollowUp(new FollowUp(TODAY, "Call client")).build();
                SimpleObjectProperty<Person> selected = new SimpleObjectProperty<>(person);
                SimpleObjectProperty<LocalDate> today = new SimpleObjectProperty<>(TODAY);
                AtomicReference<ClientProfilePanel> panelReference = new AtomicReference<>();
                Runnable trigger = switch (source) {
                    case DATE -> {
                        today.addListener((ChangeListener<LocalDate>) (observable, oldDate, newDate) ->
                                panelReference.get().dispose());
                        yield () -> today.set(TODAY.plusDays(1));
                    }
                    case SELECTION -> {
                        selected.addListener((InvalidationListener) observable -> panelReference.get().dispose());
                        yield () -> selected.set(null);
                    }
                };
                ClientProfilePanel panel = new ClientProfilePanel(selected, today, new AddressBook()::getClientId);
                panelReference.set(panel);
                Label name = (Label) findById(panel.getRoot(), "name");
                Label status = (Label) findById(panel.getRoot(), "followUpStatus");

                trigger.run();
                panel.dispose();
                selected.set(new PersonBuilder().withName("Amanda Lee").build());
                today.set(TODAY.plusDays(2));

                assertEquals("Rachel Lim", name.getText(), source.toString());
                assertEquals("DUE TODAY", status.getText(), source.toString());
            }
        }, 10);
    }

    @Test
    public void constructor_invalidInitialDate_doesNotRegisterSelectionListener() throws Exception {
        FxTestSupport.runOnFx(() -> {
            AtomicInteger subscriptions = new AtomicInteger();
            SimpleObjectProperty<Person> selected = new SimpleObjectProperty<>() {
                @Override
                public void addListener(InvalidationListener listener) {
                    subscriptions.incrementAndGet();
                    super.addListener(listener);
                }
            };
            assertThrows(NullPointerException.class, () -> new ClientProfilePanel(selected,
                    new SimpleObjectProperty<>(), new AddressBook()::getClientId));
            assertEquals(0, subscriptions.get());
        }, 10);
    }

    private enum DisposalSource {
        DATE, SELECTION
    }
}
