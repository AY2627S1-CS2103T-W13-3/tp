package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Rectangle2D;
import seedu.address.commons.core.GuiSettings;
import seedu.address.logic.Logic;
import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.FollowUp;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

public class DashboardShellTest {
    private static final Rectangle2D DISPLAY = new Rectangle2D(0, 25, 1440, 875);

    @TempDir
    public Path testFolder;

    @Test
    public void bindClientListLabels_switchingViews_updatesTitleAndDisplayedCount() throws Exception {
        ClientListFixture labels = new ClientListFixture(testFolder);
        labels.assertLabels("Clients", "3 shown");

        labels.logic.execute("followups");
        labels.assertLabels("Pending clients", "2 shown");
        labels.logic.execute("list");
        labels.assertLabels("Clients", "3 shown");
    }

    @Test
    public void bindClientListLabels_findAndList_tracksFilteredCount() throws Exception {
        ClientListFixture labels = new ClientListFixture(testFolder);

        labels.logic.execute("find Amanda");
        labels.assertLabels("Clients", "1 shown");
        labels.logic.execute("find NoMatch");
        labels.assertLabels("Clients", "0 shown");
        labels.logic.execute("list");
        labels.assertLabels("Clients", "3 shown");
    }

    @Test
    public void bindClientListLabels_addAndDelete_tracksCommittedCount() throws Exception {
        ClientListFixture labels = new ClientListFixture(testFolder);

        labels.logic.execute("add n/Dana Tan p/444 e/dana@example.com a/Test Street");
        labels.assertLabels("Clients", "4 shown");
        labels.logic.execute("delete 4");
        labels.assertLabels("Clients", "3 shown");
        labels.logic.execute("find Amanda");
        labels.logic.execute("delete 1");
        labels.assertLabels("Clients", "0 shown");
        labels.logic.execute("list");
        labels.assertLabels("Clients", "2 shown");
    }

    @Test
    public void bindClientListLabels_recordReplaceAndClearFollowUp_tracksPendingCount() throws Exception {
        ClientListFixture labels = new ClientListFixture(testFolder);

        labels.logic.execute("followup 3 d/2026-10-21 m/Call Chloe");
        labels.assertLabels("Clients", "3 shown");
        labels.logic.execute("followups");
        labels.assertLabels("Pending clients", "3 shown");
        labels.logic.execute("followup 1 d/2026-10-23 m/Reschedule call");
        labels.assertLabels("Pending clients", "3 shown");
        labels.logic.execute("followup 1 clear");
        labels.assertLabels("Pending clients", "2 shown");
        labels.logic.execute("delete 1");
        labels.assertLabels("Pending clients", "1 shown");
        labels.logic.execute("followup 1 clear");
        labels.assertLabels("Pending clients", "0 shown");
        labels.logic.execute("list");
        labels.assertLabels("Clients", "2 shown");
    }

    @Test
    public void bindClientListLabels_rejectedCommands_preservesLabels() throws Exception {
        ClientListFixture labels = new ClientListFixture(testFolder);
        labels.logic.execute("followups");

        assertThrows(CommandException.class, () -> labels.logic.execute("delete 99"));
        labels.assertLabels("Pending clients", "2 shown");
        assertThrows(CommandException.class, () -> labels.logic.execute("followup 1 d/2026-10-19 m/Past"));
        labels.assertLabels("Pending clients", "2 shown");
    }

    @Test
    public void getWindowBounds_firstLaunch_centersDashboardWithinDisplay() {
        assertEquals(new Rectangle2D(220, 102.5, 1000, 720),
                MainWindow.getWindowBounds(new GuiSettings(), DISPLAY));
    }

    @Test
    public void getWindowBounds_validSavedPosition_preservesBounds() {
        GuiSettings saved = new GuiSettings(900, 640, 120, 80);
        assertEquals(new Rectangle2D(120, 80, 900, 640), MainWindow.getWindowBounds(saved, DISPLAY));
    }

    @Test
    public void getWindowBounds_tinySavedSize_keepsWorkspaceUsable() {
        GuiSettings saved = new GuiSettings(80, 90, 50, 30);
        assertEquals(new Rectangle2D(50, 30, 640, 600), MainWindow.getWindowBounds(saved, DISPLAY));
    }

    @Test
    public void getWindowBounds_oversizedOffscreenWindow_fitsDisplay() {
        GuiSettings saved = new GuiSettings(8000, 6000, 9999, 9999);
        assertEquals(DISPLAY, MainWindow.getWindowBounds(saved, DISPLAY));
    }

    @Test
    public void getWindowBounds_offscreenCoordinates_recoversVisiblePosition() {
        GuiSettings saved = new GuiSettings(900, 640, 5000, -1000);
        assertEquals(new Rectangle2D(540, 25, 900, 640), MainWindow.getWindowBounds(saved, DISPLAY));
    }

    @Test
    public void getWindowBounds_negativeMonitorCoordinates_preservesVisibleWindow() {
        Rectangle2D secondDisplay = new Rectangle2D(-1920, 0, 1920, 1080);
        GuiSettings saved = new GuiSettings(900, 640, -1800, 100);
        assertEquals(new Rectangle2D(-1800, 100, 900, 640),
                MainWindow.getWindowBounds(saved, secondDisplay));
    }

    @Test
    public void getWindowBounds_smallDisplay_limitsMinimumToAvailableSpace() {
        Rectangle2D smallDisplay = new Rectangle2D(0, 0, 600, 400);
        assertEquals(smallDisplay,
                MainWindow.getWindowBounds(new GuiSettings(640, 600, 0, 0), smallDisplay));
    }

    @Test
    public void getWindowBounds_nonFiniteSavedSize_usesFiniteDefaults() {
        GuiSettings saved = new GuiSettings(Double.NaN, Double.POSITIVE_INFINITY, 100, 100);
        assertEquals(new Rectangle2D(100, 100, 1000, 720), MainWindow.getWindowBounds(saved, DISPLAY));
    }

    @Test
    public void getWindowBounds_nonPositiveSavedSize_usesUsableDefaults() {
        GuiSettings saved = new GuiSettings(-1, 0, 100, 100);
        assertEquals(new Rectangle2D(100, 100, 1000, 720), MainWindow.getWindowBounds(saved, DISPLAY));
    }

    @Test
    public void getWindowBounds_fractionalDisplayBounds_staysInsideDisplay() {
        Rectangle2D scaledDisplay = new Rectangle2D(100.5, 10.25, 853.333, 430.5);
        assertEquals(scaledDisplay,
                MainWindow.getWindowBounds(new GuiSettings(1000, 720, 100, 10), scaledDisplay));
    }

    @Test
    public void getWindowBounds_nullArguments_rejectsProgrammerError() {
        assertThrows(NullPointerException.class, () -> MainWindow.getWindowBounds(null, DISPLAY));
        assertThrows(NullPointerException.class, () -> MainWindow.getWindowBounds(new GuiSettings(), null));
    }

    @Test
    public void getWindowBounds_emptyDisplay_rejectsProgrammerError() {
        assertThrows(IllegalArgumentException.class, () ->
                MainWindow.getWindowBounds(new GuiSettings(), new Rectangle2D(0, 0, 0, 900)));
    }

    /** Uses the real command, publication and persistence paths without starting a graphical toolkit. */
    private static class ClientListFixture {
        private final Logic logic;
        private final StringProperty title = new SimpleStringProperty();
        private final StringProperty count = new SimpleStringProperty();

        ClientListFixture(Path folder) {
            LocalDate today = LocalDate.of(2026, 10, 20);
            AddressBook book = new AddressBook();
            book.addPerson(new PersonBuilder().withName("Amanda Tan").withPhone("111")
                    .withEmail("amanda@example.com").withFollowUp(new FollowUp(today, "Call Amanda")).build());
            book.addPerson(new PersonBuilder().withName("Ben Lee").withPhone("222")
                    .withEmail("ben@example.com").withFollowUp(new FollowUp(today.plusDays(2), "Call Ben")).build());
            book.addPerson(new PersonBuilder().withName("Chloe Ng").withPhone("333")
                    .withEmail("chloe@example.com").build());
            ModelManager model = new ModelManager(book, new UserPrefs(), today, false);
            StorageManager storage = new StorageManager(new JsonAddressBookStorage(folder.resolve("clients.json")),
                    new JsonUserPrefsStorage(folder.resolve("prefs.json")));
            logic = new LogicManager(model, storage,
                    Clock.fixed(Instant.parse("2026-10-20T00:00:00Z"), ZoneOffset.UTC));
            MainWindow.bindClientListLabels(logic, title, count);
        }

        void assertLabels(String expectedTitle, String expectedCount) {
            assertEquals(expectedTitle, title.get());
            assertEquals(expectedCount, count.get());
        }
    }
}
